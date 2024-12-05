package no.nav.foreldrepenger.behandlingsprosess.prosessering.tjeneste;

import static no.nav.foreldrepenger.behandlingsprosess.prosessering.task.FortsettBehandlingTask.GJENOPPTA_STEG;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.FortsettBehandlingTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.GjenopptaBehandlingTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.foreldrepenger.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataEndringshåndterer;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.RegisterdataOppdatererTask;
import no.nav.foreldrepenger.domene.registerinnhenting.task.InnhentIAYIAbakusTask;
import no.nav.foreldrepenger.domene.registerinnhenting.task.InnhentMedlemskapOpplysningerTask;
import no.nav.foreldrepenger.domene.registerinnhenting.task.InnhentPersonopplysningerTask;
import no.nav.foreldrepenger.domene.registerinnhenting.task.SettRegisterdataInnhentetTidspunktTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.felles.prosesstask.api.TaskType;

/**
 * Grensesnitt for å kjøre behandlingsprosess, herunder gjenopptak,
 * registeroppdatering, koordinering av sakskompleks mv. Alle kall til
 * utføringsmetode i behandlingskontroll bør gå gjennom tasks opprettet her.
 * Merk Dem: - ta av vent og grunnlagsoppdatering kan føre til reposisjonering
 * av behandling til annet steg - grunnlag endres ved ankomst av dokument, ved
 * registerinnhenting og ved senere overstyring ("bekreft AP" eller egne
 * overstyringAP) - Hendelser: Ny behandling (Manuell, dokument, mv), Gjenopptak
 * (Manuell/Frist), Interaktiv (Oppdater/Fortsett), Dokument, Datahendelse,
 * Vedtak, KØ-hendelser
 **/
@ApplicationScoped
public class BehandlingProsesseringTjenesteImpl implements BehandlingProsesseringTjeneste {

    private static final TaskType TASK_START = TaskType.forProsessTask(StartBehandlingTask.class);
    private static final TaskType TASK_FORTSETT = TaskType.forProsessTask(FortsettBehandlingTask.class);
    private static final TaskType TASK_ABAKUS = TaskType.forProsessTask(InnhentIAYIAbakusTask.class);

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RegisterdataEndringshåndterer registerdataEndringshåndterer;
    private EndringsresultatSjekker endringsresultatSjekker;
    private ProsessTaskTjeneste taskTjeneste;

    @Inject
    public BehandlingProsesseringTjenesteImpl(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            RegisterdataEndringshåndterer registerdataEndringshåndterer,
            EndringsresultatSjekker endringsresultatSjekker,
            ProsessTaskTjeneste taskTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.registerdataEndringshåndterer = registerdataEndringshåndterer;
        this.endringsresultatSjekker = endringsresultatSjekker;
        this.taskTjeneste = taskTjeneste;
    }

    public BehandlingProsesseringTjenesteImpl() {

    }

