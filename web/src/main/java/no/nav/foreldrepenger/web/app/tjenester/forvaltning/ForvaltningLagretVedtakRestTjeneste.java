package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.ekstern.RegenererVedtaksXmlTask;
import no.nav.foreldrepenger.domene.vedtak.ekstern.RegenererVedtaksXmlTjeneste;
import no.nav.foreldrepenger.domene.vedtak.ekstern.ValiderOgRegenererVedtaksXmlTask;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/datavarehus")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class ForvaltningLagretVedtakRestTjeneste {

    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private ProsessTaskTjeneste taskTjeneste;
    private VedtakTjeneste vedtakTjeneste;
    private RegenererVedtaksXmlTjeneste regenererVedtaksXmlTjeneste;
    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningLagretVedtakRestTjeneste.class);

    public ForvaltningLagretVedtakRestTjeneste() {
        // CDI
    }

    @Inject
    public ForvaltningLagretVedtakRestTjeneste(BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                               ProsessTaskTjeneste taskTjeneste,
                                               VedtakTjeneste vedtakTjeneste,
                                               RegenererVedtaksXmlTjeneste regenererVedtaksXmlTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.taskTjeneste = taskTjeneste;
        this.vedtakTjeneste = vedtakTjeneste;
        this.regenererVedtaksXmlTjeneste = regenererVedtaksXmlTjeneste;
    }

    @POST
    @Operation(description = "Generer vedtaksxml på nytt for gitt behandlingid.", tags = "datavarehus")
    @Path("/regenerer_vedtaksdokument")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response regenererVedtaksXml(@BeanParam @Valid ForvaltningBehandlingIdDto uuidDto) {

        LOG.info("Skal generere vedtakXML for behandlingid {} ", uuidDto);

        var behandling = behandlingsprosessTjeneste.hentBehandling(uuidDto.getBehandlingUuid());

        var lagretVedtak = vedtakTjeneste.hentLagreteVedtak(behandling.getId());

        if (lagretVedtak != null) {
            var prosessTaskData = ProsessTaskData.forProsessTask(RegenererVedtaksXmlTask.class);
            prosessTaskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
            taskTjeneste.lagre(prosessTaskData);
        } else {
            LOG.warn("Oppgitt behandling {} er ukjent", uuidDto);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok().build();
    }

    @POST
    @Operation(description = "Generer vedtaksxmler som ikke er gyldige på nytt", tags = "vedtak")
    @Path("/regenerer")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    @Transactional
    public Response regenererIkkeGyldigeVedtaksXml(
        @Parameter(description = "Datointervall i vedtak tabell for hvor det skal genereres ny vedtaksxml og maksAntall som behandles") @NotNull @Valid ForvaltningGenererVedtaksXmlDto genererVedtaksXmlDto) {

        LOG.info("Skal sjekke maks {} vedtaksXMLer og regenerere ikke gyldige vedtaksXMLer for perioden [{}] - [{}]",
            genererVedtaksXmlDto.maksAntall(), genererVedtaksXmlDto.fom(), genererVedtaksXmlDto.tom());

        var behandlinger = vedtakTjeneste.hentLagreteVedtakBehandlingId(genererVedtaksXmlDto.fom(), genererVedtaksXmlDto.tom());

        LOG.info("{} vedtak er funnet for perioden [{}] - [{}]", behandlinger.size(), genererVedtaksXmlDto.fom(), genererVedtaksXmlDto.tom());

        if (genererVedtaksXmlDto.maksAntall() != null && behandlinger.size() > genererVedtaksXmlDto.maksAntall().intValue())
            behandlinger = behandlinger.subList(0, genererVedtaksXmlDto.maksAntall().intValue());

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
    @Path("/validate")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = false)
    @Transactional
    public Response validerVedtaksXml(
        @Parameter(description = "Datointervall i vedtak tabell for hvilke vedtakxml som skal valideres og maksAntall som behandles") @NotNull @Valid ForvaltningGenererVedtaksXmlDto genererVedtaksXmlDto) {

        LOG.info("Skal validere maks {} vedtaksXMLer for perioden [{}] - [{}]", genererVedtaksXmlDto.maksAntall(), genererVedtaksXmlDto.fom(),
            genererVedtaksXmlDto.tom());

        var behandlinger = vedtakTjeneste.hentLagreteVedtakBehandlingId(genererVedtaksXmlDto.fom(), genererVedtaksXmlDto.tom());

        LOG.info("{} vedtak er funnet for perioden [{}] - [{}]", behandlinger.size(), genererVedtaksXmlDto.fom(), genererVedtaksXmlDto.tom());

        if (genererVedtaksXmlDto.maksAntall() != null && behandlinger.size() > genererVedtaksXmlDto.maksAntall().intValue())
            behandlinger = behandlinger.subList(0, genererVedtaksXmlDto.maksAntall().intValue());

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

    public static record ForvaltningGenererVedtaksXmlDto(@NotNull LocalDateTime fom,
                                                         @NotNull LocalDateTime tom,
                                                         @NotNull @Min(0) @Max(5000L) Long maksAntall) implements AbacDto {


        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett();
        }
    }

}
