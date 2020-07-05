package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.InntektArbeidYtelseDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.InntektArbeidYtelseDtoMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste.UtledArbeidsforholdParametere;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(InntektArbeidYtelseRestTjeneste.BASE_PATH)
@Transactional
public class InntektArbeidYtelseRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String INNTEKT_ARBEID_YTELSE_PART_PATH = "/inntekt-arbeid-ytelse";
    public static final String INNTEKT_ARBEID_YTELSE_PATH = BASE_PATH + INNTEKT_ARBEID_YTELSE_PART_PATH; //NOSONAR TFP-2234

    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseDtoMapper dtoMapper;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;

    private InntektArbeidYtelseTjeneste iayTjeneste;

    public InntektArbeidYtelseRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InntektArbeidYtelseRestTjeneste(BehandlingRepository behandlingRepository,
                                           InntektArbeidYtelseDtoMapper dtoMapper,
                                           PersonopplysningTjeneste personopplysningTjeneste,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.dtoMapper = dtoMapper;
    }

    @POST
    @Path(INNTEKT_ARBEID_YTELSE_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent informasjon om innhentet og avklart inntekter, arbeid og ytelser",
        summary = ("Returnerer info om innhentet og avklart inntekter/arbeid og ytelser for bruker, inkludert hva bruker har vedlagt søknad."),
        tags = "inntekt-arbeid-ytelse",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer InntektArbeidYtelseDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = InntektArbeidYtelseDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public InntektArbeidYtelseDto getInntektArbeidYtelser(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return getInntektArbeidYtelserFraBehandling(behandling);
    }

    @GET
    @Path(INNTEKT_ARBEID_YTELSE_PART_PATH)
    @Operation(description = "Hent informasjon om innhentet og avklart inntekter, arbeid og ytelser",
        summary = ("Returnerer info om innhentet og avklart inntekter/arbeid og ytelser for bruker, inkludert hva bruker har vedlagt søknad."),
        tags = "inntekt-arbeid-ytelse",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Returnerer InntektArbeidYtelseDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = InntektArbeidYtelseDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public InntektArbeidYtelseDto getInntektArbeidYtelser(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getInntektArbeidYtelser(new BehandlingIdDto(uuidDto));
    }

    private InntektArbeidYtelseDto getInntektArbeidYtelserFraBehandling(Behandling behandling) {
        Skjæringstidspunkt skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());

        if (erSkjæringstidspunktIkkeUtledet(skjæringstidspunkt)) {
            // Tilfelle papirsøknad før registrering
            return new InntektArbeidYtelseDto();
        }
        var grunnlag = iayTjeneste.finnGrunnlag(behandling.getId());
        if (grunnlag.isEmpty()) {
            // Fins ikke ennå, returnerer tom dto for legacy kompatibilitet med frontend
            return new InntektArbeidYtelseDto();
        }
        InntektArbeidYtelseGrunnlag iayg = grunnlag.get();

        // finn annen part
        Optional<AktørId> annenPartAktørId = getAnnenPart(behandling.getId(), behandling);
        UtledArbeidsforholdParametere param = new UtledArbeidsforholdParametere(behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD));

        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);

        var sakInntektsmeldinger = iayTjeneste.hentInntektsmeldinger(behandling.getFagsak().getSaksnummer());
        return dtoMapper.mapFra(ref, iayg, sakInntektsmeldinger, annenPartAktørId, param);
    }

    private Optional<AktørId> getAnnenPart(Long behandlingId, Behandling behandling) {
        LocalDate personopplysningTidspunkt = LocalDate.now(); // TODO: Hvorfor bruker denne dagens dato og ikke skjæringstidspunkt? (fra InntektArbeidYtelseDtoMapper commit 81e8624)
        Optional<PersonopplysningerAggregat> personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(behandlingId, behandling.getAktørId(), personopplysningTidspunkt);
        Optional<AktørId> annenPartAktørId = personopplysningerAggregat.flatMap(PersonopplysningerAggregat::getOppgittAnnenPart).map(OppgittAnnenPartEntitet::getAktørId);
        return annenPartAktørId;
    }

    private boolean erSkjæringstidspunktIkkeUtledet(Skjæringstidspunkt skjæringstidspunkt) {
        return skjæringstidspunkt == null || !skjæringstidspunkt.getSkjæringstidspunktHvisUtledet().isPresent();
    }
}
