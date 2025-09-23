package no.nav.foreldrepenger.web.app.tjenester.vedtak;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import no.nav.foreldrepenger.web.app.tjenester.tilbake.TilbakeRestTjeneste;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.ekstern.RegenererVedtaksXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.ekstern.ValiderOgRegenererVedtaksXmlTask;
import no.nav.foreldrepenger.domene.vedtak.innsyn.VedtakInnsynTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingIdDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(VedtakRestTjeneste.BASE_PATH)
@ApplicationScoped
public class VedtakRestTjeneste {

    static final String BASE_PATH = "/vedtak";
    private static final String HENT_VEDTAKSDOKUMENT_PART_PATH = "/hent-vedtaksdokument";
    public static final String HENT_VEDTAKSDOKUMENT_PATH = BASE_PATH + HENT_VEDTAKSDOKUMENT_PART_PATH;
    private static final String REGENERER_PART_PATH = "/regenerer";
    public static final String REGENERER_PATH = BASE_PATH + REGENERER_PART_PATH;
    private static final String VALIDATE_PART_PATH = "/validate";
    public static final String VALIDATE_PATH = BASE_PATH + VALIDATE_PART_PATH;

    private VedtakInnsynTjeneste vedtakInnsynTjeneste;
    private VedtakTjeneste vedtakTjeneste;
    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private ProsessTaskTjeneste taskTjeneste;
    private RegenererVedtaksXmlTjeneste regenererVedtaksXmlTjeneste;
    private static final Logger LOG = LoggerFactory.getLogger(VedtakRestTjeneste.class);

    public VedtakRestTjeneste() {
        // CDI
    }

    @Inject
    public VedtakRestTjeneste(BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                              ProsessTaskTjeneste taskTjeneste,
                              VedtakInnsynTjeneste vedtakInnsynTjeneste,
                              VedtakTjeneste vedtakTjeneste,
                              RegenererVedtaksXmlTjeneste regenererVedtaksXmlTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.vedtakInnsynTjeneste = vedtakInnsynTjeneste;
        this.vedtakTjeneste = vedtakTjeneste;
        this.taskTjeneste = taskTjeneste;
        this.regenererVedtaksXmlTjeneste = regenererVedtaksXmlTjeneste;
    }


    @GET
    @Path(HENT_VEDTAKSDOKUMENT_PART_PATH)
    @Operation(description = "Hent vedtaksdokument gitt behandlingId", summary = "Returnerer vedtaksdokument som er tilknyttet behandlingId.", tags = "vedtak")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = false)
    public Response hentVedtaksdokument(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.BehandlingIdAbacDataSupplier.class)
            @NotNull @QueryParam("behandlingId") @Parameter(description = "BehandlingId for vedtaksdokument") @Valid BehandlingIdDto behandlingIdDto) {
        var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());
        var resultat = vedtakInnsynTjeneste.hentVedtaksdokument(behandling.getId());
        return Response.ok(resultat, "text/html").build();
    }

    @POST
    @Operation(description = "Generer vedtaksxmler som ikke er gyldige på nytt", tags = "vedtak")
    @Path(REGENERER_PART_PATH)
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    @Transactional
    public Response regenererIkkeGyldigeVedtaksXml(
            @Parameter(description = "Datointervall i vedtak tabell for hvor det skal genereres ny vedtaksxml og maksAntall som behandles") @NotNull @Valid GenererVedtaksXmlDto genererVedtaksXmlDto) {

        LOG.info("Skal sjekke maks {} vedtaksXMLer og regenerere ikke gyldige vedtaksXMLer for perioden [{}] - [{}]",
                genererVedtaksXmlDto.getMaksAntall(), genererVedtaksXmlDto.getFom(), genererVedtaksXmlDto.getTom());

        var behandlinger = vedtakTjeneste.hentLagreteVedtakBehandlingId(genererVedtaksXmlDto.fom, genererVedtaksXmlDto.tom);

        LOG.info("{} vedtak er funnet for perioden [{}] - [{}]", behandlinger.size(), genererVedtaksXmlDto.getFom(), genererVedtaksXmlDto.getTom());

        if (genererVedtaksXmlDto.getMaksAntall() != null && behandlinger.size() > genererVedtaksXmlDto.getMaksAntall().intValue())
            behandlinger = behandlinger.subList(0, genererVedtaksXmlDto.getMaksAntall().intValue());

        LOG.info("Skal sjekke vedtakXMLen for {} behandlinger og regenerere de som ikke er gyldige ", behandlinger.size());

        for (var b : behandlinger) {
            var behandling = behandlingsprosessTjeneste.hentBehandling(b);
            var prosessTaskData = ProsessTaskData.forProsessTask(ValiderOgRegenererVedtaksXmlTask.class);

            prosessTaskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
            taskTjeneste.lagre(prosessTaskData);
        }

        return Response.ok().build();
    }

    @POST
    @Operation(description = "Validerer vedtaksxml", tags = "vedtak")
    @Path(VALIDATE_PART_PATH)
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    @Transactional
    public Response validerVedtaksXml(
            @Parameter(description = "Datointervall i vedtak tabell for hvilke vedtakxml som skal valideres og maksAntall som behandles") @NotNull @Valid GenererVedtaksXmlDto genererVedtaksXmlDto) {

        LOG.info("Skal validere maks {} vedtaksXMLer for perioden [{}] - [{}]", genererVedtaksXmlDto.getMaksAntall(), genererVedtaksXmlDto.getFom(),
                genererVedtaksXmlDto.getTom());

        var behandlinger = vedtakTjeneste.hentLagreteVedtakBehandlingId(genererVedtaksXmlDto.fom, genererVedtaksXmlDto.tom);

        LOG.info("{} vedtak er funnet for perioden [{}] - [{}]", behandlinger.size(), genererVedtaksXmlDto.getFom(), genererVedtaksXmlDto.getTom());

        if (genererVedtaksXmlDto.getMaksAntall() != null && behandlinger.size() > genererVedtaksXmlDto.getMaksAntall().intValue())
            behandlinger = behandlinger.subList(0, genererVedtaksXmlDto.getMaksAntall().intValue());

        LOG.info("Skal validere vedtakXMLen for {} behandlinger  ", behandlinger.size());

        var antallIkkeGyldig = 0;

        for (var b : behandlinger) {
            var behandling = behandlingsprosessTjeneste.hentBehandling(b);
            if (!regenererVedtaksXmlTjeneste.valider(behandling)) {
                antallIkkeGyldig++;
            }
        }
        LOG.info("{} vedtakXML av {} var ugyldige  ", antallIkkeGyldig, behandlinger.size());
        LOG.info("{} vedtakXML av {} var gyldige  ", behandlinger.size() - antallIkkeGyldig, behandlinger.size());

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
            return TilbakeRestTjeneste.opprett();
        }
    }

}
