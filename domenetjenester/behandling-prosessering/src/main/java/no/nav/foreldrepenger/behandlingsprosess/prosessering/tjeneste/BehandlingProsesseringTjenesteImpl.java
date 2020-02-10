package no.nav.foreldrepenger.behandlingsprosess.prosessering.tjeneste;

import static no.nav.foreldrepenger.behandlingsprosess.prosessering.task.FortsettBehandlingTaskProperties.GJENOPPTA_STEG;

import java.time.LocalDateTime;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.FortsettBehandlingTaskProperties;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.GjenopptaBehandlingTask;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
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

/**
 * Grensesnitt for å kjøre behandlingsprosess, herunder gjenopptak, registeroppdatering, koordinering av sakskompleks mv.
 * Alle kall til utføringsmetode i behandlingskontroll bør gå gjennom tasks opprettet her.
 * Merk Dem:
 * - ta av vent og grunnlagsoppdatering kan føre til reposisjonering av behandling til annet steg
 * - grunnlag endres ved ankomst av dokument, ved registerinnhenting og ved senere overstyring ("bekreft AP" eller egne overstyringAP)
 * - Hendelser: Ny behandling (Manuell, dokument, mv), Gjenopptak (Manuell/Frist), Interaktiv (Oppdater/Fortsett), Dokument, Datahendelse, Vedtak, KØ-hendelser
 **/
@ApplicationScoped
public class BehandlingProsesseringTjenesteImpl implements BehandlingProsesseringTjeneste {

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
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
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

    // Returnerer snapshot av grunnlag før registerinnhentingen. Forutsetter at behandling ikke er på vent.
    @Override
    public EndringsresultatSnapshot oppdaterRegisterdata(Behandling behandling) {
        return null; // TODO: trengs denne?
    }

    // Returnerer endringer i grunnlag mellom snapshot og nåtilstand
    @Override
    public EndringsresultatDiff finnGrunnlagsEndring(Behandling behandling, EndringsresultatSnapshot før) {
        return endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(behandling.getId(), før);
    }

