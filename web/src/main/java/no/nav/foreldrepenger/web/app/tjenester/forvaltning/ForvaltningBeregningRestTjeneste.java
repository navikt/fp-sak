package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.folketrygdloven.kalkulator.felles.MeldekortUtils;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.YtelseFilterDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.abakus.mapping.IAYTilDtoMapper;
import no.nav.foreldrepenger.domene.abakus.mapping.KodeverkMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.BeregningSatsDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerEnhetDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningBeregning")
@ApplicationScoped
@Transactional
public class ForvaltningBeregningRestTjeneste {
    private static final Period MELDEKORT_PERIODE_UTV = Period.parse("P30D");
    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningBeregningRestTjeneste.class);

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    public ForvaltningBeregningRestTjeneste(
            BeregningsgrunnlagRepository beregningsgrunnlagRepository, BehandlingRepositoryProvider repositoryProvider,
            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
            BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningsgrunnlagInputProvider = beregningsgrunnlagInputProvider;
    }

    public ForvaltningBeregningRestTjeneste() {
        // CDI
    }

    @GET
    @Path("/satsHentGjeldende")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent liste av gjeldende eller nyeste sats", tags = "FORVALTNING-beregning", responses = {
            @ApiResponse(responseCode = "200", description = "Gjeldende satser", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = BeregningSatsDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public List<BeregningSatsDto> hentGjeldendeSatser() {
        return Set.of(BeregningSatsType.ENGANG, BeregningSatsType.GRUNNBELØP, BeregningSatsType.GSNITT).stream()
                .map(beregningsresultatRepository::finnGjeldendeSats)
                .map(BeregningSatsDto::new)
                .collect(Collectors.toList());
    }

    @POST
    @Path("/satsLagreNy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagre ny sats", tags = "FORVALTNING-beregning", responses = {
            @ApiResponse(responseCode = "200", description = "Gjeldende satser", content = @Content(array = @ArraySchema(arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = BeregningSatsDto.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = CREATE, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public List<BeregningSatsDto> lagreNySats(@BeanParam @Valid @NotNull BeregningSatsDto dto) {
        var type = dto.getSatsType();
        var brukTom = dto.getSatsTom() != null ? dto.getSatsTom() : LocalDate.now().plusYears(99);
        var gjeldende = beregningsresultatRepository.finnGjeldendeSats(type);
        if (!sjekkVerdierOK(dto, gjeldende, brukTom))
            throw new ForvaltningException("Ulovlige verdier " + dto);
        LOG.warn("SATSJUSTERTING: sjekk med produkteier om det er ventet, noter usedId i loggen {}", dto);
        gjeldende.setTomDato(dto.getSatsFom().minusDays(1));
        beregningsresultatRepository.lagreSats(gjeldende);
        var nysats = new BeregningSats(type, DatoIntervallEntitet.fraOgMedTilOgMed(dto.getSatsFom(), brukTom), dto.getSatsVerdi());
        beregningsresultatRepository.lagreSats(nysats);
        var nygjeldende = beregningsresultatRepository.finnGjeldendeSats(type);
        return Set.of(gjeldende, nygjeldende).stream().map(BeregningSatsDto::new).collect(Collectors.toList());
    }

    private boolean sjekkVerdierOK(BeregningSatsDto dto, BeregningSats gjeldende, LocalDate brukTom) {
        if (!brukTom.isAfter(dto.getSatsFom()) || !dto.getSatsFom().isAfter(gjeldende.getPeriode().getFomDato()))
            return false;
        if (BeregningSatsType.GRUNNBELØP.equals(gjeldende.getSatsType())) {
            return gjeldende.getPeriode().getTomDato().isAfter(dto.getSatsFom()) && Month.MAY.equals(dto.getSatsFom().getMonth())
                    && dto.getSatsFom().getDayOfMonth() == 1;
        }
        if (BeregningSatsType.ENGANG.equals(gjeldende.getSatsType())) {
            return gjeldende.getPeriode().getTomDato().isAfter(dto.getSatsFom());
        }
        // GSNITT skal være bounded
        return dto.getSatsTom() != null && dto.getSatsFom().equals(gjeldende.getPeriode().getTomDato().plusDays(1))
                && dto.getSatsTom().equals(dto.getSatsFom().plusYears(1).minusDays(1));
    }

    @POST
    @Path("/hentIAYGrunnlag")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter iayGrunnlag", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response hentIAYGrunnlag(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = getBehandling(dto);
        var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
        var inntektArbeidYtelseGrunnlagDto = new IAYTilDtoMapper(behandling.getAktørId(),
            KodeverkMapper.fraFagsakYtelseType(behandling.getFagsakYtelseType()),
            inntektArbeidYtelseGrunnlag.getEksternReferanse(), behandling.getUuid()).mapTilDto(inntektArbeidYtelseGrunnlag);
        return Response.ok(inntektArbeidYtelseGrunnlagDto).build();
    }

    private Behandling getBehandling(ForvaltningBehandlingIdDto dto) {
        var behandlingId = dto.getBehandlingId();
        return behandlingId == null ? behandlingRepository.hentBehandling(dto.getBehandlingUUID())
            : behandlingRepository.hentBehandling(behandlingId);
    }

    @POST
    @Path("/hentBeregningsgrunnlagInput")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter input for beregning", tags = "FORVALTNING-beregning")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response hentBeregningsgrunnlagInput(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        var behandling = getBehandling(dto);
        var inputTjeneste = beregningsgrunnlagInputProvider.getTjeneste(behandling.getFagsakYtelseType());
        var beregningsgrunnlagInput = inputTjeneste.lagInput(behandling.getId());
        var kalkulatorInputDto = MapTilKalkulatorInput.map(beregningsgrunnlagInput);
        if (kalkulatorInputDto == null) {
            return Response.noContent().build();

        }
        return Response.ok(kalkulatorInputDto).build();
    }

    /**
     *
     * Skal brukes for å feilsøke saker som kan ha blitt feilberegnet etter å ha
     * brukt feil meldekort, https://jira.adeo.no/browse/TFP-2890
     */
    @POST
    @Path("/hentMeldekortFeil")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter saker som må sjekkes for feilutbetaling grunnet feil meldekort som er brukt", tags = "FORVALTNING-beregning", responses = {
            @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(uniqueItems = true, arraySchema = @Schema(implementation = List.class), schema = @Schema(implementation = String.class)), mediaType = MediaType.APPLICATION_JSON))
    })
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT, sporingslogg = false)
    public Response hentMeldekortFeil() {
        var grunnlagList = beregningsgrunnlagRepository.hentGrunnlagForPotensielleFeilMeldekort();
        var behandlinger = grunnlagList.stream()
                .filter(gr -> !besteberegningErFastsatt(gr))
                .map(BeregningsgrunnlagGrunnlagEntitet::getBehandlingId)
                .map(behandlingRepository::hentBehandling)
                .collect(Collectors.toSet());
        LOG.info("Fant {} behandlinger som må sjekkes", behandlinger.size());
        Set<SaksnummerEnhetDto> liste = new HashSet<>();
        for (var behandling : behandlinger) {
            var grunnlagEntitet = grunnlagList.stream().filter(gr -> gr.getBehandlingId().equals(behandling.getId()))
                    .findFirst().orElseThrow();
            var skjæringstidspunkt = grunnlagEntitet.getBeregningsgrunnlag().map(BeregningsgrunnlagEntitet::getSkjæringstidspunkt)
                    .orElseThrow();
            var inntektArbeidYtelseGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId());
            var kalkGrunlag = IAYMapperTilKalkulus.mapGrunnlag(inntektArbeidYtelseGrunnlag, behandling.getAktørId());
            var ytelseFilter = new YtelseFilterDto(kalkGrunlag.getAktørYtelseFraRegister()).før(skjæringstidspunkt);
            var ytelseTyper = Set.of(FagsakYtelseType.DAGPENGER, FagsakYtelseType.ARBEIDSAVKLARINGSPENGER);
            var ytelseDto = MeldekortUtils.sisteVedtakFørStpForType(ytelseFilter, skjæringstidspunkt, ytelseTyper);

            if (ytelseDto.isPresent()) {
                var sisteVedtakFom = ytelseDto.get().getPeriode().getFomDato();
                var alleMeldekort = ytelseFilter.getFiltrertYtelser().stream()
                        .filter(ytelse -> ytelseTyper.contains(ytelse.getRelatertYtelseType()))
                        .flatMap(ytelse -> ytelse.getYtelseAnvist().stream()).collect(Collectors.toList());

                // Henter ut siste meldekort som startet før STP
                var sisteMeldekort = alleMeldekort.stream()
                        .filter(ytelseAnvist -> sisteVedtakFom.minus(MELDEKORT_PERIODE_UTV).isBefore(ytelseAnvist.getAnvistTOM()))
                        .filter(mk -> mk.getAnvistFOM().isBefore(skjæringstidspunkt))
                        .max(Comparator.comparing(YtelseAnvistDto::getAnvistFOM));

                if (sisteMeldekort.isPresent()) {
                    // Hvis dette meldekortet ikke var "helt" må saken muligens revurderes
                    var erMeldekortKomplett = skjæringstidspunkt.isAfter(sisteMeldekort.get().getAnvistTOM());
                    if (!erMeldekortKomplett) {
                        var dto = new SaksnummerEnhetDto(behandling.getFagsak().getSaksnummer().getVerdi(),
                                behandling.getBehandlendeEnhet());
                        liste.add(dto);
                    }
                }

            }
        }
        return Response.ok(liste).build();
    }


    private boolean besteberegningErFastsatt(BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        var tilfeller = grunnlag.getBeregningsgrunnlag()
                .map(BeregningsgrunnlagEntitet::getFaktaOmBeregningTilfeller)
                .orElse(Collections.emptyList());
        return tilfeller.contains(FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE);
    }


}
