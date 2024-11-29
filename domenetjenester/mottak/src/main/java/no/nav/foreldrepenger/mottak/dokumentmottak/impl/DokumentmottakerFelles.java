package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.temporal.TemporalAdjusters.next;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.task.StartBehandlingTask;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.SøknadUtsettelseUttakDato;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask;
import no.nav.foreldrepenger.skjæringstidspunkt.TomtUttakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@Dependent
public class DokumentmottakerFelles {

    private ProsessTaskTjeneste taskTjeneste;
    private TomtUttakTjeneste tomtUttakTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private Behandlingsoppretter behandlingsoppretter;
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;

    @SuppressWarnings("unused")
    private DokumentmottakerFelles() {
        // For CDI
    }

    @Inject
    public DokumentmottakerFelles(BehandlingRepositoryProvider repositoryProvider,
                                  BehandlingRevurderingTjeneste behandlingRevurderingTjeneste,
                                  ProsessTaskTjeneste taskTjeneste,
                                  BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                  HistorikkinnslagTjeneste historikkinnslagTjeneste,
                                  MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                  Behandlingsoppretter behandlingsoppretter,
                                  TomtUttakTjeneste tomtUttakTjeneste) {
        this.taskTjeneste = taskTjeneste;
        this.tomtUttakTjeneste = tomtUttakTjeneste;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.behandlingsoppretter = behandlingsoppretter;
        this.behandlingRevurderingTjeneste = behandlingRevurderingTjeneste;
    }

    void leggTilBehandlingsårsak(Behandling behandling, BehandlingÅrsakType behandlingÅrsak) {
        behandlingsoppretter.leggTilBehandlingsårsak(behandling, behandlingÅrsak);
    }

    void opprettTaskForÅStarteBehandling(Behandling behandling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(StartBehandlingTask.class);
        prosessTaskData.setBehandling(behandling.getSaksnummer().getVerdi(), behandling.getFagsakId(), behandling.getId());
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }

