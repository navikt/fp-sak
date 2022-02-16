package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.Alvorlighetsgrad;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.vedtak.task.SendØkonomiOppdragTask;
import no.nav.foreldrepenger.domene.vedtak.task.VurderOgSendØkonomiOppdragTask;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.oppdrag.OppdragPatchDto;
import no.nav.foreldrepenger.økonomistøtte.BehandleØkonomioppdragKvittering;
import no.nav.foreldrepenger.økonomistøtte.ØkonomiKvittering;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

@Dependent
@Transactional
class ForvaltningOppdragTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningOppdragTjeneste.class);

    private BehandleØkonomioppdragKvittering økonomioppdragKvitteringTjeneste;
    private ØkonomioppdragRepository økonomioppdragRepository;
    private BehandlingRepository behandlingRepository;
    private PersoninfoAdapter personinfoAdapter;
    private ProsessTaskTjeneste taskTjeneste;
    private EntityManager entityManager;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    @Inject
    public ForvaltningOppdragTjeneste(final BehandleØkonomioppdragKvittering økonomioppdragKvitteringTjeneste,
                                      ØkonomioppdragRepository økonomioppdragRepository,
                                      BehandlingRepository behandlingRepository,
                                      PersoninfoAdapter personinfoAdapter,
                                      ProsessTaskTjeneste taskTjeneste,
                                      EntityManager entityManager,
                                      BehandlingVedtakRepository behandlingVedtakRepository) {
        this.økonomioppdragKvitteringTjeneste = økonomioppdragKvitteringTjeneste;
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.behandlingRepository = behandlingRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.taskTjeneste = taskTjeneste;
        this.entityManager = entityManager;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
    }

    public void kvitterOk(long behandlingId, long fagsystemId, boolean oppdaterProsesstask) {
        var kvittering = new ØkonomiKvittering();
        kvittering.setBehandlingId(behandlingId);
        kvittering.setFagsystemId(fagsystemId);
        kvittering.setAlvorlighetsgrad(Alvorlighetsgrad.OK);

        LOG.info("Kvitterer oppdrag OK for behandlingId={} fagsystemId={} oppdaterProsessTask={}", behandlingId,
            fagsystemId, oppdaterProsesstask);
        økonomioppdragKvitteringTjeneste.behandleKvittering(kvittering, oppdaterProsesstask);
    }

    public void patchOppdrag(OppdragPatchDto dto) {
        var behandlingId = dto.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandlingId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppdragskontroll for behandlingId=" + behandlingId));
        var vurderØkonomiTask = taskTjeneste.finn(oppdragskontroll.getProsessTaskId());

        validerUferdigProsesstask(vurderØkonomiTask);

        utførPatching(dto, behandlingId, behandling, oppdragskontroll);
        lagSendØkonomioppdragTask(vurderØkonomiTask, false);

        LOG.warn(
            "Patchet oppdrag for behandling={} fagsystemId={}. Ta kontakt med Team Foreldrepenger for å avsjekke resultatet når prosesstask er kjørt.",
            behandlingId, dto.getFagsystemId());
    }

    public void patchOppdragOgRekjør(OppdragPatchDto dto) {
        var behandlingId = dto.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandlingId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppdragskontroll for behandlingId=" + behandlingId));
        var vurderØkonomiTask = taskTjeneste.finn(oppdragskontroll.getProsessTaskId());

        validerFerdigProsesstask(vurderØkonomiTask);

        utførPatching(dto, behandlingId, behandling, oppdragskontroll);
        byttStatusTilVenterPåKvittering(vurderØkonomiTask);
        lagSendØkonomioppdragTask(vurderØkonomiTask, true);

        LOG.warn("Patchet oppdrag for behandling={} og kjører prosesstask for å sende. Ta kontakt med Team Foreldrepenger for å avsjekke resultatet.",
            behandlingId);
    }

    public void patchk27(long behandlingId, long fagsystemId, LocalDate maksDato) {
        var oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandlingId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke oppdragskontroll for behandlingId=" + behandlingId));

        // Finn oppdrag som skal patches
        var oppdrag110TilPatching = oppdragskontroll.getOppdrag110Liste().stream()
            .filter(oppdrag110 -> oppdrag110.getFagsystemId() == fagsystemId)
            .collect(Collectors.toList());

        if (oppdrag110TilPatching.size() != 1) {
            LOG.warn("Mer enn oppdrag110 funnet for behandlingId {} og fagsystemId {}. Avbryttet patching.", behandlingId, fagsystemId);
            return;
        }

        // Finn siste oppdrag som ble sendt
        var oppdragSomPatches = oppdragskontroll;
        var alleOppdragForSak = økonomioppdragRepository.finnAlleOppdragForSak(oppdragskontroll.getSaksnummer());
        var sisteOppdrag = alleOppdragForSak.stream().max(Comparator.comparing(Oppdragskontroll::getOpprettetTidspunkt)).orElseThrow();

        if (sisteOppdrag != oppdragskontroll) {
            LOG.info("Oppdaterer oppdraget siden ikke det siste. Oppdrag med feil: {}, oppdrag som patches: {}", oppdragskontroll.getId(), sisteOppdrag.getId());
            oppdragSomPatches = sisteOppdrag;

            // Må lagre den som ble patched til å kunne fjerne den fra oversikten.
            oppdragskontroll.setPatched(true);
            økonomioppdragRepository.lagre(oppdragskontroll);
        }

        // lag en kopi av dette hva skal patches uten kvittering med fikset tom dato
        K27OppdragMapper.mapTil(oppdragSomPatches, oppdrag110TilPatching.get(0), maksDato);

        // set venter kvittering true på oppdragskontroll
        oppdragSomPatches.setVenterKvittering(true);
        oppdragSomPatches.setPatched(true);
        økonomioppdragRepository.lagre(oppdragSomPatches);

        // opprett en sendt økonomi oppdrag task
        var behandlingPatchet = behandlingRepository.hentBehandling(oppdragSomPatches.getBehandlingId());

        LOG.info(
            "Patchet arbeidsgiver oppdrag for behandling={} fagsystemId={}. Ta kontakt med Team Foreldrepenger for å avsjekke resultatet når prosesstask er kjørt.",
            behandlingPatchet.getId(), fagsystemId);

        lagSendØkonomioppdragTask(behandlingPatchet);
    }

    private void utførPatching(OppdragPatchDto dto, Long behandlingId, Behandling behandling, Oppdragskontroll oppdragskontroll) {
        validerHyppighet();
        validerFagsystemId(behandling, dto.getFagsystemId());
        validerDelytelseId(dto);

        var fnrBruker = personinfoAdapter.hentFnr(behandling.getAktørId())
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke FNR for aktør på behandlingId=" + behandlingId));

        kvitterBortEksisterendeOppdrag(dto, oppdragskontroll);

        var behandlingVedtak = behandlingVedtakRepository.hentForBehandling(behandling.getId());
        var mapper = new OppdragMapper(dto, behandling, fnrBruker.getIdent(), behandlingVedtak);
        mapper.mapTil(oppdragskontroll);
        oppdragskontroll.setVenterKvittering(true);
        økonomioppdragRepository.lagre(oppdragskontroll);
    }

    private void validerHyppighet() {
        // denne tjenesten skal kun brukes i antatt svært sjeldne tilfeller
        // begrenser derfor hvor ofte den kan brukes for å hindre feil bruk
        var antallPatchedeINærFortid = finnAntallPatchedeSistePeriode(entityManager, Period.ofWeeks(4));
        if (antallPatchedeINærFortid > 10) {
            throw new ForvaltningException(
                "Ikke klar for patching enda. Vurder å øke tillatt hyppighet i ForvaltningOppdragRestTjeneste ved behov");
        }
    }

    private void kvitterBortEksisterendeOppdrag(OppdragPatchDto dto, Oppdragskontroll oppdragskontroll) {
        for (var eksisterendeOppdrag110 : oppdragskontroll.getOppdrag110Liste()) {
            if (eksisterendeOppdrag110.getFagsystemId() == dto.getFagsystemId() && eksisterendeOppdrag110.venterKvittering()) {
                OppdragKvittering.builder()
                    .medOppdrag110(eksisterendeOppdrag110)
                    .medAlvorlighetsgrad(Alvorlighetsgrad.OK_MED_MERKNAD)
                    .medBeskrMelding("Erstattes av nytt oppdrag")
                    .build();
                LOG.info(
                    "Eksisterende oppdrag for behandlingId={} fagsystemId={} som ventet på kvittering, ble satt til kvittert med feilkode slik at oppdraget ikke tas i betraktning i senere behandlinger.",
                    dto.getBehandlingId(), dto.getFagsystemId());
            }
        }
    }

    private void kvitterBortEksisterendeOppdrag(Oppdragskontroll oppdragskontroll, long fagsystemId) {
        var oppdrag = oppdragskontroll.getOppdrag110Liste().stream()
            .filter(oppdrag110 -> oppdrag110.getFagsystemId() == fagsystemId).findFirst().orElseThrow(() -> new IllegalStateException("Forventer å finne oppdrag."));

        var oppdragKvittering = oppdrag.getOppdragKvittering();
        oppdragKvittering.setAlvorlighetsgrad(Alvorlighetsgrad.FEIL);
        oppdragKvittering.setBeskrMelding("Erstattes med nytt oppdrag pga feil max dato.");
        økonomioppdragRepository.lagre(oppdragKvittering);

        LOG.info(
            "Eksisterende oppdrag for behandlingId={} fagsystemId={} som har kvittering, ble kvittert med feilkode slik at oppdraget ikke tas i betraktning i senere behandlinger og sendes på nytt.",
            oppdragskontroll.getBehandlingId(), fagsystemId);
    }

    private void byttStatusTilVenterPåKvittering(ProsessTaskData task) {
        task.venterPåHendelse(BehandleØkonomioppdragKvittering.ØKONOMI_OPPDRAG_KVITTERING);
        taskTjeneste.lagre(task);
    }

    private void lagSendØkonomioppdragTask(ProsessTaskData hovedProsessTask, boolean hardPatch) {
        var sendØkonomiOppdrag = ProsessTaskData.forProsessTask(SendØkonomiOppdragTask.class);
        sendØkonomiOppdrag.setGruppe(hovedProsessTask.getGruppe());
        sendØkonomiOppdrag.setCallIdFraEksisterende();
        sendØkonomiOppdrag.setProperty("patchet", hardPatch ? "hardt" : "vanlig"); // for sporing
        sendØkonomiOppdrag.setBehandling(hovedProsessTask.getFagsakId(),
            Long.valueOf(hovedProsessTask.getBehandlingId()),
            hovedProsessTask.getAktørId());
        taskTjeneste.lagre(sendØkonomiOppdrag);
    }

    private void lagSendØkonomioppdragTask(Behandling behandling) {
        var sendØkonomiOppdrag = ProsessTaskData.forProsessTask(SendØkonomiOppdragTask.class);
        sendØkonomiOppdrag.setCallIdFraEksisterende();
        sendØkonomiOppdrag.setProperty("patchet", "k27rapport"); // for sporing
        sendØkonomiOppdrag.setBehandling(behandling.getFagsakId(),
            behandling.getId(),
            behandling.getAktørId().getId());
        taskTjeneste.lagre(sendØkonomiOppdrag);
    }

    private void lagVurderOgSendØkonomioppdragTask(Behandling behandling) {
        var sendØkonomiOppdrag = ProsessTaskData.forProsessTask(VurderOgSendØkonomiOppdragTask.class);
        sendØkonomiOppdrag.setCallIdFraEksisterende();
        sendØkonomiOppdrag.setProperty("patchet", "refusjonsinfo-maxDato"); // for sporing
        sendØkonomiOppdrag.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskTjeneste.lagre(sendØkonomiOppdrag);
    }

    private int finnAntallPatchedeSistePeriode(EntityManager entityManager, Period periode) {
        var query = entityManager.createNativeQuery(
            "select count(*) from PROSESS_TASK where TASK_TYPE=:task_type AND OPPRETTET_TID > cast(:opprettet_fom as timestamp(0)) AND TASK_PARAMETERE like '%patchet%'")
            .setParameter("task_type", TaskType.forProsessTask(SendØkonomiOppdragTask.class).value())
            .setParameter("opprettet_fom", ZonedDateTime.now().minus(periode));

        var result = (BigDecimal) query.getSingleResult();
        return result.intValue();
    }

    private void validerUferdigProsesstask(ProsessTaskData task) {
        var status = task.getStatus();
        if (status != ProsessTaskStatus.VENTER_SVAR && status != ProsessTaskStatus.FEILET) {
            throw new ForvaltningException("Kan ikke patche oppdrag som er ferdig. Kan kun brukes når prosesstask er FEILET eller VENTER_SVAR");
        }
        if (status == ProsessTaskStatus.VENTER_SVAR && ChronoUnit.MINUTES.between(task.getSistKjørt(), LocalDateTime.now()) > 120) {
            throw new ForvaltningException(
                "Skal ikke patche oppdrag uten at OS har fått rimelig tid til å svare (sanity check). Prøv igjen senere.");
        }
    }

    private void validerFerdigProsesstask(ProsessTaskData task) {
        var status = task.getStatus();
        if (status != ProsessTaskStatus.FERDIG) {
            throw new ForvaltningException("Denne skal kun brukes for FERDIG prosesstask. Se om du heller skal bruke endepunktet /patch-oppdrag");
        }
        if (ChronoUnit.DAYS.between(task.getSistKjørt(), LocalDateTime.now()) > 90) {
            throw new ForvaltningException(
                "Skal ikke patche oppdrag som er så gamle som dette (sanity check). Endre grensen i java-koden hvis det er strengt nødvendig.");
        }
    }

    private void validerFagsystemId(Behandling behandling, long fagsystemId) {
        if (!Long.toString(fagsystemId / 1000).equals(behandling.getFagsak().getSaksnummer().getVerdi())) {
            throw new ForvaltningException("FagsystemId=" + fagsystemId + " passer ikke med saksnummer for behandlingId=" + behandling.getId());
        }
    }

    private void validerDelytelseId(OppdragPatchDto dto) {
        for (var linje : dto.getOppdragslinjer()) {
            if (dto.getFagsystemId() != linje.getDelytelseId() / 1000) {
                throw new ForvaltningException("FagsystemId=" + dto.getFagsystemId() + " matcher ikke med delytelseId=" + linje.getDelytelseId());
            }
            if (linje.getRefFagsystemId() != null && dto.getFagsystemId() != linje.getRefFagsystemId()) {
                throw new ForvaltningException(
                    "FagsystemId=" + dto.getFagsystemId() + " matcher ikke med refFagsystemId=" + linje.getRefFagsystemId());
            }
            if (linje.getRefDelytelseId() != null && dto.getFagsystemId() != linje.getRefDelytelseId() / 1000) {
                throw new ForvaltningException(
                    "FagsystemId=" + dto.getFagsystemId() + " matcher ikke med refDelytelseId=" + linje.getRefDelytelseId());
            }
        }
    }
}
