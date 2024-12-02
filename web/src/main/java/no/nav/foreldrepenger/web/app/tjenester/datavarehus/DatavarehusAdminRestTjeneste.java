package no.nav.foreldrepenger.web.app.tjenester.datavarehus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.ekstern.RegenererVedtaksXmlTask;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/datavarehus")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class DatavarehusAdminRestTjeneste {

    private BehandlingsprosessTjeneste behandlingsprosessTjeneste;
    private ProsessTaskTjeneste taskTjeneste;
    private VedtakTjeneste vedtakTjeneste;
    private static final Logger LOG = LoggerFactory.getLogger(DatavarehusAdminRestTjeneste.class);

    public DatavarehusAdminRestTjeneste() {
        // CDI
    }

    @Inject
    public DatavarehusAdminRestTjeneste(BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                        ProsessTaskTjeneste taskTjeneste,
                                        VedtakTjeneste vedtakTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.taskTjeneste = taskTjeneste;
        this.vedtakTjeneste = vedtakTjeneste;
    }

    @POST
    @Operation(description = "Generer vedtaksxml på nytt for gitt behandlingid.", tags = "datavarehus")
    @Path("/regenerer_vedtaksdokument")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    public Response regenererVedtaksXml(@BeanParam @Valid DvhAdminBehandlingIdDto uuidDto) {

        LOG.info("Skal generere vedtakXML for behandlingid {} ", uuidDto);

        var behandling = behandlingsprosessTjeneste.hentBehandling(uuidDto.getBehandlingUuid());

        var lagretVedtak = vedtakTjeneste.hentLagreteVedtak(behandling.getId());

        if (lagretVedtak != null) {
            var prosessTaskData = ProsessTaskData.forProsessTask(RegenererVedtaksXmlTask.class);
            prosessTaskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
            prosessTaskData.setCallIdFraEksisterende();
            taskTjeneste.lagre(prosessTaskData);
        } else {
            LOG.warn("Oppgitt behandling {} er ukjent", uuidDto);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok().build();
    }


}