    public void opprettTaskForÅVurdereDokument(Fagsak fagsak, Behandling behandling, MottattDokument mottattDokument) {
        var behandlendeEnhetsId = hentBehandlendeEnhetTilVurderDokumentOppgave(mottattDokument, fagsak, behandling);
        var prosessTaskData = ProsessTaskData.forProsessTask(OpprettOppgaveVurderDokumentTask.class);
        Optional.ofNullable(mottattDokument.getJournalpostId()).map(JournalpostId::getVerdi)
            .ifPresent(jpid -> prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_JOURNALPOST_ID, jpid));
        prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_BEHANDLENDE_ENHET, behandlendeEnhetsId);
        prosessTaskData.setProperty(OpprettOppgaveVurderDokumentTask.KEY_DOKUMENT_TYPE, mottattDokument.getDokumentType().getKode());
        prosessTaskData.setFagsak(fagsak.getSaksnummer().getVerdi(), fagsak.getId());
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }

    public void opprettKøetHistorikk(Behandling køetBehandling, boolean fantesFraFør) {
        if (!fantesFraFør) {
            opprettHistorikkinnslagForVenteFristRelaterteInnslag(køetBehandling, HistorikkinnslagType.BEH_KØET, null, Venteårsak.VENT_ÅPEN_BEHANDLING);
        }
    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Behandling behandling, HistorikkinnslagType historikkinnslagType, LocalDateTime frist, Venteårsak venteårsak) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForVenteFristRelaterteInnslag(behandling, historikkinnslagType, frist, venteårsak);
    }

    void opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(Behandling behandling, Behandling nyBehandling, MottattDokument mottattDokument) {
        historikkinnslagTjeneste.opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(behandling);
        historikkinnslagTjeneste.opprettHistorikkinnslag(nyBehandling, mottattDokument.getJournalpostId(), true,
            mottattDokument.getElektroniskRegistrert(), DokumentTypeId.INNTEKTSMELDING.equals(mottattDokument.getDokumentType()));
    }

    void opprettHistorikk(Behandling behandling, MottattDokument mottattDokument) {
        var dokType = mottattDokument.getDokumentType();
        if (dokType.erSøknadType() || dokType.erEndringsSøknadType() || DokumentTypeId.KLAGE_DOKUMENT.equals(dokType) ||
            DokumentKategori.SØKNAD.equals(mottattDokument.getDokumentKategori())) {
            historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, mottattDokument.getJournalpostId(), true,
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
        // Prod: Klageinstans + Vikafossen sender dokumenter til scanning med forside som inneholder enhet. Journalføring og Vurder dokument skal til enheten.
        var journalEnhet = dokument.getJournalEnhet().map(e -> behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(sak.getId(), e));
        if (journalEnhet.isPresent()) {
            return journalEnhet.get().enhetId();
        }
        // Midlertidig for håndtering av feil praksis utsettelse
        if (behandling == null) {
            return finnEnhetFraFagsak(sak).enhetId();
        }
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsak().getId())
                .map(this::utledEnhetFraBehandling)
                .orElse(finnEnhetFraFagsak(sak).enhetId());
        }
        return utledEnhetFraBehandling(behandling);
    }

    private String utledEnhetFraBehandling(Behandling behandling) {
        return behandlendeEnhetTjeneste.finnBehandlendeEnhetFra(behandling).enhetId();
    }

    OrganisasjonsEnhet finnEnhetFraFagsak(Fagsak sak) {
        return behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(sak);
    }

    final Behandling opprettRevurdering(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var revurdering = behandlingsoppretter.opprettRevurdering(fagsak, behandlingÅrsakType);
        mottatteDokumentTjeneste.persisterDokumentinnhold(revurdering, mottattDokument, Optional.empty());
        opprettHistorikk(revurdering, mottattDokument);
        opprettTaskForÅStarteBehandling(revurdering);
        return revurdering;
    }

    final Behandling opprettKøetRevurdering(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var revurdering = behandlingsoppretter.opprettRevurdering(fagsak, behandlingÅrsakType);
        mottatteDokumentTjeneste.persisterDokumentinnhold(revurdering, mottattDokument, Optional.empty());
        opprettHistorikk(revurdering, mottattDokument);
        leggNyBehandlingPåKøOgOpprettHistorikkinnslag(revurdering);
        return revurdering;
    }

    final Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, boolean opprettSomKøet) {
        var revurdering = behandlingsoppretter.opprettManuellRevurdering(fagsak, behandlingÅrsakType);
        if (opprettSomKøet) {
            leggNyBehandlingPåKøOgOpprettHistorikkinnslag(revurdering);
        } else {
            opprettTaskForÅStarteBehandling(revurdering);
        }
        return revurdering;
    }

    Behandling oppdatereViaHenleggelse(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsak) {
        var nyBehandling = behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling, behandlingÅrsak);
        opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(behandling, nyBehandling, mottattDokument);
        var søknadsdato = behandlingRevurderingTjeneste.finnSøknadsdatoFraHenlagtBehandling(nyBehandling);
        mottatteDokumentTjeneste.persisterDokumentinnhold(nyBehandling, mottattDokument, søknadsdato);
        return nyBehandling;
    }

    Behandling oppdatereViaHenleggelseEnkø(Behandling behandling, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsak) {
        var nyBehandling = behandlingsoppretter.oppdaterBehandlingViaHenleggelse(behandling, behandlingÅrsak);
        opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(behandling, nyBehandling, mottattDokument);
        var søknadsdato = behandlingRevurderingTjeneste.finnSøknadsdatoFraHenlagtBehandling(nyBehandling);
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

    public Behandling opprettFørstegangsbehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Optional<Behandling> tidligereBehandling) {
        return behandlingsoppretter.opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, tidligereBehandling);
    }

    public Behandling opprettNyFørstegangFraBehandlingMedSøknad(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, Behandling avsluttetBehandling, MottattDokument mottattDokument) {
        var nyBehandling = behandlingsoppretter.opprettNyFørstegangsbehandlingFraTidligereSøknad(fagsak, behandlingÅrsakType, avsluttetBehandling);
        behandlingsoppretter.opprettInntektsmeldingerFraMottatteDokumentPåNyBehandling(nyBehandling);
        historikkinnslagTjeneste.opprettHistorikkinnslag(nyBehandling, mottattDokument.getJournalpostId(), false,
            mottattDokument.getElektroniskRegistrert(), DokumentTypeId.INNTEKTSMELDING.equals(mottattDokument.getDokumentType()));
        opprettTaskForÅStarteBehandling(nyBehandling);
        return nyBehandling;
    }

    void opprettInitiellFørstegangsbehandling(Fagsak fagsak, MottattDokument mottattDokument, BehandlingÅrsakType behandlingÅrsakType) { //#S1
        // Opprett ny førstegangsbehandling
        var behandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, behandlingÅrsakType, Optional.empty());
        persisterDokumentinnhold(behandling, mottattDokument);
        opprettTaskForÅStarteBehandling(behandling);
        opprettHistorikk(behandling, mottattDokument);
    }

    public void opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(MottattDokument mottattDokument, Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType) {
        var forrigeBehandling = finnEvtForrigeBehandling(mottattDokument, fagsak);
        var nyBehandling = behandlingsoppretter.opprettNyFørstegangsbehandlingMedImOgVedleggFraForrige(fagsak, behandlingÅrsakType, forrigeBehandling, !mottattDokument.getDokumentType().erSøknadType());
        persisterDokumentinnhold(nyBehandling, mottattDokument);
        opprettTaskForÅStarteBehandling(nyBehandling);
        opprettHistorikk(nyBehandling, mottattDokument);
    }

    private Behandling finnEvtForrigeBehandling(MottattDokument mottattDokument, Fagsak fagsak) {
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
        if (behandling == null && !mottattDokument.getDokumentType().erSøknadType())
            throw new IllegalStateException("Fant ingen behandling som passet for saksnummer: " + fagsak.getSaksnummer());
        return behandling;
    }

    boolean erBehandlingHenlagt(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).filter(Behandlingsresultat::isBehandlingHenlagt).isPresent();
    }

    void standardForAvslåttEllerOpphørtBehandling(MottattDokument mottattDokument, Fagsak fagsak, Behandling avsluttetBehandling,
                                                  BehandlingÅrsakType behandlingÅrsakType, boolean kanRevurderes) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(fagsak.getYtelseType())) {
            opprettTaskForÅVurdereDokument(fagsak, avsluttetBehandling, mottattDokument);
            return;
        }
        if (skalOppretteNyFørstegangsbehandling(avsluttetBehandling.getFagsak())) { //#I3 #E6
            opprettFørstegangsbehandlingMedHistorikkinslagOgKopiAvDokumenter(mottattDokument, fagsak, behandlingÅrsakType);
        } else if (kanRevurderes && behandlingsoppretter.erOpphørtBehandling(avsluttetBehandling)) { //#I4 #E7 #S5
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

    void persisterDokumentinnhold(Behandling behandling, MottattDokument dokument) {
        mottatteDokumentTjeneste.persisterDokumentinnhold(behandling, dokument, Optional.empty());
    }

    private void leggNyBehandlingPåKøOgOpprettHistorikkinnslag(Behandling nyBehandling) {
        opprettKøetHistorikk(nyBehandling, false);
        behandlingsoppretter.settSomKøet(nyBehandling);
    }

    SøknadUtsettelseUttakDato finnUtsettelseUttak(MottattDokument dokument) {
        return mottatteDokumentTjeneste.finnUtsettelseUttakForSøknad(dokument);
    }

    public boolean endringSomUtsetterStartdato(MottattDokument mottattDokument, Fagsak fagsak) {
        var søknadUtsettelseUttak = finnUtsettelseUttak(mottattDokument);
        var eksisterendeStartdatoOpt = tomtUttakTjeneste.startdatoUttakResultatFrittUttak(fagsak);
        if (søknadUtsettelseUttak == null || søknadUtsettelseUttak.utsettelseFom() == null || eksisterendeStartdatoOpt.isEmpty()) {
            return false;
        }
        var eksisterendeStartdato = helgTilMandag(eksisterendeStartdatoOpt.orElseThrow());
        var utsettelseFraStart = !søknadUtsettelseUttak.utsettelseFom().isAfter(eksisterendeStartdato);
        // Periodene nedenfor bør matches med InntektsmeldingTjeneste . kanInntektsmeldingBrukesForSkjæringstidspunkt()
        var utsettelsePeriodeAkseptert = søknadUtsettelseUttak.uttakFom() != null &&
            (YearMonth.from(søknadUtsettelseUttak.uttakFom()).equals(YearMonth.from(eksisterendeStartdato)) ||
                søknadUtsettelseUttak.uttakFom().isBefore(eksisterendeStartdato.plusWeeks(2)));
        return utsettelseFraStart && !utsettelsePeriodeAkseptert;
    }

    private static LocalDate helgTilMandag(LocalDate dato) {
        return Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(dato.getDayOfWeek()) ? dato.with(next(DayOfWeek.MONDAY)) : dato;
    }

    void opprettAnnulleringsBehandlinger(MottattDokument dokument, Fagsak fagsak) {
        var søknadUtsettelseUttak = finnUtsettelseUttak(dokument);
        // Henlegg alle åpne behandlinger - selv berørte, fordi uttaket skal tømmes
        behandlingRepository.hentÅpneYtelseBehandlingerForFagsakIdForUpdate(fagsak.getId())
            .forEach(b -> behandlingsoppretter.henleggBehandling(b));

        var revurdering = behandlingsoppretter.opprettRevurdering(fagsak, BehandlingÅrsakType.RE_UTSATT_START);
        mottatteDokumentTjeneste.persisterDokumentinnhold(revurdering, dokument, Optional.empty());
        opprettHistorikk(revurdering, dokument);
        opprettTaskForÅStarteBehandling(revurdering);

        // Opprett køet førstegangsbehandling dersom endringssøknad inneholder uttaksperioder
        if (søknadUtsettelseUttak.uttakFom() != null) {
            var nyBehandling = behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
            behandlingsoppretter.kopierAlleGrunnlagFraTidligereBehandlingTilUtsattSøknad(fagsak, revurdering, nyBehandling);
            var søknadKopi = new MottattDokument.Builder(dokument)
                .medBehandlingId(nyBehandling.getId())
                .build();
            mottatteDokumentTjeneste.lagreMottattDokumentPåFagsak(søknadKopi);
            historikkinnslagTjeneste.opprettHistorikkinnslag(nyBehandling, søknadKopi.getJournalpostId(), false,
                søknadKopi.getElektroniskRegistrert(), false);
            leggNyBehandlingPåKøOgOpprettHistorikkinnslag(nyBehandling);
        }
    }

}
