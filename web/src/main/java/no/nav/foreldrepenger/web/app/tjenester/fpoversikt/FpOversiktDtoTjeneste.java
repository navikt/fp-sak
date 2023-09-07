package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.foreldrepenger.kompletthet.ManglendeVedlegg;

@ApplicationScoped
class FpOversiktDtoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FpOversiktDtoTjeneste.class);

    private static final String UNEXPECTED_VALUE = "Unexpected value: ";
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private FpDtoTjeneste fpDtoTjeneste;
    private SvpDtoTjeneste svpDtoTjeneste;
    private EsDtoTjeneste esDtoTjeneste;
    private KompletthetsjekkerProvider kompletthetsjekkerProvider;
    private InntektsmeldingDtoTjeneste inntektsmeldingTjeneste;

    @Inject
    FpOversiktDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                 FpDtoTjeneste fpDtoTjeneste,
                                 SvpDtoTjeneste svpDtoTjeneste,
                                 EsDtoTjeneste esDtoTjeneste,
                                 KompletthetsjekkerProvider kompletthetsjekkerProvider,
                                 InntektsmeldingDtoTjeneste inntektsmeldingTjeneste) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fpDtoTjeneste = fpDtoTjeneste;
        this.svpDtoTjeneste = svpDtoTjeneste;
        this.esDtoTjeneste = esDtoTjeneste;
        this.kompletthetsjekkerProvider = kompletthetsjekkerProvider;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    FpOversiktDtoTjeneste() {
        //CDI
    }

    Sak hentSak(String saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummer)).orElseThrow();
        LOG.info("Henter sak {}", fagsak.getSaksnummer());
        return switch (fagsak.getYtelseType()) {
            case ENGANGSTØNAD -> esDtoTjeneste.hentSak(fagsak);
            case FORELDREPENGER -> fpDtoTjeneste.hentSak(fagsak);
            case SVANGERSKAPSPENGER -> svpDtoTjeneste.hentSak(fagsak);
            case UDEFINERT -> throw new IllegalStateException(UNEXPECTED_VALUE + fagsak.getYtelseType());
        };
    }

    Set<InntektsmeldingDto> hentInntektsmeldingerForSak(String saksnummer) {
        return inntektsmeldingTjeneste.hentInntektsmeldingerForSak(new Saksnummer(saksnummer));
    }

    List<DokumentTyperDto> hentmanglendeVedleggForSak(String saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(saksnummer)).orElseThrow();
        var behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakIdReadOnly(fagsak.getId());
        if (behandlingOpt.isEmpty()) {
            return List.of();
        }
        var behandling = behandlingOpt.get();
        var kompletthetsjekker = kompletthetsjekkerProvider.finnKompletthetsjekkerFor(fagsak.getYtelseType(), behandling.getType());
        return tilDto(kompletthetsjekker.utledAlleManglendeVedleggForForsendelse(BehandlingReferanse.fra(behandling)));
    }

    private List<DokumentTyperDto> tilDto(List<ManglendeVedlegg> manglendeVedleggs) {
        return manglendeVedleggs.stream()
                .map(ManglendeVedlegg::getDokumentType)
                .map(FpOversiktDtoTjeneste::tilRelevantDokumenttypeID)
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
            case ANNET_SKJEMA_IKKE_NAV -> DokumentTyperDto.I000049;
            case BEKREFTELSE_DELTAR_KVALIFISERINGSPROGRAM -> DokumentTyperDto.I000051;
            case ANNET -> DokumentTyperDto.I000060;
            case BEKREFTELSE_FRA_STUDIESTED -> DokumentTyperDto.I000061;
            case BEKREFTELSE_VENTET_FØDSELSDATO -> DokumentTyperDto.I000062;
            case FØDSELSATTEST -> DokumentTyperDto.I000063;
            case ELEVDOKUMENTASJON_LÆRESTED -> DokumentTyperDto.I000064;
            case BEKREFTELSE_FRA_ARBEIDSGIVER -> DokumentTyperDto.I000065;
            case KOPI_SKATTEMELDING -> DokumentTyperDto.I000066;
            case I000109 -> DokumentTyperDto.I000109;
            case I000110 -> DokumentTyperDto.I000110;
            case I000111 -> DokumentTyperDto.I000111;
            case I000112 -> DokumentTyperDto.I000112;
            case DOK_HV -> DokumentTyperDto.I000116;
            case DOK_NAV_TILTAK -> DokumentTyperDto.I000117;
            default -> throw new IllegalStateException("Ukjent manglende vedlegg dokumentTypeId" + dokumentTypeId);
        };

    }
}