    @Override
    public boolean skalInnhenteRegisteropplysningerPåNytt(Behandling behandling) {
        return registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);
    }

    @Override
    public void tvingInnhentingRegisteropplysninger(Behandling behandling) {
        registerdataEndringshåndterer.sikreInnhentingRegisteropplysningerVedNesteOppdatering(behandling);
    }

    @Override
    public boolean erStegAktueltForBehandling(Behandling behandling, BehandlingStegType behandlingStegType) {
        return behandlingskontrollTjeneste.inneholderSteg(behandling, behandlingStegType);
    }

    // AV/PÅ Vent
    @Override
    public void taBehandlingAvVent(Behandling behandling) {
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
    }

    @Override
    public void settBehandlingPåVent(Behandling behandling, AksjonspunktDefinisjon apDef, LocalDateTime fristTid, Venteårsak venteårsak) {
        behandlingskontrollTjeneste.settBehandlingPåVent(behandling, apDef, behandling.getAktivtBehandlingSteg(), fristTid, venteårsak);
    }

    // For snapshot av grunnlag før man gjør andre endringer enn registerinnhenting
    @Override
    public EndringsresultatSnapshot taSnapshotAvBehandlingsgrunnlag(Behandling behandling) {
        return endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandling.getId());
    }

    @Override
    public void utledDiffOgReposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatSnapshot snapshot) {
        registerdataEndringshåndterer.utledDiffOgReposisjonerBehandlingVedEndringer(behandling, snapshot, false);
    }

    @Override
    public void reposisjonerBehandlingTilbakeTil(Behandling behandling, BehandlingStegType stegType) {
        if (behandlingskontrollTjeneste.inneholderSteg(behandling, stegType)) {
            var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
            behandlingskontrollTjeneste.behandlingTilbakeføringHvisTidligereBehandlingSteg(kontekst, stegType);
        }
    }

    @Override
    public Optional<String> finnesTasksForPolling(Behandling behandling) {
        return erAlleredeOpprettetOppdateringFor(behandling)
            .or(() -> finnesFeiletOppdateringFor(behandling));
    }

    @Override
    public ProsessTaskGruppe lagOppdaterFortsettTasksForPolling(Behandling behandling) {
        return lagTasksForOppdatering(behandling, LocalDateTime.now(), 1);
    }

    // Til bruk ved første prosessering av nyopprettet behandling
    @Override
    public String opprettTasksForStartBehandling(Behandling behandling) {
        var taskData = lagTaskData(TASK_START, behandling, LocalDateTime.now(), 2);
        return lagreEnkeltTask(taskData);
    }

    // Til bruk ved gjenopptak fra vent (Hendelse: Manuell input, Frist utløpt, mv)
    @Override
    public String opprettTasksForFortsettBehandling(Behandling behandling) {
        return erAlleredeOpprettetOppdateringFor(behandling)
            .orElseGet(() -> {
                var taskData = lagTaskData(TASK_FORTSETT, behandling, LocalDateTime.now(), 1);
                taskData.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
                return lagreEnkeltTask(taskData);
            });
    }

    @Override
    public String opprettTasksForFortsettBehandlingSettUtført(Behandling behandling, Optional<AksjonspunktDefinisjon> autopunktUtført) {
        var taskData = lagTaskData(TASK_FORTSETT, behandling, LocalDateTime.now(), 2);
        autopunktUtført.ifPresent(apu -> taskData.setProperty(FortsettBehandlingTask.UTFORT_AUTOPUNKT, apu.getKode()));
        return lagreEnkeltTask(taskData);
    }

    @Override
    public String opprettTasksForFortsettBehandlingResumeStegNesteKjøring(Behandling behandling, BehandlingStegType behandlingStegType,
                                                                          LocalDateTime nesteKjøringEtter) {
        var taskData = lagTaskData(TASK_FORTSETT, behandling, nesteKjøringEtter, 2);
        taskData.setProperty(FortsettBehandlingTask.GJENOPPTA_STEG, behandlingStegType.getKode());
        return lagreEnkeltTask(taskData);
    }

    // Robust task til bruk ved gjenopptak fra vent (eller annen tilstand)
    // (Hendelse: Manuell input, Frist utløpt, mv)
    @Override
    public String opprettTasksForGjenopptaOppdaterFortsett(Behandling behandling, LocalDateTime nesteKjøringEtter) {
        return erAlleredeOpprettetOppdateringFor(behandling)
            .or(() -> finnesFeiletOppdateringFor(behandling))
            .orElseGet(() -> {
                var gruppe = lagTasksForOppdatering(behandling, nesteKjøringEtter, 3);
                return taskTjeneste.lagre(gruppe);
            });
    }

    // Robust task til bruk ved batch-gjenopptak fra vent - forutsetter sjekk på at ikke allerede finnes
    @Override
    public String opprettTasksForGjenopptaOppdaterFortsettBatch(Behandling behandling, LocalDateTime nesteKjøringEtter) {
        var gruppe = lagTasksForOppdatering(behandling, nesteKjøringEtter, 3);
        return taskTjeneste.lagre(gruppe);
    }

    @Override
    public String opprettTasksForInitiellRegisterInnhenting(Behandling behandling) {
        var gruppe = new ProsessTaskGruppe();

        leggTilTasksForRegisterinnhenting(behandling, gruppe, LocalDateTime.now(), 3);

        var fortsettBehandlingTask = lagTaskData(TASK_FORTSETT, behandling, LocalDateTime.now(), 3);

        // NB: Viktig Starter opp prosessen igjen fra steget hvor den var satt på vent
        fortsettBehandlingTask.setProperty(GJENOPPTA_STEG, BehandlingStegType.INNHENT_REGISTEROPP.getKode());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);
        return taskTjeneste.lagre(gruppe);
    }

    private ProsessTaskGruppe lagTasksForOppdatering(Behandling behandling, LocalDateTime nesteKjøringEtter, int prioritet) {
        var gruppe = new ProsessTaskGruppe();
        var gjenopptaTask = lagTaskData(TaskType.forProsessTask(GjenopptaBehandlingTask.class), behandling, nesteKjøringEtter, prioritet);
        gruppe.addNesteSekvensiell(gjenopptaTask);

        if (behandling.erYtelseBehandling() && registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling)) {
            leggTilTasksForRegisterinnhenting(behandling, gruppe, nesteKjøringEtter, prioritet);
            var registerdataOppdatererTask = lagTaskData(TaskType.forProsessTask(RegisterdataOppdatererTask.class), behandling, nesteKjøringEtter, prioritet);
            var snapshot = endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandling.getId());
            registerdataOppdatererTask.setPayload(StandardJsonConfig.toJson(snapshot));
            gruppe.addNesteSekvensiell(registerdataOppdatererTask);
        }
        var fortsettBehandlingTask = lagTaskData(TASK_FORTSETT, behandling, nesteKjøringEtter, prioritet);
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);
        return gruppe;
    }

    private void leggTilTasksForRegisterinnhenting(Behandling behandling, ProsessTaskGruppe gruppe, LocalDateTime nesteKjøringEtter, int prioritet) {
        var tasker = new ArrayList<ProsessTaskData>();
        tasker.add(lagTaskData(TaskType.forProsessTask(InnhentPersonopplysningerTask.class), behandling, nesteKjøringEtter, prioritet));
        tasker.add(lagTaskData(TaskType.forProsessTask(InnhentMedlemskapOpplysningerTask.class), behandling, nesteKjøringEtter, prioritet));
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            tasker.add(lagTaskData(TaskType.forProsessTask(InnhentIAYIAbakusTask.class), behandling, nesteKjøringEtter, prioritet));
        }
        gruppe.addNesteParallell(tasker);
        var oppdaterInnhentTidspunkt = lagTaskData(TaskType.forProsessTask(SettRegisterdataInnhentetTidspunktTask.class), behandling, nesteKjøringEtter, prioritet);
        gruppe.addNesteSekvensiell(oppdaterInnhentTidspunkt);
    }

    private ProsessTaskData lagTaskData(TaskType tasktype, Behandling behandling, LocalDateTime nesteKjøringEtter, int prioritet) {
        var taskdata = ProsessTaskData.forTaskType(tasktype);
        taskdata.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        if (nesteKjøringEtter != null) {
            taskdata.setNesteKjøringEtter(nesteKjøringEtter);
        }
        taskdata.setPrioritet(prioritet);
        return taskdata;
    }

    private String lagreEnkeltTask(ProsessTaskData prosessTaskData) {
        return taskTjeneste.lagre(prosessTaskData);
    }

    private Optional<String> erAlleredeOpprettetOppdateringFor(Behandling behandling) {
        return taskTjeneste.finnAlleStatuser(List.of(ProsessTaskStatus.VENTER_SVAR, ProsessTaskStatus.KLAR)).stream()
            .filter(it -> TASK_ABAKUS.equals(it.taskType()))
            .filter(it -> it.getBehandlingIdAsLong().equals(behandling.getId()))
            .map(ProsessTaskData::getGruppe)
            .findFirst();
    }

    private Optional<String> finnesFeiletOppdateringFor(Behandling behandling) {
        return taskTjeneste.finnAlle(ProsessTaskStatus.FEILET).stream()
            .filter(it -> TASK_ABAKUS.equals(it.taskType()))
            .filter(it -> it.getBehandlingIdAsLong().equals(behandling.getId()))
            .map(ProsessTaskData::getGruppe)
            .findFirst();
    }

    @Override
    public Set<Long> behandlingerMedFeiletProsessTask() {
        return taskTjeneste.finnAlle(ProsessTaskStatus.FEILET).stream()
            .filter(it -> TASK_FORTSETT.equals(it.taskType()) || TASK_START.equals(it.taskType()))
            .map(ProsessTaskData::getBehandlingIdAsLong)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
}
