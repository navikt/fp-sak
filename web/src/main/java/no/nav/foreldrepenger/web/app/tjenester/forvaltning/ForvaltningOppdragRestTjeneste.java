package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.DRIFT;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag.KvitteringDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag.OppdragPatchDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag.OppdragslinjePatchDto;
import no.nav.foreldrepenger.økonomi.økonomistøtte.BehandleØkonomioppdragKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomiKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumerMedCache;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningOppdrag")
@ApplicationScoped
@Transactional
public class ForvaltningOppdragRestTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForvaltningOppdragRestTjeneste.class);

    private BehandleØkonomioppdragKvittering økonomioppdragKvitteringTjeneste;
    private ØkonomioppdragRepository økonomioppdragRepository;
    private BehandlingRepository behandlingRepository;
    private AktørConsumerMedCache aktørConsumer;
    private ProsessTaskRepository prosessTaskRepository;

    public ForvaltningOppdragRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningOppdragRestTjeneste(BehandleØkonomioppdragKvittering økonomioppdragKvitteringTjeneste, ØkonomioppdragRepository økonomioppdragRepository, BehandlingRepository behandlingRepository, AktørConsumerMedCache aktørConsumer, ProsessTaskRepository prosessTaskRepository) {
        this.økonomioppdragKvitteringTjeneste = økonomioppdragKvitteringTjeneste;
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.behandlingRepository = behandlingRepository;
        this.aktørConsumer = aktørConsumer;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @POST
    @Path("/kvitter-oppdrag-ok")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Kvitterer oppdrag manuelt. Brukes kun når det er avklart at oppdrag har gått OK, og kvittering ikke kommer til å komme fra Oppdragsystemet. Sjekk med Team Ukelønn hvis i tvil",
        tags = "FORVALTNING-oppdrag")
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT, sporingslogg = false)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response kvitterOK(@Parameter(description = "Identifikasjon av oppdrag som kvitteres OK. Sett oppdaterProsessTask til false kun når prosesstasken allerede er flyttet til FERDIG") @NotNull @Valid KvitteringDto kvitteringDto) {
        boolean oppdaterProsessTask = kvitteringDto.getOppdaterProsessTask();

        ØkonomiKvittering kvittering = new ØkonomiKvittering();
        kvittering.setBehandlingId(kvitteringDto.getBehandlingId());
        kvittering.setFagsystemId(kvitteringDto.getFagsystemId());
        kvittering.setAlvorlighetsgrad("00"); //kode som indikerer at alt er OK

        logger.info("Kvitterer oppdrag OK for behandlingId={] fagsystemId={} oppdaterProsessTask=", kvitteringDto.getBehandlingId(), kvitteringDto.getFagsystemId(), oppdaterProsessTask);
        økonomioppdragKvitteringTjeneste.behandleKvittering(kvittering, oppdaterProsessTask);

        return Response.ok().build();
    }

    @POST
    @Path("/patch-oppdrag")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Patcher oppdrag som har feilet fordi fpsak har generert det på feil måte, før ny oversendelse til oppdragsysstemet. Sjekk med Team Ukelønn hvis i tvil. Viktig at det sjekkes i Oppdragsystemet etter oversending at alt har gått som forventet",
        tags = "FORVALTNING-oppdrag")
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT, sporingslogg = false)
    public Response patchOppdrag(@NotNull @Valid OppdragPatchDto dto) {
        Long behandlingId = dto.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Oppdragskontroll oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandlingId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppdragskontroll for behandlingId=" + behandlingId));

        validerProsesstask(oppdragskontroll);
        validerFagsystemId(behandling, dto.getFagsystemId());
        validerDelytelseId(dto);

        String fnrBruker = aktørConsumer.hentPersonIdentForAktørId(behandling.getAktørId().getId())
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke FNR for aktør på behandlingId=" + behandlingId));

        OppdragMapper mapper = new OppdragMapper(dto, behandling, fnrBruker);
        mapper.mapTil(oppdragskontroll);
        økonomioppdragRepository.lagre(oppdragskontroll);

        logger.warn("Patchet oppdrag for behandling={}. Ta kontakt med Team Ukelønn for å avsjekke resultatet når prosesstaskId={} kjøres på nytt. Husk å rekjøre prosesstask når ferdig med å patche/kvittere oppdrag", behandlingId, oppdragskontroll.getProsessTaskId());
        return Response.ok("Patchet oppdrag for behandling=" + behandlingId).build();
    }

    private void validerProsesstask(Oppdragskontroll oppdragskontroll) {
        ProsessTaskData task = prosessTaskRepository.finn(oppdragskontroll.getProsessTaskId());
        ProsessTaskStatus status = task.getStatus();
        if (status != ProsessTaskStatus.VENTER_SVAR && status != ProsessTaskStatus.FEILET) {
            throw new IllegalArgumentException("Kan ikke patche oppdrag som er ferdig. Kan kun brukes når prosesstask er FEILET eller VENTER_SVAR");
        }
        if (status == ProsessTaskStatus.VENTER_SVAR && ChronoUnit.MINUTES.between(task.getSistKjørt(), LocalDate.now()) > 120) {
            throw new IllegalArgumentException("Skal ikke patche oppdrag uten at OS har fått rimelig tid til å svare (sanity check). Prøv igjen senere.");
        }
    }

    private void validerFagsystemId(Behandling behandling, long fagsystemId) {
        if (!Long.toString(fagsystemId / 100).equals(behandling.getFagsak().getSaksnummer().getVerdi())) {
            throw new IllegalArgumentException("FagsystemId=" + fagsystemId + " passer ikke med saksnummer for behandlingId=" + behandling.getId());
        }
    }

    private void validerDelytelseId(OppdragPatchDto dto) {
        for (OppdragslinjePatchDto linje : dto.getOppdragslinjer()) {
            if (dto.getFagsystemId() != linje.getDelytelseId() / 100) {
                throw new IllegalArgumentException("FagsystemId=" + dto.getFagsystemId() + " matcher ikke med delytelseId=" + linje.getDelytelseId());
            }
            if (linje.getRefFagsystemId() != null && dto.getFagsystemId() != linje.getRefFagsystemId()) {
                throw new IllegalArgumentException("FagsystemId=" + dto.getFagsystemId() + " matcher ikke med refFagsystemId=" + linje.getRefFagsystemId());
            }
            if (linje.getRefDelytelseId() != null && dto.getFagsystemId() != linje.getRefDelytelseId() / 100) {
                throw new IllegalArgumentException("FagsystemId=" + dto.getFagsystemId() + " matcher ikke med refDelytelseId=" + linje.getRefDelytelseId());
            }
        }
    }

}
