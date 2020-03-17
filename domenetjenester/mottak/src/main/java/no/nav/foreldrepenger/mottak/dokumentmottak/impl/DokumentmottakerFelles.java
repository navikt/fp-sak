package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@Dependent
public class DokumentmottakerFelles {

    private ProsessTaskRepository prosessTaskRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRepository behandlingRepository;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private Behandlingsoppretter behandlingsoppretter;
    private BehandlingRevurderingRepository revurderingRepository;

    @SuppressWarnings("unused")
    private DokumentmottakerFelles() { // NOSONAR
        // For CDI
    }

    @Inject
    public DokumentmottakerFelles(BehandlingRepositoryProvider repositoryProvider,
                                  ProsessTaskRepository prosessTaskRepository,
                                  BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                  HistorikkinnslagTjeneste historikkinnslagTjeneste,
                                  MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                  Behandlingsoppretter behandlingsoppretter) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
        this.revurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
    }

    void leggTilBehandlingsårsak(Behandling behandling, BehandlingÅrsakType behandlingÅrsak) {
        BehandlingÅrsak.Builder builder = BehandlingÅrsak.builder(behandlingÅrsak);
        behandling.getOriginalBehandling().ifPresent(builder::medOriginalBehandling);
        builder.buildFor(behandling);

        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);
    }

    void opprettTaskForÅStarteBehandling(Behandling behandling) {
        ProsessTaskData prosessTaskData = new ProsessTaskData(StartBehandlingTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    public void opprettTaskForÅVurdereDokument(Fagsak fagsak, Behandling behandling, MottattDokument mottattDokument) {
        String behandlendeEnhetsId = hentBehandlendeEnhetTilVurderDokumentOppgave(mottattDokument, fagsak, behandling);
        ProsessTaskData prosessTaskData = new ProsessTaskData(OpprettOppgaveVurderDokumentTask.TASKTYPE);
        prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_DOKUMENT_TYPE, mottattDokument.getDokumentType().getKode());
        prosessTaskData.setFagsak(fagsak.getId(), fagsak.getAktørId().getId());
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    public void opprettKøetHistorikk(Behandling køetBehandling, boolean fantesFraFør) {
        if (!fantesFraFør) {
            opprettHistorikkinnslagForVenteFristRelaterteInnslag(køetBehandling, HistorikkinnslagType.BEH_KØET, null, Venteårsak.VENT_ÅPEN_BEHANDLING);
        }
    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Behandling behandling, HistorikkinnslagType historikkinnslagType, LocalDateTime frist, Venteårsak venteårsak) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForVenteFristRelaterteInnslag(behandling, historikkinnslagType, frist, venteårsak);
    }

    void opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(Behandling behandling, MottattDokument mottattDokument) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(behandling);
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, mottattDokument.getJournalpostId(), true);
    }

    void opprettHistorikk(Behandling behandling, JournalpostId journalPostId) {
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, journalPostId, false);
    }

    void opprettHistorikkinnslagForVedlegg(Long fagsakId, JournalpostId journalpostId, DokumentTypeId dokumentTypeId) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(fagsakId, journalpostId, dokumentTypeId);
    }

    String hentBehandlendeEnhetTilVurderDokumentOppgave(MottattDokument dokument, Fagsak sak, Behandling behandling) {
        // Prod: Klageinstans + Viken sender dokumenter til scanning med forside som inneholder enhet. Journalføring og Vurder dokument skal til enheten.
        if (dokument.getJournalEnhet().isPresent() && behandlendeEnhetTjeneste.gyldigEnhetNfpNk(dokument.getJournalEnhet().get())) {
            return dokument.getJournalEnhet().get();
        }
        if (behandling == null) {
            return finnEnhetFraFagsak(sak);
        }
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsak().getId())
                .map(Behandling::getBehandlendeEnhet)
                .orElse(finnEnhetFraFagsak(sak));
        }
        return behandling.getBehandlendeEnhet();
    }

    private String finnEnhetFraFagsak(Fagsak sak) {
        OrganisasjonsEnhet organisasjonsEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(sak);
        return organisasjonsEnhet.getEnhetId();
    }

    OrganisasjonsEnhet utledEnhetFraTidligereBehandling(Behandling tidligereBehandling) {
        // Utleder basert på regler rundt sakskompleks og diskresjonskoder. Vil bruke forrige enhet med mindre noen tilsier Kode6 eller opphør av enhet
        return behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(tidligereBehandling.getFagsak());
    }

    void opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(behandling, behandlingÅrsakType);
    }

    final Behandling opprettRevurdering(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Behandling revurdering = behandlingsoppretter.opprettRevurdering(fagsak, behandlingÅrsakType);
        mottatteDokumentTjeneste.persisterDokumentinnhold(revurdering, mottattDokument, Optional.empty());
        opprettTaskForÅStarteBehandling(revurdering);
        return revurdering;
    }

    final Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Behandling revurdering = behandlingsoppretter.opprettManuellRevurdering(fagsak, behandlingÅrsakType);
        opprettTaskForÅStarteBehandling(revurdering);
        return revurdering;
    }

    Behandling oppdatereViaHenleggelse(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsak) {
        Behandling nyBehandling = behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling, behandlingÅrsak);
        opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(behandling, mottattDokument);
        Optional<LocalDate> søknadsdato = revurderingRepository.finnSøknadsdatoFraHenlagtBehandling(nyBehandling);
        mottatteDokumentTjeneste.persisterDokumentinnhold(nyBehandling, mottattDokument, søknadsdato);
        return nyBehandling;
    }

    boolean skalOppretteNyFørstegangsbehandling(Fagsak fagsak) {
        if (mottatteDokumentTjeneste.erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(fagsak)) {
            return !mottatteDokumentTjeneste.harFristForInnsendingAvDokGåttUt(fagsak);
        }
        return false;
    }

    Behandling opprettNyFørstegangFraAvslag(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling) {
        Behandling nyBehandling = behandlingsoppretter.opprettNyFørstegangsbehandling(mottattDokument, fagsak, avsluttetBehandling);
        behandlingsoppretter.opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(fagsak.getSaksnummer(), nyBehandling);
        opprettTaskForÅStarteBehandling(nyBehandling);
        return nyBehandling;
    }

    Behandling opprettNyFørstegangFraBehandlingMedSøknad(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling avsluttetBehandling, MottattDokument mottattDokument) {
        Behandling nyBehandling = behandlingsoppretter.opprettNyFørstegangsbehandlingFraTidligereSøknad(fagsak, behandlingÅrsakType, avsluttetBehandling);
        behandlingsoppretter.opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(fagsak.getSaksnummer(), nyBehandling);
        historikkinnslagTjeneste.opprettHistorikkinnslag(nyBehandling, mottattDokument.getJournalpostId(), false);
        opprettTaskForÅStarteBehandling(nyBehandling);
        return nyBehandling;
    }
}
