package no.nav.foreldrepenger.dokumentbestiller;

import static no.nav.foreldrepenger.dokumentbestiller.vedtak.VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.formidling.Brev;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingDto;

@ApplicationScoped
public class DokumentForhåndsvisningTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(DokumentForhåndsvisningTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    private KlageRepository klageRepository;
    private Brev brev;

    public DokumentForhåndsvisningTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentForhåndsvisningTjeneste(BehandlingRepository behandlingRepository,
                                           BehandlingsresultatRepository behandlingsresultatRepository,
                                           DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                           KlageRepository klageRepository,
                                           Brev brev) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.klageRepository = klageRepository;
        this.brev = brev;
    }

    public byte[] forhåndsvisBrev(DokumentbestillingDto bestilling) {
        LOG.info("Forhåndsviser {} brev for {}", bestilling.getDokumentMal(), bestilling.getBehandlingUuid());

        if (bestilling.getDokumentMal() == null) {
            LOG.info("Utleder dokumentMal for {}", bestilling.getBehandlingUuid());

            var behandling = behandlingRepository.hentBehandling(bestilling.getBehandlingUuid());
            var resultat = behandlingsresultatRepository.hent(behandling.getId());

            var erRevurderingMedUendretUtfall = erRevurderingMedUendretUtfall(behandling) || erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering(resultat);

            var dokumentMal = velgDokumentMalForForhåndsvisningAvVedtak(behandling,
                resultat.getBehandlingResultatType(),
                resultat.getKonsekvenserForYtelsen(),
                erRevurderingMedUendretUtfall,
                klageRepository);

            LOG.info("Utleder {} dokumentMal for {}", dokumentMal, bestilling.getBehandlingUuid());
            bestilling.setDokumentMal(dokumentMal.getKode());
        }

        return brev.forhåndsvis(bestilling);
    }

    private boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsakYtelseType()).orElseThrow().erRevurderingMedUendretUtfall(behandling);
    }

    private boolean erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering(Behandlingsresultat behandlingResultat) {
        return behandlingResultat != null && foreldrepengerErEndret(behandlingResultat) && erKunEndringIFordelingAvYtelsen(behandlingResultat.getKonsekvenserForYtelsen())
            && harSendtVarselOmRevurdering(behandlingResultat.getBehandlingId());
    }

    private boolean harSendtVarselOmRevurdering(Long behandlingId) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingId, DokumentMalType.VARSEL_OM_REVURDERING);
    }

    private boolean foreldrepengerErEndret(Behandlingsresultat behandlingsresultat) {
        return BehandlingResultatType.FORELDREPENGER_ENDRET.equals(behandlingsresultat.getBehandlingResultatType());
    }

    private static boolean erKunEndringIFordelingAvYtelsen(List<KonsekvensForYtelsen> konsekvensForYtelsen) {
        return konsekvensForYtelsen.contains(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN) && konsekvensForYtelsen.size() == 1;
    }

}
