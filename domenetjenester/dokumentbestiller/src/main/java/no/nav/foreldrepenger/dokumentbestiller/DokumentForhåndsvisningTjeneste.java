package no.nav.foreldrepenger.dokumentbestiller;

import static no.nav.foreldrepenger.dokumentbestiller.formidling.BestillDokumentDtoMapper.mapDokumentMal;
import static no.nav.foreldrepenger.dokumentbestiller.formidling.BestillDokumentDtoMapper.mapRevurderignÅrsak;
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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.dokumentbestiller.formidling.Dokument;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentForhåndsvisDto;

@ApplicationScoped
public class DokumentForhåndsvisningTjeneste extends AbstractDokumentBestillerTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(DokumentForhåndsvisningTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private Dokument brev;

    DokumentForhåndsvisningTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentForhåndsvisningTjeneste(BehandlingRepository behandlingRepository,
                                           BehandlingsresultatRepository behandlingsresultatRepository,
                                           DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                           KlageRepository klageRepository,
                                           Dokument brev) {
        super(klageRepository);
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.brev = brev;
    }

    public byte[] forhåndsvisDokument(DokumentForhandsvisning bestilling) {
        var behandlingUuid = bestilling.behandlingUuid();
        var bestillingDokumentMal = bestilling.dokumentMal();
        LOG.info("Forhåndsviser brev med mal {} for behandling {}", bestillingDokumentMal, behandlingUuid);

        // Av og til er FRITEK satt allerede av GUI.
        if (bestillingDokumentMal == null) { // Gjelder kun vedtaksbrev
            LOG.info("Utleder dokumentMal for {}", behandlingUuid);

            var behandling = behandlingRepository.hentBehandling(behandlingUuid);
            var resultat = behandlingsresultatRepository.hent(behandling.getId());

            var brevType = bestilling.dokumentType();
            var resultatBrev = resultat.getVedtaksbrev();

            LOG.info("brevType: {}, Vedtaksbrev: {}", brevType, resultatBrev);
            // (gjelderAutomatiskBrev == null || Boolean.FALSE.equals(gjelderAutomatiskBrev))
            if (DokumentForhandsvisning.DokumentType.OVERSTYRT.equals(brevType) && Vedtaksbrev.FRITEKST.equals(resultatBrev)) {
                LOG.info("Utleder Fritekst mal.");
                bestillingDokumentMal = DokumentMalType.FRITEKSTBREV;
            } else {
                LOG.info("Utleder Automatisk mal");
                var revurderingMedUendretUtfall = erRevurderingMedUendretUtfall(behandling);
                var erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering = erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering(
                    resultat.getBehandlingResultatType(), resultat.getKonsekvenserForYtelsen(), behandling.getId());

                var erRevurderingMedUendretUtfall = revurderingMedUendretUtfall || erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering;

                LOG.info("revurderingMedUendretUtfall: {}, erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering: {}", revurderingMedUendretUtfall,
                    erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering);

                var klageVurdering = finnKlageVurdering(behandling);

                var dokumentMal = velgDokumentMalForForhåndsvisningAvVedtak(behandling, resultat.getBehandlingResultatType(),
                    resultat.getKonsekvenserForYtelsen(), erRevurderingMedUendretUtfall, klageVurdering);

                LOG.info("Utledet {} dokumentMal for {}", dokumentMal, behandlingUuid);
                bestillingDokumentMal = dokumentMal;
            }
        }
        return brev.forhåndsvis(legForhåndsvisningDto(bestilling, bestillingDokumentMal));
    }

    private DokumentForhåndsvisDto legForhåndsvisningDto(DokumentForhandsvisning bestilling, DokumentMalType bestillingDokumentMal) {
        return new DokumentForhåndsvisDto(
            bestilling.behandlingUuid(),
            mapDokumentMal(bestillingDokumentMal),
            mapRevurderignÅrsak(bestilling.revurderingÅrsak()),
            bestilling.tittel(),
            bestilling.fritekst()
        );
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

    private boolean harSendtVarselOmRevurdering(Long behandlingId) {
        return dokumentBehandlingTjeneste.erDokumentBestilt(behandlingId, DokumentMalType.VARSEL_OM_REVURDERING);
    }

}
