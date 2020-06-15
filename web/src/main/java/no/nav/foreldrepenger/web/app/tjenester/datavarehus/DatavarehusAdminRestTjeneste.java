package no.nav.foreldrepenger.web.app.tjenester.datavarehus;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.DRIFT;

import java.util.List;

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
import no.nav.foreldrepenger.behandling.BehandlingIdDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtak;
import no.nav.foreldrepenger.datavarehus.task.RegenererVedtaksXmlDatavarehusTask;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.ekstern.RegenererVedtaksXmlTask;
import no.nav.foreldrepenger.domene.vedtak.repo.LagretVedtakRepository;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.BehandlingsprosessApplikasjonTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/datavarehus")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class DatavarehusAdminRestTjeneste {

    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private LagretVedtakRepository lagretVedtakRepository;
    private DatavarehusTjeneste datavarehusTjeneste;
    private VedtakTjeneste vedtakTjeneste;
    private final Logger log = LoggerFactory.getLogger(DatavarehusAdminRestTjeneste.class);

    public DatavarehusAdminRestTjeneste() {
        //CDI
    }

    @Inject
    public DatavarehusAdminRestTjeneste(BehandlingsprosessApplikasjonTjeneste behandlingsprosessTjeneste,
                                        ProsessTaskRepository prosessTaskRepository,
                                        LagretVedtakRepository lagretVedtakRepository,
                                        DatavarehusTjeneste datavarehusTjeneste,
                                        VedtakTjeneste vedtakTjeneste) {
        this.behandlingsprosessTjeneste = behandlingsprosessTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.lagretVedtakRepository = lagretVedtakRepository;
        this.datavarehusTjeneste = datavarehusTjeneste;
        this.vedtakTjeneste = vedtakTjeneste;
    }

    @Path("/vedtaksxml/regenerer")
    @POST
    @Operation(description = "Generer opp vedtaks xml til datavarehus på nytt for behandling(er).", tags = "datavarehus")
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    public Response genererVedtaksXmlTilDvh(@Parameter(description = "Datointervall i dvh vedtak tabell hvor det skal genereres ny vedtaks xml. FagsakYtelseType kan settes for å angi for hvilken ytelese det skal genereres ny vedtaks xml, dersom .")
                                            @NotNull @Valid GenererVedtaksXmlDvhDto genererVedtaksXmlDvhDto) {

        log.info("Forsøker å regenerere vedtaks XML  for datavarehus i intervall [{}] - [{}]", genererVedtaksXmlDvhDto.getFom(), genererVedtaksXmlDvhDto.getTom());

        List<Long> behandlinger = datavarehusTjeneste.hentVedtakBehandlinger(genererVedtaksXmlDvhDto.getFom(), genererVedtaksXmlDvhDto.getTom());
        int antBehandlinger = 0;
        for (var behandlingId : behandlinger) {
            Behandling behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
            if (genererVedtaksXmlDvhDto.getFagsakYtelseType() == null || behandling.getFagsakYtelseType().getKode().equals(genererVedtaksXmlDvhDto.getFagsakYtelseType())) {
                ProsessTaskData prosessTaskData = new ProsessTaskData(RegenererVedtaksXmlDatavarehusTask.TASKTYPE);

                prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
                prosessTaskData.setCallIdFraEksisterende();
                prosessTaskRepository.lagre(prosessTaskData);
                antBehandlinger++;
            }
        }
        log.info("Har opprettet nye prosesstask for å regenrere vedtaksxml for {} behandlinger", antBehandlinger);

        return Response.ok().build();
    }


    @Path("/vedtaksxml/generer")
    @POST
    @Operation(description = "Generer opp vedtaks xml til datavarehus for behandling(er) uten at det trenger å finnes DVH vedtaks-xml fra før. Finner behandlinger via LAGRET_VEDTAK.", tags = "datavarehus")
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    public Response genererVedtaksXmlTilDvhSomKanMangleDvhVedtakXml(@Parameter(description = "Datointervall i dvh vedtak tabell hvor det skal genereres ny vedtaks xml. FagsakYtelseType settes for å angi for hvilken ytelese det skal genereres ny vedtaks xml.")
                                                                    @NotNull @Valid GenererVedtaksXmlDvhDto genererVedtaksXmlDvhDto) {

        log.info("Forsøker å generere vedtaks XML for datavarehus i intervall [{}] - [{}] med fagsakYtelseType {}", genererVedtaksXmlDvhDto.getFom(), genererVedtaksXmlDvhDto.getTom(), genererVedtaksXmlDvhDto.getFagsakYtelseType());

        List<Long> behandlinger = lagretVedtakRepository.hentLagretVedtakBehandlingId(genererVedtaksXmlDvhDto.getFom(), genererVedtaksXmlDvhDto.getTom(), FagsakYtelseType.fraKode(genererVedtaksXmlDvhDto.getFagsakYtelseType()));

        for (var behandlingId : behandlinger) {
            Behandling behandling = behandlingsprosessTjeneste.hentBehandling(behandlingId);
            ProsessTaskData prosessTaskData = new ProsessTaskData(RegenererVedtaksXmlDatavarehusTask.TASKTYPE);

            prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            prosessTaskData.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(prosessTaskData);
        }
        log.info("Har opprettet nye prosesstask for å genrere vedtaksxml for {} behandlinger", behandlinger.size());

        return Response.ok().build();
    }


    @POST
    @Operation(description = "Generer vedtaksxml på nytt for gitt behandlingid.", tags = "datavarehus")
    @Path("/regenerer_vedtaksdokument")
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    public Response regenererVedtaksXml(@Parameter(description = "Behandlingid")
                                        @QueryParam("BehandlingId") @NotNull @Valid BehandlingIdDto behandlingIdDto) {

        log.info("Skal generere vedtakXML for behandlingid {} ", behandlingIdDto);

        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
            : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());

        LagretVedtak lagretVedtak = vedtakTjeneste.hentLagreteVedtak(behandling.getId());

        if (lagretVedtak != null) {
            ProsessTaskData prosessTaskData = new ProsessTaskData(RegenererVedtaksXmlTask.TASKTYPE);
            prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            prosessTaskData.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(prosessTaskData);
        } else {
            log.warn("Oppgitt behandling {} er ukjent", behandlingIdDto); //NOSONAR
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok().build();
    }

    @Path("/regenerer_vedtaksdokument_dvh")
    @POST
    @Operation(description = "Generer opp vedtaks xml til datavarehus på nytt for gitt behandlingid", tags = "datavarehus")
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT)
    public Response regenererVedtaksXmlDvh(@Parameter(description = "Behandlingid")
                                           @QueryParam("BehandlingId") @NotNull @Valid BehandlingIdDto behandlingIdDto) {

        log.info("Skal generere vedtakXML_DVH for behandlingid {} ", behandlingIdDto);

        Long behandlingId = behandlingIdDto.getBehandlingId();
        Behandling behandling = behandlingId != null
            ? behandlingsprosessTjeneste.hentBehandling(behandlingId)
            : behandlingsprosessTjeneste.hentBehandling(behandlingIdDto.getBehandlingUuid());

        List<Long> behandlinger = datavarehusTjeneste.hentVedtakBehandlinger(behandling.getId());

        behandlinger.forEach(b -> {
            ProsessTaskData prosessTaskData = new ProsessTaskData(RegenererVedtaksXmlDatavarehusTask.TASKTYPE);

            prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            prosessTaskData.setCallIdFraEksisterende();
            prosessTaskRepository.lagre(prosessTaskData);
            log.info("Har opprettet  prosesstask for å regenrere dvh vedtaksxml for {} behandling", behandlingIdDto);
        });

        return Response.ok().build();
    }

}
