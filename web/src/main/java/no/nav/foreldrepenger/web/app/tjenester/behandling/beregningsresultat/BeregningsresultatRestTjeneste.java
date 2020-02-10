package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.TilkjentYtelse;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.app.BeregningsresultatTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatEngangsstønadDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto.BeregningsresultatMedUttaksplanDto;
import no.nav.foreldrepenger.økonomi.tilkjentytelse.TilkjentYtelseTjeneste;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(BeregningsresultatRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transaction
public class BeregningsresultatRestTjeneste {

    static final String BASE_PATH = "/behandling/beregningsresultat";
    private static final String ENGANGSTONAD_PART_PATH = "/engangsstonad";
    public static final String ENGANGSTONAD_PATH = BASE_PATH + ENGANGSTONAD_PART_PATH;
    private static final String FORELDREPENGER_PART_PATH = "/foreldrepenger";
    public static final String FORELDREPENGER_PATH = BASE_PATH + FORELDREPENGER_PART_PATH;
    private static final String TILKJENTYTELSE_PART_PATH = "/tilkjentytelse";
    public static final String TILKJENTYTELSE_PATH = BASE_PATH + TILKJENTYTELSE_PART_PATH; //NOSONAR TFP-2234
    private static final String HAR_SAMME_RESULTAT_PART_PATH = "/har-samme-resultat";
    public static final String HAR_SAMME_RESULTAT_PATH = BASE_PATH + HAR_SAMME_RESULTAT_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatTjeneste beregningsresultatTjeneste;
    private TilkjentYtelseTjeneste tilkjentYtelseTjeneste;

    public BeregningsresultatRestTjeneste() {
        // for resteasy
    }

    @Inject
    public BeregningsresultatRestTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                          BeregningsresultatTjeneste beregningsresultatMedUttaksplanTjeneste, TilkjentYtelseTjeneste tilkjentYtelseTjeneste) {
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.beregningsresultatTjeneste = beregningsresultatMedUttaksplanTjeneste;
        this.tilkjentYtelseTjeneste = tilkjentYtelseTjeneste;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(ENGANGSTONAD_PART_PATH)
    @Operation(description = "Hent beregningsresultat med uttaksplan for engangsstønad behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsresultatEngangsstønadDto hentBeregningsresultatEngangsstønad(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatEnkel(behandling.getId()).orElse(null);
    }

    @GET
    @Path(ENGANGSTONAD_PART_PATH)
    @Operation(description = "Hent beregningsresultat med uttaksplan for engangsstønad behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsresultatEngangsstønadDto hentBeregningsresultatEngangsstønad(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatEnkel(behandling.getId()).orElse(null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(FORELDREPENGER_PART_PATH)
    @Operation(description = "Hent beregningsresultat med uttaksplan for foreldrepenger behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsresultatMedUttaksplanDto hentBeregningsresultatForeldrepenger(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatMedUttaksplan(behandling)
            .orElse(null);
    }

    @GET
    @Path(FORELDREPENGER_PART_PATH)
    @Operation(description = "Hent beregningsresultat med uttaksplan for foreldrepenger behandling", summary = ("Returnerer beregningsresultat med uttaksplan for behandling."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public BeregningsresultatMedUttaksplanDto hentBeregningsresultatForeldrepenger(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return beregningsresultatTjeneste.lagBeregningsresultatMedUttaksplan(behandling).orElse(null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path(TILKJENTYTELSE_PART_PATH)
    @Operation(description = "Hent beregningsresultat", summary = ("Brukes av fpoppdrag."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @Deprecated
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public TilkjentYtelse hentTilkjentYtelse(@NotNull @Parameter(description = "BehandlingId for aktuell behandling") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingRepository.hentBehandling(behandlingId)
            : behandlingRepository.hentBehandling(behandlingIdDto.getBehandlingUuid());
        return tilkjentYtelseTjeneste.hentilkjentYtelse(behandling.getId());
    }

    @GET
    @Path(TILKJENTYTELSE_PART_PATH)
    @Operation(description = "Hent beregningsresultat", summary = ("Brukes av fpoppdrag."), tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public TilkjentYtelse hentTilkjentYtelse(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return tilkjentYtelseTjeneste.hentilkjentYtelse(behandling.getId());
    }

    @GET
    @Path(HAR_SAMME_RESULTAT_PART_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Har revurdering samme resultat som original behandling", tags = "beregningsresultat")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Boolean harRevurderingSammeResultatSomOriginalBehandling(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        Behandling behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        if (!BehandlingType.REVURDERING.getKode().equals(behandling.getType().getKode())) {
            throw new IllegalStateException("Behandling må være en revurdering");
        }

        var behandlingsresultat = behandling.getBehandlingsresultat();
        if (behandlingsresultat == null) {
            return false;
        }

        List<KonsekvensForYtelsen> konsekvenserForYtelsen = behandlingsresultat.getKonsekvenserForYtelsen();
        if (konsekvenserForYtelsen != null) {
            return konsekvenserForYtelsen.stream().anyMatch(kfy -> KonsekvensForYtelsen.INGEN_ENDRING.getKode().equals(kfy.getKode()));
        }

        Behandling originalBehandling = behandling.getOriginalBehandling().orElseThrow(() -> new IllegalStateException("Revurdering må ha originalbehandling"));
        Behandlingsresultat originaltBehandlingsresultat = originalBehandling.getBehandlingsresultat();
        BehandlingResultatType behandlingResultatType = behandlingsresultat.getBehandlingResultatType();

        boolean harSammeResultatType = behandlingResultatType.getKode().equals(originaltBehandlingsresultat.getBehandlingResultatType().getKode());
        boolean erInnvilget = BehandlingResultatType.INNVILGET.getKode().equals(behandlingResultatType.getKode());
        boolean erEngangsstønad = FagsakYtelseType.ENGANGSTØNAD.getKode().equals(behandling.getFagsakYtelseType().getKode());

        if (harSammeResultatType && erInnvilget && erEngangsstønad) {
            BeregningsresultatEngangsstønadDto beregningsresultatEngangsstønadDto = this.hentBeregningsresultatEngangsstønad(uuidDto);
            BeregningsresultatEngangsstønadDto originalBeregningsresultatEngangsstønadDto = this.hentBeregningsresultatEngangsstønad(new UuidDto(originalBehandling.getUuid()));
            return beregningsresultatEngangsstønadDto != null && beregningsresultatEngangsstønadDto.getAntallBarn() == originalBeregningsresultatEngangsstønadDto.getAntallBarn();
        }

        return harSammeResultatType;
    }
}
