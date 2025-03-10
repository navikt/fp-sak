package no.nav.foreldrepenger.domene.vedtak.intern;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBestillerTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

@ApplicationScoped
public class SendVedtaksbrev {

    private static final Logger LOG = LoggerFactory.getLogger(SendVedtaksbrev.class);

    private BehandlingRepository behandlingRepository;
    private KlageRepository klageRepository;
    private DokumentBestillerTjeneste dokumentBestillerTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    private BehandlingVedtakRepository behandlingVedtakRepository;

    SendVedtaksbrev() {
        // for CDI proxy
    }

    @Inject
    public SendVedtaksbrev(BehandlingRepository behandlingRepository,
                           BehandlingVedtakRepository behandlingVedtakRepository,
                           DokumentBestillerTjeneste dokumentBestillerTjeneste,
                           DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                           KlageRepository klageRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.dokumentBestillerTjeneste = dokumentBestillerTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.klageRepository = klageRepository;
    }

    void sendVedtaksbrev(Long behandlingId) {
        var behandlingVedtakOpt = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId);
        if (behandlingVedtakOpt.isEmpty()) {
            LOG.info("Det foreligger ikke vedtak i behandling: {}, kan ikke sende vedtaksbrev", behandlingId);
            return;
        }
        var behandlingVedtak = behandlingVedtakOpt.get();

        if (Vedtaksbrev.INGEN.equals(behandlingVedtak.getBehandlingsresultat().getVedtaksbrev())) {
            LOG.info("Sender ikke vedtaksbrev om det er eksplisit markert med INGEN: {}", behandlingId);
            return;
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);

        //hardkoding for å unngå å sende vedtaksbrev til bruker i spesifikk behandling
        if (behandling.getUuid() != null && behandling.getUuid().equals(UUID.fromString("a2f77047-a441-4a95-ac61-52c4a0739658"))) {
            LOG.info("Sender ikke vedtaksbrev for behandlingUuid {} på sak {}", behandling.getUuid(), behandling.getFagsak().getSaksnummer());
            return;
        }

        if (BehandlingType.ANKE.equals(behandling.getType())) {
            LOG.info("Ankebrev sendes av kabal for behandling med id {} ", behandling.getId());
            return;
        }

        if (BehandlingType.KLAGE.equals(behandling.getType()) && (!skalSendeVedtaksbrevIKlagebehandling(behandling) || harKlageBlittBehandletAvKabal(behandling))) {
            LOG.info("Sender ikke vedtaksbrev fra klagebehandlingen i behandlingen etter, eller når KlageVurderingResultat = null. For behandlingId {}", behandlingId);
            return;
        }

        if (erBehandlingEtterKlage(behandling) && !skalSendeVedtaksbrevEtterKlage(behandling)) {
            LOG.info("Sender ikke vedtaksbrev for vedtak fra omgjøring fra klageinstansen på behandling {}, gjelder medhold fra klageinstans", behandlingId);
            return;
        }

        if (SpesialBehandling.erJusterFeriepenger(behandling)) {
            LOG.info("Sender ikke vedtaksbrev for reberegning av feriepenger: {}", behandlingId);
            return;
        }

        if (Boolean.TRUE.equals(behandlingVedtak.isBeslutningsvedtak())) { // Beslutningsvedtak betyr at vedtaket er innvilget men har ingen konsekvens for ytelsen.
            if (Boolean.TRUE.equals(harSendtVarselOmRevurdering(behandlingId)) || harFritekstBrev(behandlingVedtak)) {
                LOG.info("Sender informasjonsbrev om uendret utfall i behandling: {}", behandlingId);
                // Dette her håndteres videre i dokumentMalUtleder
            } else {
                LOG.info("Uendret utfall av revurdering og har ikke sendt varsel om revurdering eller fritekst brev. Sender ikke brev for behandling: {}", behandlingId);
                return;
            }
        } else if (gjelderEngangsstønad(behandling)) {
            LOG.info("Sender vedtaksbrev({}) for engangsstønad i behandling: {}", behandlingVedtak.getVedtakResultatType().getKode(), behandlingId);
        } else if (gjelderForeldrepenger(behandling)){
            LOG.info("Sender vedtaksbrev({}) for foreldrepenger i behandling: {}", behandlingVedtak.getVedtakResultatType().getKode(), behandlingId); //$NON-NLS-1
        } else {
            LOG.info("Sender vedtaksbrev({}) for svangerskapspenger i behandling: {}", behandlingVedtak.getVedtakResultatType().getKode(), behandlingId); //$NON-NLS-1
        }
        dokumentBestillerTjeneste.produserVedtaksbrev(behandlingVedtak);
    }

    private static boolean harFritekstBrev(BehandlingVedtak behandlingVedtak) {
        return Vedtaksbrev.FRITEKST.equals(behandlingVedtak.getBehandlingsresultat().getVedtaksbrev());
    }

    private boolean gjelderEngangsstønad(Behandling behandling) {
        return FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType());
    }
    private boolean gjelderForeldrepenger(Behandling behandling) {
        return FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType());
    }

    private boolean skalSendeVedtaksbrevIKlagebehandling(Behandling behandling) {
        if (behandling.erRevurdering() && erBehandlingEtterKlage(behandling)) {
            return false;
        }
        var vurderingOpt = klageRepository.hentGjeldendeKlageVurderingResultat(behandling);
        return vurderingOpt.isPresent();
    }

    private boolean skalSendeVedtaksbrevEtterKlage(Behandling behandling) {

        var klage = behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(behandling.getFagsakId(), BehandlingType.KLAGE).orElse(null);

        if (klage == null) {
            return true;
        }
        var vurdering = klageRepository.hentGjeldendeKlageVurderingResultat(klage).orElse(null);
        // henlagt eller ikke avsluttet
        return vurdering == null;
    }

    private boolean harKlageBlittBehandletAvKabal(Behandling behandling) {
        return klageRepository.hentKlageResultatHvisEksisterer(behandling.getId())
            .map(KlageResultatEntitet::erBehandletAvKabal).orElse(false);
    }

    private boolean erBehandlingEtterKlage(Behandling behandling) {
        return BehandlingÅrsakType.årsakerEtterKlageBehandling().stream().anyMatch(behandling::harBehandlingÅrsak);
    }

    private Boolean harSendtVarselOmRevurdering(Long behandlingId) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingId, DokumentMalType.VARSEL_OM_REVURDERING);
    }
}
