package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak;

import static no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers.UuidAbacDataSupplier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakArbeidsforholdTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.SaldoerDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.SvangerskapspengerUttakResultatDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.UttakPerioderDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon.DokumentasjonVurderingBehovDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dokumentasjon.DokumentasjonVurderingBehovDtoTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BehandlingMedUttaksperioderDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SaldoerDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SvangerskapspengerUttakResultatDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakResultatPerioderDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.eøs.AvklarUttakEøsForAnnenforelderDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.eøs.AvklarUttakEøsForAnnenforelderTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta.FaktaUttakPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta.FaktaUttakPeriodeDtoTjeneste;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

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
    private static final String RESULTAT_PERIODER_PART_PATH = "/resultat-perioder";
    public static final String RESULTAT_PERIODER_PATH = BASE_PATH + RESULTAT_PERIODER_PART_PATH;
    private static final String FAKTA_UTTAK_PART_PATH = "/kontroller-fakta-perioder-v2";
    public static final String FAKTA_UTTAK_PATH = BASE_PATH + FAKTA_UTTAK_PART_PATH;
    private static final String FAKTA_UTTAK_ANNENPART_EØS_PART_PATH = "/uttak-eos-annenpart";
    public static final String FAKTA_UTTAK_EØS_PATH = BASE_PATH + FAKTA_UTTAK_ANNENPART_EØS_PART_PATH;
    private static final String STONADSKONTOER_GITT_UTTAKSPERIODER_PART_PATH = "/stonadskontoerGittUttaksperioder";
    public static final String STONADSKONTOER_GITT_UTTAKSPERIODER_PATH = BASE_PATH + STONADSKONTOER_GITT_UTTAKSPERIODER_PART_PATH;
    private static final String STONADSKONTOER_PART_PATH = "/stonadskontoer";
    public static final String STONADSKONTOER_PATH = BASE_PATH + STONADSKONTOER_PART_PATH;
    private static final String VURDER_DOKUMENTASJON_PART_PATH = "/vurder-dokumentasjon";
    public static final String VURDER_DOKUMENTASJON_PATH = BASE_PATH + VURDER_DOKUMENTASJON_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private SaldoerDtoTjeneste saldoerDtoTjeneste;
    private UttakPerioderDtoTjeneste uttakResultatPerioderDtoTjeneste;
    private SvangerskapspengerUttakResultatDtoTjeneste svpUttakResultatDtoTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private DokumentasjonVurderingBehovDtoTjeneste dokumentasjonVurderingBehovDtoTjeneste;
    private FaktaUttakPeriodeDtoTjeneste faktaUttakPeriodeDtoTjeneste;
    private AvklarUttakEøsForAnnenforelderTjeneste avklarUttakEøsForAnnenforelderTjeneste;

    @Inject
    public UttakRestTjeneste(BehandlingRepository behandlingRepository,
                             SaldoerDtoTjeneste saldoerDtoTjeneste,
                             UttakPerioderDtoTjeneste uttakResultatPerioderDtoTjeneste,
                             SvangerskapspengerUttakResultatDtoTjeneste svpUttakResultatDtoTjeneste,
                             UttakInputTjeneste uttakInputTjeneste,
                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                             DokumentasjonVurderingBehovDtoTjeneste dokumentasjonVurderingBehovDtoTjeneste,
                             FaktaUttakPeriodeDtoTjeneste faktaUttakPeriodeDtoTjeneste,
                             AvklarUttakEøsForAnnenforelderTjeneste avklarUttakEøsForAnnenforelderTjeneste) {
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.uttakResultatPerioderDtoTjeneste = uttakResultatPerioderDtoTjeneste;
        this.saldoerDtoTjeneste = saldoerDtoTjeneste;
        this.svpUttakResultatDtoTjeneste = svpUttakResultatDtoTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.dokumentasjonVurderingBehovDtoTjeneste = dokumentasjonVurderingBehovDtoTjeneste;
        this.faktaUttakPeriodeDtoTjeneste = faktaUttakPeriodeDtoTjeneste;
        this.avklarUttakEøsForAnnenforelderTjeneste = avklarUttakEøsForAnnenforelderTjeneste;
    }

    public UttakRestTjeneste() {
        // for CDI proxy
    }

    @GET
    @Path(STONADSKONTOER_PART_PATH)
    @Operation(description = "Hent informasjon om stønadskontoer for behandling", summary = "Returnerer stønadskontoer for behandling", tags = "uttak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public SaldoerDto getStonadskontoer(@TilpassetAbacAttributt(supplierClass = UuidAbacDataSupplier.class)
                                            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = hentBehandling(uuidDto);
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            var uttakInput = uttakInputTjeneste.lagInput(behandling);
            return saldoerDtoTjeneste.lagStønadskontoerDto(uttakInput);
        }
        return defaultSvar();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(STONADSKONTOER_GITT_UTTAKSPERIODER_PART_PATH)
    @Operation(description = "Hent informasjon om stønadskontoer for behandling gitt uttaksperioder", summary = "Returnerer stønadskontoer for behandling", tags = "uttak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public SaldoerDto getStonadskontoerGittUttaksperioder(@TilpassetAbacAttributt(supplierClass = BehandlingMedUttakAbacSupplier.class)
            @NotNull @Parameter(description = "Behandling og liste med uttaksperioder") @Valid BehandlingMedUttaksperioderDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.getBehandlingUuid());
        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            var uttakInput = uttakInputTjeneste.lagInput(behandling);
            return saldoerDtoTjeneste.lagStønadskontoerDto(uttakInput, dto.getPerioder());
        }
        return defaultSvar();
    }

    private SaldoerDto defaultSvar() {
        return new SaldoerDto(Map.of(), 0);
    }

    @GET
    @Path(RESULTAT_PERIODER_PART_PATH)
    @Operation(description = "Henter uttaksresultatperioder", summary = "Returnerer uttaksresultatperioder", tags = "uttak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public UttakResultatPerioderDto hentUttakResultatPerioder(@TilpassetAbacAttributt(supplierClass = UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = hentBehandling(uuidDto);
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        return uttakResultatPerioderDtoTjeneste.mapFra(behandling, skjæringstidspunkt);
    }

    @GET
    @Path(FAKTA_ARBEIDSFORHOLD_PART_PATH)
    @Operation(description = "Henter arbeidsforhold som er relevant for fakta uttak", summary = "Henter arbeidsforhold som er relevant for fakta uttak", tags = "uttak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public List<ArbeidsforholdDto> hentArbeidsforhold(@TilpassetAbacAttributt(supplierClass = UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = hentBehandling(uuidDto);
        var input = uttakInputTjeneste.lagInput(behandling);
        return FaktaUttakArbeidsforholdTjeneste.hentArbeidsforhold(input);
    }

    @GET
    @Path(RESULTAT_SVANGERSKAPSPENGER_PART_PATH)
    @Operation(description = "Henter svangerskapspenger uttaksresultat", summary = "Returnerer svangerskapspenger uttaksresultat", tags = "uttak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public SvangerskapspengerUttakResultatDto hentSvangerskapspengerUttakResultat(@TilpassetAbacAttributt(supplierClass = UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = hentBehandling(uuidDto);
        return svpUttakResultatDtoTjeneste.mapFra(behandling).orElse(null);
    }


    @GET
    @Path(VURDER_DOKUMENTASJON_PART_PATH)
    @Operation(description = "Hent perioder med behov for dokumentasjon", tags = "uttak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public List<DokumentasjonVurderingBehovDto> hentDokumentasjonVurderingBehov(@TilpassetAbacAttributt(supplierClass = UuidAbacDataSupplier.class) @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return dokumentasjonVurderingBehovDtoTjeneste.lagDtos(uuidDto);
    }

    @GET
    @Path(FAKTA_UTTAK_PART_PATH)
    @Operation(description = "Hent perioder for fakta om uttak", tags = "uttak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public List<FaktaUttakPeriodeDto> hentFaktaUttakPerioder(@TilpassetAbacAttributt(supplierClass = UuidAbacDataSupplier.class) @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return faktaUttakPeriodeDtoTjeneste.lagDtos(uuidDto);
    }

    @GET
    @Path(FAKTA_UTTAK_ANNENPART_EØS_PART_PATH)
    @Operation(description = "Hent eøs uttaksperioder for annenpart registert i EØS", tags = "uttak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public List<AvklarUttakEøsForAnnenforelderDto.EøsUttakPeriodeDto> hentAnnenpartPerioder(@TilpassetAbacAttributt(supplierClass = UuidAbacDataSupplier.class) @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return avklarUttakEøsForAnnenforelderTjeneste.annenpartsPerioder(uuidDto);
    }

    private Behandling hentBehandling(UuidDto uuidDto) {
        return behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
    }

    public static class BehandlingMedUttakAbacSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (BehandlingMedUttaksperioderDto) obj;
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_UUID, req.getBehandlingUuid());
        }
    }
}
