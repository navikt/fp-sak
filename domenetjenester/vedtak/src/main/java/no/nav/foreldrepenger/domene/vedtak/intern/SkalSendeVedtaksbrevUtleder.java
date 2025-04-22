package no.nav.foreldrepenger.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.*;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;

@ApplicationScoped
public class SkalSendeVedtaksbrevUtleder {

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

    public VedtaksbrevStatus statusVedtaksbrev(Long behandlingId) {
        var behandlingresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        if (behandlingresultatOpt.isEmpty()) {
            return VedtaksbrevStatus.INGEN_VEDTAKSBREV;
        }

        var behandlingsresultat = behandlingresultatOpt.get();
        if (Vedtaksbrev.INGEN.equals(behandlingsresultat.getVedtaksbrev())) {
            return VedtaksbrevStatus.INGEN_VEDTAKSBREV;
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (BehandlingType.ANKE.equals(behandling.getType())) {
            // Ankebrev sendes av kabal
            return VedtaksbrevStatus.INGEN_VEDTAKSBREV_ANKE;
        }

        if (BehandlingType.KLAGE.equals(behandling.getType()) && (!skalSendeVedtaksbrevIKlagebehandling(behandling) || harKlageBlittBehandletAvKabal(behandling))) {
            // Sender ikke vedtaksbrev fra klagebehandlingen i behandlingen etter, eller når KlageVurderingResultat = null
            return VedtaksbrevStatus.INGEN_VEDTAKSBREV_KLAGEBEHANDLING;
        }

        if (erBehandlingEtterKlage(behandling) && !skalSendeVedtaksbrevEtterKlage(behandling)) {
            // Sender ikke vedtaksbrev for revurderinger med omgjøring fra klageinstanse (gjelder medhold)
            return VedtaksbrevStatus.INGEN_VEDTAKSBREV_BEHANDLING_ETTER_KLAGE;
        }

        if (SpesialBehandling.erJusterFeriepenger(behandling)) {
            // Sender ikke vedtaksbrev for reberegning av feriepenger
            return VedtaksbrevStatus.INGEN_VEDTAKSBREV_JUSTERING_AV_FERIEPENGER;
        }

        if (Boolean.TRUE.equals(erRevurderingMedUendretUtfall(behandling))) { // Beslutningsvedtak betyr at vedtaket er innvilget men har ingen konsekvens for ytelsen.
            if (Boolean.TRUE.equals(harSendtVarselOmRevurdering(behandlingId)) || harFritekstBrev(behandlingsresultat)) {
                // Sender informasjonsbrev om uendret utfall. Dette her håndteres videre i dokumentMalUtleder
                return VedtaksbrevStatus.VEDTAKSBREV_PRODUSERES;
            } else {
                // Uendret utfall av revurdering og har ikke sendt varsel om revurdering eller fritekst brev
                return VedtaksbrevStatus.INGEN_VEDTAKSBREV_INGEN_KONSEKVENS_FOR_YTELSE;
            }
        }

        return VedtaksbrevStatus.VEDTAKSBREV_PRODUSERES;
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
