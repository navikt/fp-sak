package no.nav.foreldrepenger.web.app.tjenester.vedtak;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.DRIFT;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandling.UuidDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.ekstern.RegenererVedtaksXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.ekstern.ValiderOgRegenererVedtaksXmlTask;
import no.nav.foreldrepenger.domene.vedtak.innsyn.VedtakInnsynTjeneste;
import no.nav.foreldrepenger.domene.vedtak.observer.VedtattYtelseTjeneste;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessApplikasjonTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.vedtakfattet.dto.AktørParam;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path(VedtakRestTjeneste.BASE_PATH)
@ApplicationScoped
public class VedtakRestTjeneste {

    static final String BASE_PATH = "/vedtak";
    private static final String HENT_VEDTAKSDOKUMENT_PART_PATH = "/hent-vedtaksdokument";
    public static final String HENT_VEDTAKSDOKUMENT_PATH = BASE_PATH + HENT_VEDTAKSDOKUMENT_PART_PATH; //NOSONAR TFP-2234
    private static final String REGENERER_PART_PATH = "/regenerer";
    public static final String REGENERER_PATH = BASE_PATH + REGENERER_PART_PATH; //NOSONAR TFP-2234
    private static final String VALIDATE_PART_PATH = "/validate";
    public static final String VALIDATE_PATH = BASE_PATH + VALIDATE_PART_PATH; //NOSONAR TFP-2234
    private static final String VEDTAK_FP_SNAPSHOT_PART_PATH = "/gjeldendevedtak-foreldrepenger";
    public static final String VEDTAK_FP_SNAPSHOT_PATH = BASE_PATH + VEDTAK_FP_SNAPSHOT_PART_PATH; //NOSONAR TFP-2234
    private static final String VEDTAK_SVP_SNAPSHOT_PART_PATH = "/gjeldendevedtak-svangerskapspenger";
    public static final String VEDTAK_SVP_SNAPSHOT_PATH = BASE_PATH + VEDTAK_SVP_SNAPSHOT_PART_PATH; //NOSONAR TFP-2234

    private VedtakInnsynTjeneste vedtakInnsynTjeneste;
    private VedtakTjeneste vedtakTjeneste;
    private FagsakTjeneste fagsakTjeneste;
    private VedtattYtelseTjeneste vedtattYtelseTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private RegenererVedtaksXmlTjeneste regenererVedtaksXmlTjeneste;
    private final Logger log = LoggerFactory.getLogger(VedtakRestTjeneste.class);

    public VedtakRestTjeneste() {
        // for resteasy
    }

    @Inject
    public VedtakRestTjeneste(BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
                              ProsessTaskRepository prosessTaskRepository,
                              VedtakInnsynTjeneste vedtakInnsynTjeneste,
                              VedtakTjeneste vedtakTjeneste,
                              FagsakTjeneste fagsakTjeneste,
                              VedtattYtelseTjeneste vedtattYtelseTjeneste,
                              BehandlingRepository behandlingRepository,
                              RegenererVedtaksXmlTjeneste regenererVedtaksXmlTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.vedtakInnsynTjeneste = vedtakInnsynTjeneste;
        this.vedtakTjeneste = vedtakTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.regenererVedtaksXmlTjeneste = regenererVedtaksXmlTjeneste;
        this.fagsakTjeneste = fagsakTjeneste;
        this.vedtattYtelseTjeneste = vedtattYtelseTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Path(VEDTAK_FP_SNAPSHOT_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter informasjon om Foreldrepenger for en aktør - POC for Sykepenger",
        tags = "vedtak",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returnerer vedtak",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = FeedDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<Ytelse> vedtakForeldrepengerForBruker(
        @QueryParam("aktoerId") @Parameter(description = "aktoerId") @Valid @NotNull AktørParam aktørParam) {
        if (aktørParam.get().isEmpty()) {
            return new ArrayList<>();
        }
        List<Ytelse> ytelser = hentVedtakMedPerioderSiste12M(aktørParam.get().get(), FagsakYtelseType.FORELDREPENGER);
        log.info("vedtakForeldrepengerForBruker antall {}", ytelser.size());
        return ytelser;
    }

    @GET
    @Path(VEDTAK_SVP_SNAPSHOT_PART_PATH)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Operation(description = "Henter informasjon om Svangerskapspenger for en aktør - POC for Sykepenger",
        tags = "vedtak",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returnerer vedtak",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = FeedDto.class)
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<Ytelse> vedtakSvangerskapspengerForBruker(
        @QueryParam("aktoerId") @Parameter(description = "aktoerId") @Valid @NotNull AktørParam aktørParam) {
        if (aktørParam.get().isEmpty()) {
            return new ArrayList<>();
        }
        List<Ytelse> ytelser = hentVedtakMedPerioderSiste12M(aktørParam.get().get(), FagsakYtelseType.SVANGERSKAPSPENGER);
        log.info("vedtakSvangerskapspengerForBruker antall {}", ytelser.size());
        return ytelser;
    }

    private List<Ytelse> hentVedtakMedPerioderSiste12M(AktørId aktørId, FagsakYtelseType ytelseType) {
        return fagsakTjeneste.finnFagsakerForAktør(aktørId).stream()
            .filter(f -> ytelseType.equals(f.getYtelseType()))
            .map(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()))
            .flatMap(Optional::stream)
            .map(b -> (YtelseV1)vedtattYtelseTjeneste.genererYtelse(b))
            .filter(y -> y.getPeriode().getTom().isAfter(LocalDate.now().minusMonths(12)))
            .collect(Collectors.toList());
    }

    @GET
    @Path(HENT_VEDTAKSDOKUMENT_PART_PATH)
    @Operation(description = "Hent vedtaksdokument gitt behandlingId", summary = ("Returnerer vedtaksdokument som er tilknyttet behandlingId."), tags = "vedtak")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentVedtaksdokument(@NotNull @QueryParam("behandlingId") @Parameter(description = "BehandlingId for vedtaksdokument") @Valid BehandlingIdDto behandlingIdDto) {
        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
            : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());

        String resultat = vedtakInnsynTjeneste.hentVedtaksdokument(behandling.getId());
        return Response.ok(resultat, "text/html").build();
    }

