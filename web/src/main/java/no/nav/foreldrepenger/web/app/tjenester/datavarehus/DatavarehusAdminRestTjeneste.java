package no.nav.foreldrepenger.web.app.tjenester.datavarehus;

import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.task.RegenererVedtaksXmlDatavarehusTask;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.ekstern.RegenererVedtaksXmlTask;
import no.nav.foreldrepenger.domene.vedtak.repo.LagretVedtakRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
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
    private LagretVedtakRepository lagretVedtakRepository;
    private DatavarehusTjeneste datavarehusTjeneste;
    private VedtakTjeneste vedtakTjeneste;
    private static final Logger LOG = LoggerFactory.getLogger(DatavarehusAdminRestTjeneste.class);

    public DatavarehusAdminRestTjeneste() {
        // CDI
    }

    @Inject
    public DatavarehusAdminRestTjeneste(BehandlingsprosessTjeneste behandlingsprosessTjeneste,
                                        ProsessTaskTjeneste taskTjeneste,
                                        LagretVedtakRepository lagretVedtakRepository,
                                        DatavarehusTjeneste datavarehusTjeneste,
                                        VedtakTjeneste vedtakTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.taskTjeneste = taskTjeneste;
        this.lagretVedtakRepository = lagretVedtakRepository;
        this.datavarehusTjeneste = datavarehusTjeneste;
        this.vedtakTjeneste = vedtakTjeneste;
    }

    @Path("/vedtaksxml/regenerer")
    @POST
    @Operation(description = "Generer opp vedtaks xml til datavarehus på nytt for behandling(er).", tags = "datavarehus")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    public Response genererVedtaksXmlTilDvh(
            @Parameter(description = "Datointervall i dvh vedtak tabell hvor det skal genereres ny vedtaks xml. FagsakYtelseType kan settes for å angi for hvilken ytelese det skal genereres ny vedtaks xml, dersom .") @NotNull @Valid GenererVedtaksXmlDvhDto genererVedtaksXmlDvhDto) {

        LOG.info("Forsøker å regenerere vedtaks XML  for datavarehus i intervall [{}] - [{}]", genererVedtaksXmlDvhDto.getFom(),
                genererVedtaksXmlDvhDto.getTom());

        var behandlinger = datavarehusTjeneste.hentVedtakBehandlinger(genererVedtaksXmlDvhDto.getFom(), genererVedtaksXmlDvhDto.getTom());
        var antBehandlinger = 0;
        for (var behandlingId : behandlinger) {
            var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
            if (genererVedtaksXmlDvhDto.getFagsakYtelseType() == null
                    || behandling.getFagsakYtelseType().getKode().equals(genererVedtaksXmlDvhDto.getFagsakYtelseType())) {
                var prosessTaskData = ProsessTaskData.forProsessTask(RegenererVedtaksXmlDatavarehusTask.class);

                prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
                prosessTaskData.setCallIdFraEksisterende();
                taskTjeneste.lagre(prosessTaskData);
                antBehandlinger++;
            }
        }
        LOG.info("Har opprettet nye prosesstask for å regenrere vedtaksxml for {} behandlinger", antBehandlinger);

        return Response.ok().build();
    }

    @Path("/vedtaksxml/generer")
    @POST
    @Operation(description = "Generer opp vedtaks xml til datavarehus for behandling(er) uten at det trenger å finnes DVH vedtaks-xml fra før. Finner behandlinger via LAGRET_VEDTAK.", tags = "datavarehus")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    public Response genererVedtaksXmlTilDvhSomKanMangleDvhVedtakXml(
            @Parameter(description = "Datointervall i dvh vedtak tabell hvor det skal genereres ny vedtaks xml. FagsakYtelseType settes for å angi for hvilken ytelese det skal genereres ny vedtaks xml.") @NotNull @Valid GenererVedtaksXmlDvhDto genererVedtaksXmlDvhDto) {

        LOG.info("Forsøker å generere vedtaks XML for datavarehus i intervall [{}] - [{}] med fagsakYtelseType {}", genererVedtaksXmlDvhDto.getFom(),
                genererVedtaksXmlDvhDto.getTom(), genererVedtaksXmlDvhDto.getFagsakYtelseType());

        var behandlinger = lagretVedtakRepository.hentLagretVedtakBehandlingId(genererVedtaksXmlDvhDto.getFom(),
                genererVedtaksXmlDvhDto.getTom(), FagsakYtelseType.fraKode(genererVedtaksXmlDvhDto.getFagsakYtelseType()));

        for (var behandlingId : behandlinger) {
            var behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
            var prosessTaskData = ProsessTaskData.forProsessTask(RegenererVedtaksXmlDatavarehusTask.class);

            prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            prosessTaskData.setCallIdFraEksisterende();
            taskTjeneste.lagre(prosessTaskData);
        }
        LOG.info("Har opprettet nye prosesstask for å genrere vedtaksxml for {} behandlinger", behandlinger.size());

        return Response.ok().build();
    }

    @POST
    @Operation(description = "Generer vedtaksxml på nytt for gitt behandlingid.", tags = "datavarehus")
    @Path("/regenerer_vedtaksdokument")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    public Response regenererVedtaksXml(@TilpassetAbacAttributt(supplierClass = UuidAbacDataSupplier.class)
            @Parameter(description = "Behandlingid") @QueryParam("BehandlingId") @NotNull @Valid UuidDto uuidDto) {

        LOG.info("Skal generere vedtakXML for behandlingid {} ", uuidDto);

        var behandling = behandlingsprosessTjeneste.hentBehandling(uuidDto.getBehandlingUuid());

        var lagretVedtak = vedtakTjeneste.hentLagreteVedtak(behandling.getId());

        if (lagretVedtak != null) {
            var prosessTaskData = ProsessTaskData.forProsessTask(RegenererVedtaksXmlTask.class);
            prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            prosessTaskData.setCallIdFraEksisterende();
            taskTjeneste.lagre(prosessTaskData);
        } else {
            LOG.warn("Oppgitt behandling {} er ukjent", uuidDto); // NOSONAR
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok().build();
    }

    @Path("/regenerer_vedtaksdokument_dvh")
    @POST
    @Operation(description = "Generer opp vedtaks xml til datavarehus på nytt for gitt behandlingid", tags = "datavarehus")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT)
    public Response regenererVedtaksXmlDvh(@TilpassetAbacAttributt(supplierClass = UuidAbacDataSupplier.class)
            @Parameter(description = "Behandlingid") @QueryParam("BehandlingId") @NotNull @Valid UuidDto uuidDto) {

        LOG.info("Skal generere vedtakXML_DVH for behandlingid {} ", uuidDto);

        var behandling = behandlingsprosessTjeneste.hentBehandling(uuidDto.getBehandlingUuid());

        var behandlinger = datavarehusTjeneste.hentVedtakBehandlinger(behandling.getId());

        behandlinger.forEach(b -> {
            var prosessTaskData = ProsessTaskData.forProsessTask(RegenererVedtaksXmlDatavarehusTask.class);

            prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            prosessTaskData.setCallIdFraEksisterende();
            taskTjeneste.lagre(prosessTaskData);
            LOG.info("Har opprettet  prosesstask for å regenrere dvh vedtaksxml for {} behandling", uuidDto);
        });

        return Response.ok().build();
    }

    public static class UuidAbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }

}
