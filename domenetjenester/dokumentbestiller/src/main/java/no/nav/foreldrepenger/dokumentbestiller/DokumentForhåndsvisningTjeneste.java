package no.nav.foreldrepenger.dokumentbestiller;

import static no.nav.foreldrepenger.dokumentbestiller.formidling.BestillDokumentDtoMapper.mapDokumentMal;
import static no.nav.foreldrepenger.dokumentbestiller.formidling.BestillDokumentDtoMapper.mapRevurderignÅrsak;
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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.dokumentbestiller.formidling.Dokument;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.kontrakter.formidling.kodeverk.Saksnummer;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingHtmlDto;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentForhåndsvisDto;

@ApplicationScoped
public class DokumentForhåndsvisningTjeneste extends AbstractDokumentBestillerTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(DokumentForhåndsvisningTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private Dokument brev;

    DokumentForhåndsvisningTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentForhåndsvisningTjeneste(BehandlingRepository behandlingRepository,
                                           BehandlingsresultatRepository behandlingsresultatRepository,
                                           DokumentBehandlingTjeneste dokumentBehandlingTjeneste,
                                           KlageRepository klageRepository,
                                           ForeldrepengerUttakTjeneste uttakTjeneste,
                                           Dokument brev) {
        super(klageRepository);
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.brev = brev;
    }

    public byte[] forhåndsvisDokument(Long behandlingId, DokumentForhandsvisning bestilling) {
        var bestillingDokumentMal = maltypeFraBestillingEllersUtledFraBehandling(behandlingId, bestilling, bestilling.behandlingUuid());
        LOG.info("Forhåndsviser brev med mal {} for behandling {}", bestillingDokumentMal, bestilling.behandlingUuid());
        return brev.forhåndsvis(lagForhåndsvisningDto(bestilling, bestillingDokumentMal));
    }

    public String genererHtml(Behandling behandling) {
        var dokumentMalType = utledDokumentmalTypeFraBehandling(behandling.getId(), behandling.getUuid(), DokumentForhandsvisning.DokumentType.AUTOMATISK);
        if (DokumentMalType.FORELDREPENGER_INNVILGELSE.equals(dokumentMalType) && erUttakTomt(behandling)) {
            LOG.info("Genererer annuleringsbrev som utgangspunkt for overstyring av foreldrepenger som med tomt uttak: {}", behandling.getFagsak().getSaksnummer());
            dokumentMalType = DokumentMalType.FORELDREPENGER_ANNULLERT;
        }

        var dokumentBestillingHtmlDto = new DokumentBestillingHtmlDto(
            behandling.getUuid(),
            new Saksnummer(behandling.getSaksnummer().getVerdi()),
            mapDokumentMal(dokumentMalType)
        );

        LOG.info("Genererer HTML for brev med mal {} for behandling {}", dokumentBestillingHtmlDto.dokumentMal(), behandling.getUuid());
        return brev.genererHtml(dokumentBestillingHtmlDto);
    }

    private boolean erUttakTomt(Behandling behandling) {
        var uttakOpt = uttakTjeneste.hentHvisEksisterer(behandling.getId());
        if (uttakOpt.isEmpty() || uttakOpt.get().getGjeldendePerioder().isEmpty()) {
            return true;
        }

        return uttakOpt.get().getGjeldendePerioder().stream()
            .noneMatch(periode -> periode.harUtbetaling() || periode.harTrekkdager());
    }

    private DokumentMalType maltypeFraBestillingEllersUtledFraBehandling(Long behandlingId, DokumentForhandsvisning bestilling, UUID behandlingUuid) {
        if (bestilling.dokumentMal() != null) {
            return bestilling.dokumentMal();
        }
        return utledDokumentmalTypeFraBehandling(behandlingId, behandlingUuid, bestilling.dokumentType());
    }

    private DokumentMalType utledDokumentmalTypeFraBehandling(Long behandlingId, UUID behandlingUuid, DokumentForhandsvisning.DokumentType brevType) {
        LOG.info("Utleder dokumentMal for {}", behandlingUuid);

        var behandling = behandlingRepository.hentBehandling(behandlingUuid);
        var resultat = behandlingsresultatRepository.hent(behandling.getId());

        var resultatBrev = resultat.getVedtaksbrev();

        LOG.info("brevType: {}, Vedtaksbrev: {}", brevType, resultatBrev);
        // (gjelderAutomatiskBrev == null || Boolean.FALSE.equals(gjelderAutomatiskBrev))
        if (DokumentForhandsvisning.DokumentType.OVERSTYRT.equals(brevType) && Vedtaksbrev.FRITEKST.equals(resultatBrev)) {
            var fritekstMal = dokumentBehandlingTjeneste.hentMellomlagretOverstyring(behandlingId).isPresent()
                ? DokumentMalType.VEDTAKSBREV_FRITEKST_HTML
                : DokumentMalType.FRITEKSTBREV;

            LOG.info("Utledere maltype for fritekst: {}", fritekstMal);
            return fritekstMal;
        } else {
            LOG.info("Utleder Automatisk mal");
            var revurderingMedUendretUtfall = erRevurderingMedUendretUtfall(behandling);
            var erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering = erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering(resultat.getBehandlingResultatType(), resultat.getKonsekvenserForYtelsen(), behandling.getId());

            var erRevurderingMedUendretUtfall = revurderingMedUendretUtfall || erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering;

            LOG.info("revurderingMedUendretUtfall: {}, erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering: {}", revurderingMedUendretUtfall,
                erKunEndringIFordelingAvYtelsenOgHarSendtVarselOmRevurdering);

            var klageVurdering = finnKlageVurdering(behandling);

            var dokumentMal = velgDokumentMalForForhåndsvisningAvVedtak(behandling, resultat.getBehandlingResultatType(), resultat.getKonsekvenserForYtelsen(), erRevurderingMedUendretUtfall, klageVurdering);

            LOG.info("Utledet {} dokumentMal for {}", dokumentMal, behandlingUuid);
            return dokumentMal;
        }
    }

    private DokumentForhåndsvisDto lagForhåndsvisningDto(DokumentForhandsvisning bestilling, DokumentMalType bestillingDokumentMal) {
        return new DokumentForhåndsvisDto(
            bestilling.behandlingUuid(),
            new Saksnummer(bestilling.saksnummer().getVerdi()),
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