    @GET
    @Path(HENT_VEDTAKSDOKUMENT_PART_PATH)
    @Operation(description = "Hent vedtaksdokument gitt behandlingId", summary = ("Returnerer vedtaksdokument som er tilknyttet behandlingId."), tags = "vedtak")
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response hentVedtaksdokument(@NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        return hentVedtaksdokument(new BehandlingIdDto(uuidDto));
    }

    @POST
    @Operation(description = "Generer vedtaksxmler som ikke er gyldige på nytt", tags = "vedtak")
    @Path(REGENERER_PART_PATH)
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    @Transactional
public Response regenererIkkeGyldigeVedtaksXml(@Parameter(description = "Datointervall i vedtak tabell for hvor det skal genereres ny vedtaksxml og maksAntall som behandles")
                                                   @NotNull @Valid GenererVedtaksXmlDto genererVedtaksXmlDto) {

        log.info("Skal sjekke maks {} vedtaksXMLer og regenerere ikke gyldige vedtaksXMLer for perioden [{}] - [{}]", genererVedtaksXmlDto.getMaksAntall(), genererVedtaksXmlDto.getFom(), genererVedtaksXmlDto.getTom());

        List<Long> behandlinger = vedtakTjeneste.hentLagreteVedtakBehandlingId(genererVedtaksXmlDto.fom, genererVedtaksXmlDto.tom);

        log.info("{} vedtak er funnet for perioden [{}] - [{}]", behandlinger.size(), genererVedtaksXmlDto.getFom(), genererVedtaksXmlDto.getTom());

        if (genererVedtaksXmlDto.getMaksAntall() != null && behandlinger.size() > genererVedtaksXmlDto.getMaksAntall().intValue())
            behandlinger = behandlinger.subList(0, genererVedtaksXmlDto.getMaksAntall().intValue());

        log.info("Skal sjekke vedtakXMLen for {} behandlinger og regenerere de som ikke er gyldige ", behandlinger.size());

        for (Long b : behandlinger) {
            Behandling behandling = behandlingsprosessTjeneste.hentBehandling(b);
            ProsessTaskData prosessTaskData = new ProsessTaskData(ValiderOgRegenererVedtaksXmlTask.TASKTYPE);

            prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            prosessTaskData.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(prosessTaskData);
        }

        return Response.ok().build();
    }

    @POST
    @Operation(description = "Validerer vedtaksxml", tags = "vedtak")
    @Path(VALIDATE_PART_PATH)
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    @Transactional
public Response validerVedtaksXml(@Parameter(description = "Datointervall i vedtak tabell for hvilke vedtakxml som skal valideres og maksAntall som behandles")
                                      @NotNull @Valid GenererVedtaksXmlDto genererVedtaksXmlDto) {

        log.info("Skal validere maks {} vedtaksXMLer for perioden [{}] - [{}]", genererVedtaksXmlDto.getMaksAntall(), genererVedtaksXmlDto.getFom(), genererVedtaksXmlDto.getTom());

        List<Long> behandlinger = vedtakTjeneste.hentLagreteVedtakBehandlingId(genererVedtaksXmlDto.fom, genererVedtaksXmlDto.tom);

        log.info("{} vedtak er funnet for perioden [{}] - [{}]", behandlinger.size(), genererVedtaksXmlDto.getFom(), genererVedtaksXmlDto.getTom());

        if (genererVedtaksXmlDto.getMaksAntall() != null && behandlinger.size() > genererVedtaksXmlDto.getMaksAntall().intValue())
            behandlinger = behandlinger.subList(0, genererVedtaksXmlDto.getMaksAntall().intValue());

        log.info("Skal validere vedtakXMLen for {} behandlinger  ", behandlinger.size());

        int antallIkkeGyldig = 0;

        for (Long b : behandlinger) {
            Behandling behandling = behandlingsprosessTjeneste.hentBehandling(b);
            if (!regenererVedtaksXmlTjeneste.valider(behandling)) {
                antallIkkeGyldig++;
            }
        }
        log.info("{} vedtakXML av {} var ugyldige  ", antallIkkeGyldig, behandlinger.size());
        log.info("{} vedtakXML av {} var gyldige  ", behandlinger.size() - antallIkkeGyldig, behandlinger.size());

        return Response.ok().build();
    }

    public static class GenererVedtaksXmlDto implements AbacDto {

        @NotNull
        private LocalDateTime fom;

        @NotNull
        private LocalDateTime tom;

        @NotNull
        @Min(0)
        @Max(5000L)
        private Long maksAntall;

        public GenererVedtaksXmlDto() {
        }

        public GenererVedtaksXmlDto(LocalDateTime fomTidspunkt, LocalDateTime tomTidspunkt, Long maksAntall) {
            this.fom = fomTidspunkt;
            this.tom = tomTidspunkt;
            this.maksAntall = maksAntall;
        }

        public LocalDateTime getFom() {
            return fom;
        }

        public void setFom(LocalDateTime fom) {
            this.fom = fom;
        }

        public LocalDateTime getTom() {
            return tom;
        }

        public void setTom(LocalDateTime tom) {
            this.tom = tom;
        }

        public Long getMaksAntall() {
            return maksAntall;
        }

        public void setMaksAntall(Long maksAntall) {
            this.maksAntall = maksAntall;
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett();
        }
    }

}
