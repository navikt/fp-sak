package no.nav.foreldrepenger.behandlingsprosess.prosessering.tjeneste;

import static no.nav.foreldrepenger.behandlingsprosess.prosessering.task.FortsettBehandlingTask.GJENOPPTA_STEG;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
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
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;

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

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingProsesseringTjenesteImpl.class);

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RegisterdataEndringshåndterer registerdataEndringshåndterer;
    private EndringsresultatSjekker endringsresultatSjekker;
    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    public BehandlingProsesseringTjenesteImpl(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            RegisterdataEndringshåndterer registerdataEndringshåndterer,
            EndringsresultatSjekker endringsresultatSjekker,
            ProsessTaskRepository prosessTaskRepository) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.registerdataEndringshåndterer = registerdataEndringshåndterer;
        this.endringsresultatSjekker = endringsresultatSjekker;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public BehandlingProsesseringTjenesteImpl() {
        // NOSONAR
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
        return erAlleredeOpprettetOppdateringFor(behandling);
    }

    @Override
    public ProsessTaskGruppe lagOppdaterFortsettTasksForPolling(Behandling behandling) {
        return lagTasksForOppdatering(behandling, LocalDateTime.now());
    }

    // Til bruk ved første prosessering av nyopprettet behandling
    @Override
    public String opprettTasksForStartBehandling(Behandling behandling) {
        var taskData = lagTaskData(StartBehandlingTask.TASKTYPE, behandling, LocalDateTime.now());
        return lagreEnkeltTask(taskData);
    }

    @Override
    public boolean kanOppretteTasksForFortsettBehandling(Behandling behandling) {
        return erAlleredeOpprettetOppdateringFor(behandling).isEmpty();
    }

    // Til bruk ved gjenopptak fra vent (Hendelse: Manuell input, Frist utløpt, mv)
    @Override
    public String opprettTasksForFortsettBehandling(Behandling behandling) {
        return erAlleredeOpprettetOppdateringFor(behandling)
            .orElseGet(() -> {
                var taskData = lagTaskData(FortsettBehandlingTask.TASKTYPE, behandling, LocalDateTime.now());
                taskData.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
                return lagreEnkeltTask(taskData);
            });
    }

    @Override
    public String opprettTasksForFortsettBehandlingSettUtført(Behandling behandling, Optional<AksjonspunktDefinisjon> autopunktUtført) {
        var taskData = lagTaskData(FortsettBehandlingTask.TASKTYPE, behandling, LocalDateTime.now());
        autopunktUtført.ifPresent(apu -> taskData.setProperty(FortsettBehandlingTask.UTFORT_AUTOPUNKT, apu.getKode()));
        return lagreEnkeltTask(taskData);
    }

    @Override
    public String opprettTasksForFortsettBehandlingResumeStegNesteKjøring(Behandling behandling, BehandlingStegType behandlingStegType,
                                                                          LocalDateTime nesteKjøringEtter) {
        var taskData = lagTaskData(FortsettBehandlingTask.TASKTYPE, behandling, nesteKjøringEtter);
        taskData.setProperty(FortsettBehandlingTask.GJENOPPTA_STEG, behandlingStegType.getKode());
        return lagreEnkeltTask(taskData);
    }

    // Robust task til bruk ved gjenopptak fra vent (eller annen tilstand)
    // (Hendelse: Manuell input, Frist utløpt, mv)
    @Override
    public String opprettTasksForGjenopptaOppdaterFortsett(Behandling behandling, LocalDateTime nesteKjøringEtter) {
        return erAlleredeOpprettetOppdateringFor(behandling)
            .orElseGet(() -> {
                var gruppe = lagTasksForOppdatering(behandling, nesteKjøringEtter);
                return prosessTaskRepository.lagre(gruppe);
            });
    }

    @Override
    public String opprettTasksForInitiellRegisterInnhenting(Behandling behandling) {
        var gruppe = new ProsessTaskGruppe();

        leggTilTasksForRegisterinnhenting(behandling, gruppe, LocalDateTime.now());

        var fortsettBehandlingTask = lagTaskData(FortsettBehandlingTask.TASKTYPE, behandling, LocalDateTime.now());

        // NB: Viktig Starter opp prosessen igjen fra steget hvor den var satt på vent
        fortsettBehandlingTask.setProperty(GJENOPPTA_STEG, BehandlingStegType.INNHENT_REGISTEROPP.getKode());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);
        return prosessTaskRepository.lagre(gruppe);
    }

    private ProsessTaskGruppe lagTasksForOppdatering(Behandling behandling, LocalDateTime nesteKjøringEtter) {
        var gruppe = new ProsessTaskGruppe();
        var gjenopptaTask = lagTaskData(GjenopptaBehandlingTask.TASKTYPE, behandling, nesteKjøringEtter);
        gruppe.addNesteSekvensiell(gjenopptaTask);

        if (behandling.erYtelseBehandling() && registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling)) {
            leggTilTasksForRegisterinnhenting(behandling, gruppe, nesteKjøringEtter);
            var registerdataOppdatererTask = lagTaskData(RegisterdataOppdatererTask.TASKTYPE, behandling, nesteKjøringEtter);
            var snapshot = endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandling.getId());
            registerdataOppdatererTask.setPayload(StandardJsonConfig.toJson(snapshot));
            gruppe.addNesteSekvensiell(registerdataOppdatererTask);
        }
        var fortsettBehandlingTask = lagTaskData(FortsettBehandlingTask.TASKTYPE, behandling, nesteKjøringEtter);
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);
        return gruppe;
    }

    private void leggTilTasksForRegisterinnhenting(Behandling behandling, ProsessTaskGruppe gruppe, LocalDateTime nesteKjøringEtter) {

        var innhentPersonopplysniger = lagTaskData(InnhentPersonopplysningerTask.TASKTYPE, behandling, nesteKjøringEtter);
        var innhentMedlemskapOpplysniger = lagTaskData(InnhentMedlemskapOpplysningerTask.TASKTYPE, behandling, nesteKjøringEtter);
        var abakusRegisterInnheting = lagTaskData(InnhentIAYIAbakusTask.TASKTYPE, behandling, nesteKjøringEtter);

        gruppe.addNesteParallell(innhentPersonopplysniger, innhentMedlemskapOpplysniger, abakusRegisterInnheting);

        var oppdaterInnhentTidspunkt = lagTaskData(SettRegisterdataInnhentetTidspunktTask.TASKTYPE, behandling, nesteKjøringEtter);
        gruppe.addNesteSekvensiell(oppdaterInnhentTidspunkt);
    }

    private ProsessTaskData lagTaskData(String tasktype, Behandling behandling, LocalDateTime nesteKjøringEtter) {
        var taskdata = new ProsessTaskData(tasktype);
        taskdata.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskdata.setCallIdFraEksisterende();
        if (nesteKjøringEtter != null) {
            taskdata.setNesteKjøringEtter(nesteKjøringEtter);
        }
        taskdata.setPrioritet(50);
        return taskdata;
    }

    private String lagreEnkeltTask(ProsessTaskData prosessTaskData) {
        return prosessTaskRepository.lagre(prosessTaskData);
    }

    private Optional<String> erAlleredeOpprettetOppdateringFor(Behandling behandling) {
        return prosessTaskRepository.finnAlle(ProsessTaskStatus.VENTER_SVAR, ProsessTaskStatus.KLAR).stream()
            .filter(it -> it.getBehandlingId().equals("" + behandling.getId()))
            .filter(it -> InnhentIAYIAbakusTask.TASKTYPE.equals(it.getTaskType()))
            .map(ProsessTaskData::getGruppe)
            .findFirst();
    }
}
