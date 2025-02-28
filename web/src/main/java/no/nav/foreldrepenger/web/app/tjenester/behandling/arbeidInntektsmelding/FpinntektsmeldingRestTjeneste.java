package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(FpinntektsmeldingRestTjeneste.BASE_PATH)
@Transactional
public class FpinntektsmeldingRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FpinntektsmeldingRestTjeneste.class);

    static final String BASE_PATH = "/behandling";
    private static final String KONTROLLER_FORESPØRSEL_PART_PATH = "/inntektsmelding/kontroller-forespoersel";

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    FpinntektsmeldingRestTjeneste() {
        // CDI
    }

    @Inject
    public FpinntektsmeldingRestTjeneste(BehandlingRepository behandlingRepository,
                                         FagsakRepository fagsakRepository,
                                         InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste,
                                         SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    /**
     *
     * @param kontrollDto - spesifiserer et orgnr som har en åpen forespørsel, og hvilket saksnummer denne forespørselen tilhører
     * @return om det fortsatt er krav om inntektsmelding fra arbeidsgiveren
     */
    @POST
    @Path(KONTROLLER_FORESPØRSEL_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Sjekker om en sak fortsatt trenger inntektsmelding fra gitt orgnr.", summary = "Sjekker om en sak fortsatt trenger inntektsmelding fra gitt orgnr", tags = "arbeid-intektsmelding")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public Response kontrollerForespørselForSaksnummer(@TilpassetAbacAttributt(supplierClass = KontrollerForespørselDtoAbacDataSupplier.class)
                                   @NotNull @Parameter(description = "Saksnummer og orgnr der behov for inntektsmelding skal kontrolleres.") @Valid FpinntektsmeldingRestTjeneste.KontrollerForespørselRequestDto kontrollDto) {
        var virksomhet = Arbeidsgiver.virksomhet(kontrollDto.orgnr());
        LOG.info("Undersøker behov for inntektsmelding fra orgnr {} på saksnummer {}", virksomhet, kontrollDto.saksnummer());
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(kontrollDto.saksnummer())).orElseThrow();
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();
        var ref = BehandlingReferanse.fra(behandling);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId());
        // Denne tar ikke stilling til saksbehandlers avgjørelse om å få videre i behandlingen uten inntektsmelding
        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(ref, stp);
        LOG.info("Påkrevde inntektsmeldinger for orgnr: {}", manglendeInntektsmeldinger.keySet());
        var erFortsattKravOmInntektsmelding = manglendeInntektsmeldinger.keySet()
            .stream()
            .anyMatch(ag -> ag.equals(virksomhet));
        LOG.info("Er inntektsmelding påkrevd for orgnr {} i saksnummer {}: {}", virksomhet, ref.saksnummer(), erFortsattKravOmInntektsmelding);
        return Response.ok(new KontrollForespørselResponseDto(erFortsattKravOmInntektsmelding)).build();
    }

    public record KontrollerForespørselRequestDto(@NotNull @Digits(integer = 18, fraction = 0) String saksnummer, @NotNull @Digits(integer = 9, fraction = 0) String orgnr){}

    public record KontrollForespørselResponseDto(@NotNull Boolean erInntektsmeldingPåkrevd){}

    public static class KontrollerForespørselDtoAbacDataSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (KontrollerForespørselRequestDto) obj;
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.SAKSNUMMER, req.saksnummer);
        }
    }


}
