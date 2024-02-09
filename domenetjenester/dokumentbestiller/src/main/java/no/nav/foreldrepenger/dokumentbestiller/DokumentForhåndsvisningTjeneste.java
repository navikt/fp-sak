package no.nav.foreldrepenger.dokumentbestiller;

import static no.nav.foreldrepenger.dokumentbestiller.vedtak.VedtaksbrevUtleder.velgDokumentMalForForhåndsvisningAvVedtak;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
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
        var behandlingUuid = bestilling.getBehandlingUuid();
        var bestillingDokumentMal = bestilling.getDokumentMal();
        LOG.info("Forhåndsviser {} brev for {}", bestillingDokumentMal, behandlingUuid);

        if (bestillingDokumentMal == null) { // Gjelder kun vedtaksbrev
            LOG.info("Utleder dokumentMal for {}", behandlingUuid);

            var behandling = behandlingRepository.hentBehandling(behandlingUuid);
            var resultat = behandlingsresultatRepository.hent(behandling.getId());

            var revurderingMedUendretUtfall = erRevurderingMedUendretUtfall(behandling);
            var erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering = erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering(
                resultat.getBehandlingResultatType(), resultat.getKonsekvenserForYtelsen(), resultat.getBehandlingId());

            var erRevurderingMedUendretUtfall = revurderingMedUendretUtfall || erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering;

            LOG.info("revurderingMedUendretUtfall: {}, erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering: {}", revurderingMedUendretUtfall,
                erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering);

            var klageVurdering = finnKlageVurdering(behandling);

            var dokumentMal = velgDokumentMalForForhåndsvisningAvVedtak(behandling, resultat.getBehandlingResultatType(),
                resultat.getKonsekvenserForYtelsen(), erRevurderingMedUendretUtfall, klageVurdering);

            LOG.info("Utledet {} dokumentMal for {}", dokumentMal, behandlingUuid);
            bestilling.setDokumentMal(dokumentMal.getKode());
        }

        return brev.forhåndsvis(bestilling);
    }

    private boolean erRevurderingMedUendretUtfall(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsakYtelseType())
            .orElseThrow()
            .erRevurderingMedUendretUtfall(behandling);
    }

    private boolean erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering(BehandlingResultatType resultatType,
                                                                                 List<KonsekvensForYtelsen> konsekvensForYtelsenList,
                                                                                 long behandlingId) {
        return foreldrepengerErEndret(resultatType) && erKunEndringIFordelingAvYtelsen(konsekvensForYtelsenList) && harSendtVarselOmRevurdering(
            behandlingId);
    }

    private boolean foreldrepengerErEndret(BehandlingResultatType behandlingResultatType) {
        return BehandlingResultatType.FORELDREPENGER_ENDRET.equals(behandlingResultatType);
    }

    private static boolean erKunEndringIFordelingAvYtelsen(List<KonsekvensForYtelsen> konsekvensForYtelsenList) {
        return konsekvensForYtelsenList.contains(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN) && konsekvensForYtelsenList.size() == 1;
    }

    private boolean harSendtVarselOmRevurdering(Long behandlingId) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingId, DokumentMalType.VARSEL_OM_REVURDERING);
    }

    private KlageVurdering finnKlageVurdering(Behandling behandling) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return klageRepository.hentGjeldendeKlageVurderingResultat(behandling).map(KlageVurderingResultat::getKlageVurdering).orElse(null);
        }
        return null;
    }

}
