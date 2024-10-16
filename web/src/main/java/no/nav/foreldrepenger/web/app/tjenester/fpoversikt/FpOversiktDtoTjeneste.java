package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
class FpOversiktDtoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(FpOversiktDtoTjeneste.class);

    private static final String UNEXPECTED_VALUE = "Unexpected value: ";
    private FagsakRepository fagsakRepository;
    private FpDtoTjeneste fpDtoTjeneste;
    private SvpDtoTjeneste svpDtoTjeneste;
    private EsDtoTjeneste esDtoTjeneste;
    private InntektsmeldingDtoTjeneste inntektsmeldingTjeneste;
    private ManglendeVedleggDtoTjeneste manglendeVedleggDtoTjeneste;

    @Inject
    FpOversiktDtoTjeneste(FagsakRepository fagsakRepository,
                          FpDtoTjeneste fpDtoTjeneste,
                          SvpDtoTjeneste svpDtoTjeneste,
                          EsDtoTjeneste esDtoTjeneste,
                          InntektsmeldingDtoTjeneste inntektsmeldingTjeneste,
                          ManglendeVedleggDtoTjeneste manglendeVedleggDtoTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.fpDtoTjeneste = fpDtoTjeneste;
        this.svpDtoTjeneste = svpDtoTjeneste;
        this.esDtoTjeneste = esDtoTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.manglendeVedleggDtoTjeneste = manglendeVedleggDtoTjeneste;
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

    List<FpOversiktInntektsmeldingDto> hentInntektsmeldingerForSak(String saksnummer) {
        return inntektsmeldingTjeneste.hentInntektsmeldingerForSak(new Saksnummer(saksnummer));
    }

    List<DokumentTyperDto> hentManglendeVedleggForSak(String saksnummer) {
        return manglendeVedleggDtoTjeneste.hentManglendeVedleggForSak(new Saksnummer(saksnummer));
    }
}
