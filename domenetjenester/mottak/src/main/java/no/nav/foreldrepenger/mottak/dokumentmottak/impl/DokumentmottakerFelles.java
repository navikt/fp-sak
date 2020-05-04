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
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
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
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, mottattDokument.getJournalpostId(), true,
            mottattDokument.getElektroniskRegistrert(), DokumentTypeId.INNTEKTSMELDING.equals(mottattDokument.getDokumentType()));
    }

    void opprettHistorikk(Behandling behandling, MottattDokument mottattDokument) {
        DokumentTypeId dokType = mottattDokument.getDokumentType();
        if (dokType.erSøknadType() || dokType.erEndringsSøknadType() || DokumentTypeId.KLAGE_DOKUMENT.equals(dokType) ||
            DokumentKategori.SØKNAD.equals(mottattDokument.getDokumentKategori())) {
            historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, mottattDokument.getJournalpostId(), false,
                mottattDokument.getElektroniskRegistrert(), false);
        } else {
            historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(behandling.getFagsak(), mottattDokument.getJournalpostId(),
                mottattDokument.getDokumentType(), mottattDokument.getElektroniskRegistrert());
        }
    }

    void opprettHistorikkinnslagForVedlegg(Fagsak fagsak, MottattDokument mottattDokument) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForVedlegg(fagsak, mottattDokument.getJournalpostId(),
            mottattDokument.getDokumentType(), mottattDokument.getElektroniskRegistrert());
    }

    String hentBehandlendeEnhetTilVurderDokumentOppgave(MottattDokument dokument, Fagsak sak, Behandling behandling) {
        // Prod: Klageinstans + Viken sender dokumenter til scanning med forside som inneholder enhet. Journalføring og Vurder dokument skal til enheten.
        if (dokument.getJournalEnhet().map(behandlendeEnhetTjeneste::gyldigEnhetNfpNk).orElse(Boolean.FALSE)) {
            return dokument.getJournalEnhet().get();
        }
        if (behandling == null) {
            return finnEnhetFraFagsak(sak).getEnhetId();
        }
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsak().getId())
                .map(Behandling::getBehandlendeEnhet)
                .orElse(finnEnhetFraFagsak(sak).getEnhetId());
        }
        return behandling.getBehandlendeEnhet();
    }

    OrganisasjonsEnhet finnEnhetFraFagsak(Fagsak sak) {
        return behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(sak);
    }

    void opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(behandling, behandlingÅrsakType);
    }

    final Behandling opprettRevurdering(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Behandling revurdering = behandlingsoppretter.opprettRevurdering(fagsak, behandlingÅrsakType);
        mottatteDokumentTjeneste.persisterDokumentinnhold(revurdering, mottattDokument, Optional.empty());
        opprettHistorikk(revurdering, mottattDokument);
        opprettTaskForÅStarteBehandling(revurdering);
        return revurdering;
    }

    final Behandling opprettKøetRevurdering(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Behandling revurdering = behandlingsoppretter.opprettRevurdering(fagsak, behandlingÅrsakType);
        mottatteDokumentTjeneste.persisterDokumentinnhold(revurdering, mottattDokument, Optional.empty());
        opprettHistorikk(revurdering, mottattDokument);
        leggNyBehandlingPåKøOgOpprettHistorikkinnslag(revurdering);
        return revurdering;
    }

    final Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, boolean opprettSomKøet) {
        Behandling revurdering = behandlingsoppretter.opprettManuellRevurdering(fagsak, behandlingÅrsakType);
        if (opprettSomKøet) {
            leggNyBehandlingPåKøOgOpprettHistorikkinnslag(revurdering);
        } else {
            opprettTaskForÅStarteBehandling(revurdering);
        }
        return revurdering;
    }

    Behandling oppdatereViaHenleggelse(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsak) {
        Behandling nyBehandling = behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling, behandlingÅrsak);
        opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(behandling, mottattDokument);
        Optional<LocalDate> søknadsdato = revurderingRepository.finnSøknadsdatoFraHenlagtBehandling(nyBehandling);
        mottatteDokumentTjeneste.persisterDokumentinnhold(nyBehandling, mottattDokument, søknadsdato);
        return nyBehandling;
    }

    Behandling oppdatereViaHenleggelseEnkø(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsak) {
        Behandling nyBehandling = behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling, behandlingÅrsak);
        opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(behandling, mottattDokument);
        Optional<LocalDate> søknadsdato = revurderingRepository.finnSøknadsdatoFraHenlagtBehandling(nyBehandling);
        mottatteDokumentTjeneste.persisterDokumentinnhold(nyBehandling, mottattDokument, søknadsdato);
        behandlingsoppretter.settSomKøet(nyBehandling);
        return nyBehandling;
    }

    boolean skalOppretteNyFørstegangsbehandling(Fagsak fagsak) {
        if (mottatteDokumentTjeneste.erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(fagsak)) {
            return !mottatteDokumentTjeneste.harFristForInnsendingAvDokGåttUt(fagsak);
        }
        return false;
    }

    public Behandling opprettFørstegangsbehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<Behandling> tidligereBehandling, boolean opprettSomKøet) {
        Behandling nyBehandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, tidligereBehandling);
        if (opprettSomKøet) {
            leggNyBehandlingPåKøOgOpprettHistorikkinnslag(nyBehandling);
        }
        return nyBehandling;
    }

    public Behandling opprettNyFørstegangFraBehandlingMedSøknad(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling avsluttetBehandling, MottattDokument mottattDokument, boolean opprettSomKøet) {
        Behandling nyBehandling = behandlingsoppretter.opprettNyFørstegangsbehandlingFraTidligereSøknad(fagsak, behandlingÅrsakType, avsluttetBehandling);
        behandlingsoppretter.opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(nyBehandling);
        historikkinnslagTjeneste.opprettHistorikkinnslag(nyBehandling, mottattDokument.getJournalpostId(), false,
            mottattDokument.getElektroniskRegistrert(), DokumentTypeId.INNTEKTSMELDING.equals(mottattDokument.getDokumentType()));
        if (opprettSomKøet) {
            leggNyBehandlingPåKøOgOpprettHistorikkinnslag(nyBehandling);
        } else {
            opprettTaskForÅStarteBehandling(nyBehandling);
        }
        return nyBehandling;
    }

    void opprettInitiellFørstegangsbehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#S1
        // Opprett ny førstegangsbehandling
        Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.empty());
        persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
        opprettTaskForÅStarteBehandling(behandling);
        opprettHistorikk(behandling, mottattDokument);
    }

    void opprettKøetInitiellFørstegangsbehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#S1
        // Opprett ny førstegangsbehandling
        Behandling behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.empty());
        persisterDokumentinnhold(behandling, mottattDokument, Optional.empty());
        opprettHistorikk(behandling, mottattDokument);
        behandlingsoppretter.settSomKøet(behandling);
    }

    public void opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Behandling forrigeBehandling = finnEvtForrigeBehandling(mottattDokument, fagsak);
        Behandling nyBehandling = behandlingsoppretter.opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(fagsak, behandlingÅrsakType, forrigeBehandling, !mottattDokument.getDokumentType().erSøknadType());
        persisterDokumentinnhold(nyBehandling, mottattDokument, Optional.empty());
        opprettTaskForÅStarteBehandling(nyBehandling);
        opprettHistorikk(nyBehandling, mottattDokument);
    }

    public void opprettKøetFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        Behandling forrigeBehandling = finnEvtForrigeBehandling(mottattDokument, fagsak);
        Behandling nyBehandling = behandlingsoppretter.opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(fagsak, behandlingÅrsakType, forrigeBehandling, !mottattDokument.getDokumentType().erSøknadType());
        persisterDokumentinnhold(nyBehandling, mottattDokument, Optional.empty());
        opprettHistorikk(nyBehandling, mottattDokument);
        leggNyBehandlingPåKøOgOpprettHistorikkinnslag(nyBehandling);
    }

    private Behandling finnEvtForrigeBehandling(MottattDokument mottattDokument, Fagsak fagsak) {
        Behandling behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
        if (behandling == null && !mottattDokument.getDokumentType().erSøknadType())
            throw new IllegalStateException("Fant ingen behandling som passet for saksnummer: " + fagsak.getSaksnummer());
        return behandling;
    }

    void standardForAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling, BehandlingÅrsakType behandlingÅrsakType, boolean harAvslåttPeriode) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument);
            return;
        }
        if (skalOppretteNyFørstegangsbehandling(avsluttetBehandling.getFagsak())) { //#I3 #E6
            opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType);
        } else if (harAvslåttPeriode && behandlingsoppretter.harBehandlingsresultatOpphørt(avsluttetBehandling)) { //#I4 #E7 #S5
            opprettRevurdering(mottattDokument, fagsak, behandlingÅrsakType);
        } else { //#I5 #E8 #S7
            opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument);
        }
    }

    void oppdaterMottattDokumentMedBehandling(MottattDokument mottattDokument, Long behandlingId) {
        mottatteDokumentTjeneste.oppdaterMottattDokumentMedBehandling(mottattDokument, behandlingId);
    }

    boolean harAlleredeMottattEndringssøknad(Behandling behandling) {
        return mottatteDokumentTjeneste.harMottattDokumentSet(behandling.getId(), DokumentTypeId.getEndringSøknadTyper());
    }

    boolean harMottattSøknadTidligere(Long behandlingId) {
        return mottatteDokumentTjeneste.harMottattDokumentSet(behandlingId, DokumentTypeId.getSøknadTyper()) ||
            mottatteDokumentTjeneste.harMottattDokumentSet(behandlingId, DokumentTypeId.getEndringSøknadTyper()) ||
            mottatteDokumentTjeneste.harMottattDokumentKat(behandlingId, DokumentKategori.SØKNAD);
    }

    boolean harFagsakMottattSøknadTidligere(Long fagsakId) {
        return mottatteDokumentTjeneste.hentMottatteDokumentFagsak(fagsakId).stream()
            .anyMatch(d -> d.getDokumentType().erSøknadType() || DokumentKategori.SØKNAD.equals(d.getDokumentKategori()));
    }

    void persisterDokumentinnhold(Behandling behandling, MottattDokument dokument, Optional<LocalDate> gjelderFra) {
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, dokument, gjelderFra);
    }

    private void leggNyBehandlingPåKøOgOpprettHistorikkinnslag(Behandling nyBehandling) {
        opprettKøetHistorikk(nyBehandling, false);
        behandlingsoppretter.settSomKøet(nyBehandling);
    }

}
