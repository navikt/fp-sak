package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

@ApplicationScoped
class ManglendeVedleggDtoTjeneste {

    private Kompletthetsjekker kompletthetsjekker;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    ManglendeVedleggDtoTjeneste(Kompletthetsjekker kompletthetsjekker,
                                FagsakRepository fagsakRepository,
                                BehandlingRepository behandlingRepository) {
        this.kompletthetsjekker = kompletthetsjekker;
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
    }

    ManglendeVedleggDtoTjeneste() {
        //CDI
    }

    List<DokumentTyperDto> hentManglendeVedleggForSak(Saksnummer saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        var behandlingOpt = behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId())
            .stream()
            .filter(Behandling::erYtelseBehandling)
            .max(Comparator.comparing(Behandling::getOpprettetTidspunkt));
        if (behandlingOpt.isEmpty()) {
            return List.of();
        }
        var behandling = behandlingOpt.get();
        return tilDto(kompletthetsjekker.utledAlleManglendeVedleggForForsendelse(BehandlingReferanse.fra(behandling)));
    }

    private List<DokumentTyperDto> tilDto(List<ManglendeVedlegg> manglendeVedleggs) {
        return manglendeVedleggs.stream()
            .map(ManglendeVedlegg::getDokumentType)
            .map(ManglendeVedleggDtoTjeneste::tilRelevantDokumenttypeID)
            .toList();
    }

    private static DokumentTyperDto tilRelevantDokumenttypeID(DokumentTypeId dokumentTypeId) {
        return switch (dokumentTypeId) {
            case INNTEKTSOPPLYSNING_SELVSTENDIG -> DokumentTyperDto.I000007;
            case LEGEERKLÆRING -> DokumentTyperDto.I000023;
            case RESULTATREGNSKAP -> DokumentTyperDto.I000032;
            case DOK_FERIE -> DokumentTyperDto.I000036;
            case DOK_INNLEGGELSE -> DokumentTyperDto.I000037;
            case DOK_MORS_UTDANNING_ARBEID_SYKDOM -> DokumentTyperDto.I000038;
            case DOK_MILITÆR_SIVIL_TJENESTE -> DokumentTyperDto.I000039;
            case DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL -> DokumentTyperDto.I000041;
            case DOKUMENTASJON_AV_OMSORGSOVERTAKELSE -> DokumentTyperDto.I000042;
            case DOK_ETTERLØNN -> DokumentTyperDto.I000044;
            case BESKRIVELSE_FUNKSJONSNEDSETTELSE -> DokumentTyperDto.I000045;
            case BEKREFTELSE_DELTAR_KVALIFISERINGSPROGRAM -> DokumentTyperDto.I000051;
            case ANNET, ANNET_SKJEMA_IKKE_NAV, BREV, BREV_UTLAND, ANNET_SKJEMA_UTLAND_IKKE_NAV -> DokumentTyperDto.I000060;
            case BEKREFTELSE_FRA_STUDIESTED -> DokumentTyperDto.I000061;
            case BEKREFTELSE_VENTET_FØDSELSDATO -> DokumentTyperDto.I000062;
            case FØDSELSATTEST -> DokumentTyperDto.I000063;
            case BEKREFTELSE_FRA_ARBEIDSGIVER -> DokumentTyperDto.I000065;
            case KOPI_SKATTEMELDING -> DokumentTyperDto.I000066;
            case I000109 -> DokumentTyperDto.I000109;
            case I000110 -> DokumentTyperDto.I000110;
            case I000111 -> DokumentTyperDto.I000111;
            case I000112 -> DokumentTyperDto.I000112;
            case TILBAKEKREVING_UTTALSELSE -> DokumentTyperDto.I000114;
            case DOK_HV -> DokumentTyperDto.I000116;
            case DOK_NAV_TILTAK -> DokumentTyperDto.I000117;
            case SEN_SØKNAD -> DokumentTyperDto.I000118;
            case TILBAKEBETALING_UTTALSELSE -> DokumentTyperDto.I000119;
            case MOR_INNLAGT -> DokumentTyperDto.I000120;
            case MOR_SYK -> DokumentTyperDto.I000121;
            case FAR_INNLAGT -> DokumentTyperDto.I000122;
            case FAR_SYK -> DokumentTyperDto.I000123;
            case BARN_INNLAGT -> DokumentTyperDto.I000124;
            case MOR_ARBEID_STUDIE -> DokumentTyperDto.I000130;
            case MOR_STUDIE -> DokumentTyperDto.I000131;
            case MOR_ARBEID -> DokumentTyperDto.I000132;
            case MOR_KVALIFISERINGSPROGRAM -> DokumentTyperDto.I000133;
            case SKATTEMELDING -> DokumentTyperDto.I000140;
            case TERMINBEKREFTELSE -> DokumentTyperDto.I000141;
            default -> throw new IllegalStateException("Ukjent manglende vedlegg dokumentTypeId" + dokumentTypeId);
        };

    }
}
