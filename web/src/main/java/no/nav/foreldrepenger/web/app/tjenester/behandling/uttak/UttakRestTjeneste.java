package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.List;
import java.util.Map;
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
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakArbeidsforholdTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.KontrollerAktivitetskravDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.KontrollerFaktaPeriodeTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.SaldoerDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.SvangerskapspengerUttakResultatDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.UttakPeriodegrenseDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.UttakPerioderDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BehandlingMedUttaksperioderDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerAktivitetskravPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaDataDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SaldoerDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SvangerskapspengerUttakResultatDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakPeriodegrenseDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPerioderDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(UttakRestTjeneste.BASE_PATH)
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class UttakRestTjeneste {

    static final String BASE_PATH = "/behandling/uttak";
    private static final String FAKTA_ARBEIDSFORHOLD_PART_PATH = "/fakta-arbeidsforhold";
    public static final String FAKTA_ARBEIDSFORHOLD_PATH = BASE_PATH + FAKTA_ARBEIDSFORHOLD_PART_PATH;
    private static final String RESULTAT_SVANGERSKAPSPENGER_PART_PATH = "/resultat-svangerskapspenger";
    public static final String RESULTAT_SVANGERSKAPSPENGER_PATH = BASE_PATH + RESULTAT_SVANGERSKAPSPENGER_PART_PATH;
    private static final String PERIODE_GRENSE_PART_PATH = "/periode-grense";
    public static final String PERIODE_GRENSE_PATH = BASE_PATH + PERIODE_GRENSE_PART_PATH;
    private static final String RESULTAT_PERIODER_PART_PATH = "/resultat-perioder";
    public static final String RESULTAT_PERIODER_PATH = BASE_PATH + RESULTAT_PERIODER_PART_PATH;
    private static final String KONTROLLER_FAKTA_PERIODER_PART_PATH = "/kontroller-fakta-perioder";
    public static final String KONTROLLER_FAKTA_PERIODER_PATH = BASE_PATH + KONTROLLER_FAKTA_PERIODER_PART_PATH;
    private static final String KONTROLLER_AKTIVTETSKRAV_PART_PATH = "/kontroller-aktivitetskrav";
    public static final String  KONTROLLER_AKTIVTETSKRAV_PATH = BASE_PATH + KONTROLLER_AKTIVTETSKRAV_PART_PATH;
    private static final String STONADSKONTOER_GITT_UTTAKSPERIODER_PART_PATH = "/stonadskontoerGittUttaksperioder";
    public static final String STONADSKONTOER_GITT_UTTAKSPERIODER_PATH = BASE_PATH + STONADSKONTOER_GITT_UTTAKSPERIODER_PART_PATH;
    private static final String STONADSKONTOER_PART_PATH = "/stonadskontoer";
    public static final String STONADSKONTOER_PATH = BASE_PATH + STONADSKONTOER_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private SaldoerDtoTjeneste saldoerDtoTjeneste;
    private KontrollerFaktaPeriodeTjeneste kontrollerFaktaPeriodeTjeneste;
    private UttakPerioderDtoTjeneste uttakResultatPerioderDtoTjeneste;
    private UttakPeriodegrenseDtoTjeneste uttakPeriodegrenseDtoTjeneste;
    private SvangerskapspengerUttakResultatDtoTjeneste svpUttakResultatDtoTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private KontrollerAktivitetskravDtoTjeneste kontrollerAktivitetskravDtoTjeneste;

    public UttakRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public UttakRestTjeneste(BehandlingRepository behandlingRepository,
                             SaldoerDtoTjeneste saldoerDtoTjeneste,
                             KontrollerFaktaPeriodeTjeneste kontrollerFaktaPeriodeTjeneste,
                             UttakPerioderDtoTjeneste uttakResultatPerioderDtoTjeneste,
                             UttakPeriodegrenseDtoTjeneste uttakPeriodegrenseDtoTjeneste,
                             SvangerskapspengerUttakResultatDtoTjeneste svpUttakResultatDtoTjeneste,
                             UttakInputTjeneste uttakInputTjeneste,
                             KontrollerAktivitetskravDtoTjeneste kontrollerAktivitetskravDtoTjeneste) {
        this.uttakPeriodegrenseDtoTjeneste = uttakPeriodegrenseDtoTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.kontrollerFaktaPeriodeTjeneste = kontrollerFaktaPeriodeTjeneste;
        this.uttakResultatPerioderDtoTjeneste = uttakResultatPerioderDtoTjeneste;
        this.saldoerDtoTjeneste = saldoerDtoTjeneste;
        this.svpUttakResultatDtoTjeneste = svpUttakResultatDtoTjeneste;
        this.kontrollerAktivitetskravDtoTjeneste = kontrollerAktivitetskravDtoTjeneste;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(STONADSKONTOER_PART_PATH)
    @Operation(description = "Hent informasjon om stønadskontoer for behandling", summary = "Returnerer stønadskontoer for behandling", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public SaldoerDto getStonadskontoer(
            @NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Behandling behandling = hentBehandling(behandlingIdDto);
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            var uttakInput = uttakInputTjeneste.lagInput(behandling);
            return saldoerDtoTjeneste.lagStønadskontoerDto(uttakInput);
        }
        return defaultSvar();
    }

    @GET
    @Path(STONADSKONTOER_PART_PATH)
    @Operation(description = "Hent informasjon om stønadskontoer for behandling", summary = "Returnerer stønadskontoer for behandling", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public SaldoerDto getStonadskontoer(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return getStonadskontoer(new BehandlingIdDto(uuidDto));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(STONADSKONTOER_GITT_UTTAKSPERIODER_PART_PATH)
    @Operation(description = "Hent informasjon om stønadskontoer for behandling gitt uttaksperioder", summary = "Returnerer stønadskontoer for behandling", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public SaldoerDto getStonadskontoerGittUttaksperioder(
            @NotNull @Parameter(description = "Behandling og liste med uttaksperioder") @Valid BehandlingMedUttaksperioderDto dto) {
        BehandlingIdDto behandlingIdDto = dto.getBehandlingId();
        Behandling behandling = hentBehandling(behandlingIdDto);
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            UttakInput uttakInput = uttakInputTjeneste.lagInput(behandling);
            return saldoerDtoTjeneste.lagStønadskontoerDto(uttakInput, dto.getPerioder());
        }
        return defaultSvar();
    }

    private SaldoerDto defaultSvar() {
        return new SaldoerDto(Optional.empty(), Map.of(), 0);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(KONTROLLER_FAKTA_PERIODER_PART_PATH)
    @Operation(description = "Hent perioder for å kontrollere fakta ifbm uttak", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public KontrollerFaktaDataDto hentKontrollerFaktaPerioder(
            @NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Behandling behandling = hentBehandling(behandlingIdDto);
        return kontrollerFaktaPeriodeTjeneste.hentKontrollerFaktaPerioder(behandling.getId());
    }

    @GET
    @Path(KONTROLLER_AKTIVTETSKRAV_PART_PATH)
    @Operation(description = "Hent perioder for å kontrollere aktivtetskrav", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public List<KontrollerAktivitetskravPeriodeDto> hentKontrollerAktivitetskrav(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return kontrollerAktivitetskravDtoTjeneste.lagDtos(uuidDto);
    }

    @GET
    @Path(KONTROLLER_FAKTA_PERIODER_PART_PATH)
    @Operation(description = "Hent perioder for å kontrollere fakta ifbm uttak", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public KontrollerFaktaDataDto hentKontrollerFaktaPerioder(
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return hentKontrollerFaktaPerioder(new BehandlingIdDto(uuidDto));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(RESULTAT_PERIODER_PART_PATH)
    @Operation(description = "Henter uttaksresultatperioder", summary = "Returnerer uttaksresultatperioder", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public UttakResultatPerioderDto hentUttakResultatPerioder(
            @NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Behandling behandling = hentBehandling(behandlingIdDto);
        return uttakResultatPerioderDtoTjeneste.mapFra(behandling).orElse(null);
    }

    @GET
    @Path(RESULTAT_PERIODER_PART_PATH)
    @Operation(description = "Henter uttaksresultatperioder", summary = "Returnerer uttaksresultatperioder", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public UttakResultatPerioderDto hentUttakResultatPerioder(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return hentUttakResultatPerioder(new BehandlingIdDto(uuidDto));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(PERIODE_GRENSE_PART_PATH)
    @Operation(description = "Henter uttakperiodegrense", summary = "Returnerer uttakperiodegrense", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public UttakPeriodegrenseDto hentUttakPeriodegrense(
            @NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Behandling behandling = hentBehandling(behandlingIdDto);
        var input = uttakInputTjeneste.lagInput(behandling);
        return uttakPeriodegrenseDtoTjeneste.mapFra(input).orElse(null);
    }

    @GET
    @Path(PERIODE_GRENSE_PART_PATH)
    @Operation(description = "Henter uttakperiodegrense", summary = "Returnerer uttakperiodegrense", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public UttakPeriodegrenseDto hentUttakPeriodegrense(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return hentUttakPeriodegrense(new BehandlingIdDto(uuidDto));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(FAKTA_ARBEIDSFORHOLD_PART_PATH)
    @Operation(description = "Henter arbeidsforhold som er relevant for fakta uttak", summary = "Henter arbeidsforhold som er relevant for fakta uttak", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public List<ArbeidsforholdDto> hentArbeidsforhold(
            @NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Behandling behandling = hentBehandling(behandlingIdDto);
        var input = uttakInputTjeneste.lagInput(behandling);
        return FaktaUttakArbeidsforholdTjeneste.hentArbeidsforhold(input);
    }

    @GET
    @Path(FAKTA_ARBEIDSFORHOLD_PART_PATH)
    @Operation(description = "Henter arbeidsforhold som er relevant for fakta uttak", summary = "Henter arbeidsforhold som er relevant for fakta uttak", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public List<ArbeidsforholdDto> hentArbeidsforhold(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return hentArbeidsforhold(new BehandlingIdDto(uuidDto));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(RESULTAT_SVANGERSKAPSPENGER_PART_PATH)
    @Operation(description = "Henter svangerskapspenger uttaksresultat", summary = "Returnerer svangerskapspenger uttaksresultat", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    @Deprecated
    public SvangerskapspengerUttakResultatDto hentSvangerskapspengerUttakResultat(
            @NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Behandling behandling = hentBehandling(behandlingIdDto);
        return svpUttakResultatDtoTjeneste.mapFra(behandling).orElse(null);
    }

    @GET
    @Path(RESULTAT_SVANGERSKAPSPENGER_PART_PATH)
    @Operation(description = "Henter svangerskapspenger uttaksresultat", summary = "Returnerer svangerskapspenger uttaksresultat", tags = "uttak")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.FAGSAK)
    public SvangerskapspengerUttakResultatDto hentSvangerskapspengerUttakResultat(
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return hentSvangerskapspengerUttakResultat(new BehandlingIdDto(uuidDto));
    }

    private Behandling hentBehandling(BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        return behandlingIdDto.getBehandlingId() != null
                ? behandlingRepository.hentBehandling(behandlingId)
                : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
    }
}