    @Override
    public void reposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatDiff grunnlagDiff) {
        registerdataEndringshåndterer.reposisjonerBehandlingVedEndringer(behandling, grunnlagDiff);
    }

    @Override
    public void reposisjonerBehandlingTilbakeTil(Behandling behandling, BehandlingStegType stegType) {
        if (behandlingskontrollTjeneste.inneholderSteg(behandling, stegType)) {
            BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
            behandlingskontrollTjeneste.behandlingTilbakeføringHvisTidligereBehandlingSteg(kontekst, stegType);
        }
    }

    @Override
    public void oppdaterRegisterdataReposisjonerVedEndringer(Behandling behandling) {
        registerdataEndringshåndterer.oppdaterRegisteropplysningerOgReposisjonerBehandlingVedEndringer(behandling);
    }

    @Override
    public ProsessTaskGruppe lagOppdaterFortsettTasksForPolling(Behandling behandling) {
        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        ProsessTaskData registerdataOppdatererTask = new ProsessTaskData(RegisterdataOppdatererTask.TASKTYPE);
        registerdataOppdatererTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        registerdataOppdatererTask.setCallIdFraEksisterende();
        gruppe.addNesteSekvensiell(registerdataOppdatererTask);
        ProsessTaskData fortsettBehandlingTask = new ProsessTaskData(FortsettBehandlingTaskProperties.TASKTYPE);
        fortsettBehandlingTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTaskProperties.MANUELL_FORTSETTELSE, String.valueOf(true));
        fortsettBehandlingTask.setCallIdFraEksisterende();
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);
        return gruppe;
    }

    // Til bruk ved første prosessering av nyopprettet behandling
    @Override
    public String opprettTasksForStartBehandling(Behandling behandling) {
        ProsessTaskData taskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        return lagreMedCallId(taskData);
    }

    // Til bruk ved gjenopptak fra vent (Hendelse: Manuell input, Frist utløpt, mv)
    @Override
    public String opprettTasksForFortsettBehandling(Behandling behandling) {
        ProsessTaskData taskData = new ProsessTaskData(FortsettBehandlingTaskProperties.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskData.setProperty(FortsettBehandlingTaskProperties.MANUELL_FORTSETTELSE, String.valueOf(true));
        return lagreMedCallId(taskData);
    }

    @Override
    public String opprettTasksForFortsettBehandlingSettUtført(Behandling behandling, Optional<AksjonspunktDefinisjon> autopunktUtført) {
        ProsessTaskData taskData = new ProsessTaskData(FortsettBehandlingTaskProperties.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        autopunktUtført.ifPresent(apu -> {
            taskData.setProperty(FortsettBehandlingTaskProperties.UTFORT_AUTOPUNKT, apu.getKode());
        });
        return lagreMedCallId(taskData);
    }

    @Override
    public String opprettTasksForFortsettBehandlingGjenopptaStegNesteKjøring(Behandling behandling, BehandlingStegType behandlingStegType, LocalDateTime nesteKjøringEtter) {
        ProsessTaskData taskData = new ProsessTaskData(FortsettBehandlingTaskProperties.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskData.setProperty(FortsettBehandlingTaskProperties.GJENOPPTA_STEG, behandlingStegType.getKode());
        if (nesteKjøringEtter != null) {
            taskData.setNesteKjøringEtter(nesteKjøringEtter);
        }
        return lagreMedCallId(taskData);
    }

    // Til bruk ved gjenopptak fra vent (Hendelse: Manuell input, Frist utløpt, mv)
    @Override
    public String opprettTasksForOppdaterFortsett(Behandling behandling) {
        return prosessTaskRepository.lagre(lagOppdaterFortsettTasksForPolling(behandling));
    }

    // Til bruk ved gjenopptak fra vent (Hendelse: Manuell input, Frist utløpt, mv)
    @Override
    public String opprettTasksForGjenopptaFortsett(Behandling behandling) {
        // TODO: trengs denne? Evt må den over ha manuell fortsettelse false
        ProsessTaskData taskData = new ProsessTaskData(FortsettBehandlingTaskProperties.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        taskData.setProperty(FortsettBehandlingTaskProperties.MANUELL_FORTSETTELSE, String.valueOf(true));
        return lagreMedCallId(taskData);
    }

    // Robust task til bruk ved gjenopptak fra vent (eller annen tilstand) (Hendelse: Manuell input, Frist utløpt, mv)
    @Override
    public String opprettTasksForGjenopptaOppdaterFortsett(Behandling behandling) {
        ProsessTaskData taskData = new ProsessTaskData(GjenopptaBehandlingTask.TASKTYPE);
        taskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        return lagreMedCallId(taskData);
    }

    @Override
    public String opprettTasksForInitiellRegisterInnhenting(Behandling behandling) {
        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();

        ProsessTaskData innhentPersonopplysniger = new ProsessTaskData(InnhentPersonopplysningerTask.TASKTYPE);
        innhentPersonopplysniger.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        innhentPersonopplysniger.setCallIdFraEksisterende();
        ProsessTaskData innhentMedlemskapOpplysniger = new ProsessTaskData(InnhentMedlemskapOpplysningerTask.TASKTYPE);
        innhentMedlemskapOpplysniger.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        innhentMedlemskapOpplysniger.setCallIdFraEksisterende();

        ProsessTaskData abakusRegisterInnheting = new ProsessTaskData(InnhentIAYIAbakusTask.TASKTYPE);
        abakusRegisterInnheting.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        abakusRegisterInnheting.setCallIdFraEksisterende();

        gruppe.addNesteParallell(innhentPersonopplysniger, innhentMedlemskapOpplysniger, abakusRegisterInnheting);

        ProsessTaskData oppdaterInnhentTidspunkt = new ProsessTaskData(SettRegisterdataInnhentetTidspunktTask.TASKTYPE);
        oppdaterInnhentTidspunkt.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        oppdaterInnhentTidspunkt.setCallIdFraEksisterende();
        gruppe.addNesteSekvensiell(oppdaterInnhentTidspunkt);

        // Starter opp prosessen igjen fra steget hvor den var satt på vent
        ProsessTaskData fortsettBehandlingTask = new ProsessTaskData(FortsettBehandlingTaskProperties.TASKTYPE);
        fortsettBehandlingTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        // NB: Viktig
        fortsettBehandlingTask.setProperty(GJENOPPTA_STEG, BehandlingStegType.INNHENT_REGISTEROPP.getKode());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTaskProperties.MANUELL_FORTSETTELSE, String.valueOf(true));
        fortsettBehandlingTask.setCallIdFraEksisterende();
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);
        return prosessTaskRepository.lagre(gruppe);
    }

    private String lagreMedCallId(ProsessTaskData prosessTaskData) {
        prosessTaskData.setCallIdFraEksisterende();
        return prosessTaskRepository.lagre(prosessTaskData);
    }
}
