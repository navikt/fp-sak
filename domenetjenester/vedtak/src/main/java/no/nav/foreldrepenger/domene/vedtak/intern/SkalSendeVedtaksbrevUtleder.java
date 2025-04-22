package no.nav.foreldrepenger.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

@ApplicationScoped
public class SkalSendeVedtaksbrevUtleder {

    private static final Logger LOG = LoggerFactory.getLogger(SkalSendeVedtaksbrevUtleder.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private KlageRepository klageRepository;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private Instance<RevurderingTjeneste> revurderingTjenesteInstanser;

    SkalSendeVedtaksbrevUtleder() {
        // for CDI proxy
    }

    @Inject
    public SkalSendeVedtaksbrevUtleder(BehandlingRepository behandlingRepository,
                                       BehandlingsresultatRepository behandlingsresultatRepository,
                                       DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                       KlageRepository klageRepository,
                                       @Any Instance<RevurderingTjeneste> revurderingTjenesteInstanser) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.klageRepository = klageRepository;
        this.revurderingTjenesteInstanser = revurderingTjenesteInstanser;
    }

    public boolean skalSendVedtaksbrev(Long behandlingId) {
        var behandlingresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        if (behandlingresultatOpt.isEmpty()) {
            return false;
        }

        var behandlingsresultat = behandlingresultatOpt.get();
        if (Vedtaksbrev.INGEN.equals(behandlingsresultat.getVedtaksbrev())) {
            LOG.info("Sender ikke vedtaksbrev om det er eksplisit markert med INGEN: {}", behandlingId);
            return false;
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            LOG.info("Ankebrev sendes av kabal for behandling med id {} ", behandling.getId());
            return false;
        }

        if (BehandlingType.KLAGE.equals(behandling.getType()) && (!skalSendeVedtaksbrevIKlagebehandling(behandling) || harKlageBlittBehandletAvKabal(behandling))) {
            LOG.info("Sender ikke vedtaksbrev fra klagebehandlingen i behandlingen etter, eller når KlageVurderingResultat = null. For behandlingId {}", behandlingId);
            return false;
        }

        if (erBehandlingEtterKlage(behandling) && !skalSendeVedtaksbrevEtterKlage(behandling)) {
            LOG.info("Sender ikke vedtaksbrev for vedtak fra omgjøring fra klageinstansen på behandling {}, gjelder medhold fra klageinstans", behandlingId);
            return false;
        }

        if (SpesialBehandling.erJusterFeriepenger(behandling)) {
            LOG.info("Sender ikke vedtaksbrev for reberegning av feriepenger: {}", behandlingId);
            return false;
        }

        if (Boolean.TRUE.equals(erRevurderingMedUendretUtfall(behandling))) { // Beslutningsvedtak betyr at vedtaket er innvilget men har ingen konsekvens for ytelsen.
            if (Boolean.TRUE.equals(harSendtVarselOmRevurdering(behandlingId)) || harFritekstBrev(behandlingsresultat)) {
                LOG.info("Sender informasjonsbrev om uendret utfall i behandling: {}", behandlingId);
                // Dette her håndteres videre i dokumentMalUtleder
            } else {
                LOG.info(
                    "Uendret utfall av revurdering og har ikke sendt varsel om revurdering eller fritekst brev. Sender ikke brev for behandling: {}",
                    behandlingId);
                return false;
            }
        }
        return true;
    }

    private static boolean harFritekstBrev(Behandlingsresultat behandlingsresultat) {
        return Vedtaksbrev.FRITEKST.equals(behandlingsresultat.getVedtaksbrev());
    }

    private boolean skalSendeVedtaksbrevIKlagebehandling(Behandling behandling) {
        if (behandling.erRevurdering() && erBehandlingEtterKlage(behandling)) {
            return false;
        }
        return klageRepository.hentGjeldendeKlageVurderingResultat(behandling).isPresent();
    }

    private boolean skalSendeVedtaksbrevEtterKlage(Behandling behandling) {
        var klage = behandlingRepository.finnSisteIkkeHenlagteBehandlingavAvBehandlingTypeFor(behandling.getFagsakId(), BehandlingType.KLAGE).orElse(null);
        if (klage == null) {
            return true;
        }

        // henlagt eller ikke avsluttet
        if (klageRepository.hentGjeldendeKlageVurderingResultat(klage).orElse(null) == null) {
            return true;
        }
        return !behandling.erRevurdering(); // Send brev hvis førstegangssøkad, ellers ikke send
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

    private boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(revurderingTjenesteInstanser, behandling.getFagsakYtelseType())
            .orElseThrow()
            .erRevurderingMedUendretUtfall(behandling);
    }
}
