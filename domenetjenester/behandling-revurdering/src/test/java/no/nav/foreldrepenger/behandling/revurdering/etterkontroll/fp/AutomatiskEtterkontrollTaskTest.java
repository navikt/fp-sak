package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.TrekkdagerUtregningUtil;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Periode;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class AutomatiskEtterkontrollTaskTest {

    @Mock
    private PersoninfoAdapter personinfoAdapter;

    @Mock
    private ProsessTaskTjeneste taskTjenesteMock;

    private BehandlingRepositoryProvider repositoryProvider;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    private EtterkontrollRepository etterkontrollRepository;

    private AutomatiskEtterkontrollTask task;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    private HistorikkRepository historikkRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private EntityManager entityManager;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        this.entityManager = entityManager;
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        etterkontrollRepository = new EtterkontrollRepository(entityManager);
        behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        lenient().when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class)))
            .thenReturn(new OrganisasjonsEnhet("1234", "Testlokasjon"));

        this.historikkRepository = mock(HistorikkRepository.class);
        this.fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
    }

    private Behandling lagBehandling(AktørId aktørId, RelasjonsRolleType relasjonsRolleType, Saksnummer saksnummer) {
        var navBruker = new NavBrukerBuilder().medAktørId(aktørId).build();
        entityManager.persist(navBruker);
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, relasjonsRolleType, saksnummer);
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        Behandlingsresultat.opprettFor(behandling);
        entityManager.persist(behandling.getFagsak());
        entityManager.flush();
        var lås = repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId());

        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .buildFor(behandling);
        repositoryProvider.getBehandlingRepository().lagre(vilkårResultat, lås);
        fagsakRelasjonTjeneste.opprettRelasjon(behandling.getFagsak());
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandling, LocalDate.now().minusYears(1), LocalDate.now(), false);
        return behandling;
    }

    private Arbeidsgiver arbeidsgiver(String arbeidsgiverIdentifikator) {
        return Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator);
    }

    private UttakAktivitetEntitet lagUttakAktivitet(Arbeidsgiver arbeidsgiver) {
        return new UttakAktivitetEntitet.Builder().medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
    }

    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            UttakAktivitetEntitet uttakAktivitet,
                            LocalDate fom,
                            LocalDate tom,
                            UttakPeriodeType stønadskontoType) {
        lagPeriode(uttakResultatPerioder, uttakAktivitet, fom, tom, stønadskontoType, false, false);

    }

    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            UttakAktivitetEntitet uttakAktivitet,
                            LocalDate fom,
                            LocalDate tom,
                            UttakPeriodeType stønadskontoType,
                            boolean samtidigUttak,
                            boolean flerbarnsdager) {
        lagPeriode(uttakResultatPerioder, fom, tom, stønadskontoType, samtidigUttak, flerbarnsdager, uttakAktivitet, Optional.empty());
    }

    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            LocalDate fom,
                            LocalDate tom,
                            UttakPeriodeType stønadskontoType,
                            boolean samtidigUttak,
                            boolean flerbarnsdager,
                            UttakAktivitetEntitet uttakAktivitetEntitet,
                            Optional<Trekkdager> trekkdagerOptional) {

        var periode = new UttakResultatPeriodeEntitet.Builder(fom, tom).medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medSamtidigUttak(samtidigUttak)
            .medFlerbarnsdager(flerbarnsdager)
            .build();
        uttakResultatPerioder.leggTilPeriode(periode);

        var trekkdager = trekkdagerOptional.orElseGet(() -> new Trekkdager(
            TrekkdagerUtregningUtil.trekkdagerFor(new Periode(periode.getFom(), periode.getTom()), false, BigDecimal.ZERO, null).decimalValue()));

        var aktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitetEntitet).medTrekkdager(trekkdager)
            .medTrekkonto(stønadskontoType)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();
        periode.leggTilAktivitet(aktivitet);
    }

    @Test
    void skal_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_og_køe_etterkontroll_dersom_fødsel_mangler_i_pdl() {

        var fødseldato = LocalDate.now().minusDays(70);

        // --- Mors behandling
        var morsBehandling = lagBehandling(AktørId.dummy(), RelasjonsRolleType.MORA, new Saksnummer("69996"));
        var virksomhetForMor = arbeidsgiver("123");
        var uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        var uttakResultatPerioderForMor = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), UttakPeriodeType.MØDREKVOTE);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1),
            UttakPeriodeType.FELLESPERIODE);

        var behandlingsresultatForMor = morsBehandling.getBehandlingsresultat();
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        entityManager.persist(behandlingsresultatForMor);
        var behandlingVedtak = BehandlingVedtak.builder()
            .medBehandlingsresultat(behandlingsresultatForMor)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Saksbehadlersen")
            .medVedtakstidspunkt(LocalDateTime.now())
            .build();
        entityManager.persist(behandlingVedtak);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository()
            .lagre(morsBehandling, repositoryProvider.getBehandlingLåsRepository().taLås(morsBehandling.getId()));
        entityManager.flush();
        entityManager.clear();

        // --- Fars behandling
        var farsBehandling = lagBehandling(AktørId.dummy(), RelasjonsRolleType.FARA, new Saksnummer("7799999"));

        fagsakRelasjonTjeneste.kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak());
        var virksomhetForFar = arbeidsgiver("456");
        var uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        var uttakResultatPerioderForFar = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForFar, uttakAktivitetForFar, fødseldato.plusWeeks(16), fødseldato.plusWeeks(31).minusDays(1),
            UttakPeriodeType.FEDREKVOTE);

        var behandlingsresultatForFar = farsBehandling.getBehandlingsresultat();
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForFar).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        entityManager.persist(behandlingsresultatForFar);
        var behandlingVedtakFar = BehandlingVedtak.builder()
            .medBehandlingsresultat(behandlingsresultatForFar)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Saksbehadlersen")
            .medVedtakstidspunkt(LocalDateTime.now())
            .build();
        entityManager.persist(behandlingVedtakFar);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(farsBehandling.getId(), uttakResultatPerioderForFar);

        farsBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository()
            .lagre(farsBehandling, repositoryProvider.getBehandlingLåsRepository().taLås(farsBehandling.getId()));
        entityManager.flush();
        entityManager.clear();

        when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), any(), any())).thenReturn(Collections.emptyList());

        var termindato = LocalDate.now().minusDays(70);

        var familieHendelseBuilder = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(morsBehandling.getId());
        var søknadHendelse = familieHendelseBuilder.medAntallBarn(1)
            .medTerminbekreftelse(
                familieHendelseBuilder.getTerminbekreftelseBuilder().medTermindato(termindato).medUtstedtDato(LocalDate.now()).medNavnPå("Doktor"));
        repositoryProvider.getFamilieHendelseRepository().lagre(morsBehandling.getId(), søknadHendelse);

        var familieHendelseBuilderFar = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(farsBehandling.getId());
        var søknadHendelseFar = familieHendelseBuilderFar.medAntallBarn(1)
            .medTerminbekreftelse(familieHendelseBuilderFar.getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor"));
        repositoryProvider.getFamilieHendelseRepository().lagre(farsBehandling.getId(), søknadHendelseFar);

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(morsBehandling.getFagsakId(), morsBehandling.getId(), morsBehandling.getAktørId().getId());

        createTask();
        task.doTask(prosessTaskData);
        assertRevurdering(morsBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);

        var prosessTaskDataFar = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskDataFar.setBehandling(farsBehandling.getFagsakId(), farsBehandling.getId(), farsBehandling.getAktørId().getId());

        createTask();
        task.doTask(prosessTaskDataFar);
        assertKøetRevurdering(farsBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
    }

    @Test
    void skal_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_og_starte_behandling_dersom_bare_far_skal_etterkontrolleres() {

        var fødseldato = LocalDate.now().minusDays(70);

        // --- Mors behandling
        var morsBehandling = lagBehandling(AktørId.dummy(), RelasjonsRolleType.MORA, new Saksnummer("69996"));
        var virksomhetForMor = arbeidsgiver("123");
        var uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        var uttakResultatPerioderForMor = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1),
            UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), UttakPeriodeType.MØDREKVOTE);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1),
            UttakPeriodeType.FELLESPERIODE);

        var behandlingsresultatForMor = morsBehandling.getBehandlingsresultat();
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        entityManager.persist(behandlingsresultatForMor);
        var behandlingVedtak = BehandlingVedtak.builder()
            .medBehandlingsresultat(behandlingsresultatForMor)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Saksbehadlersen")
            .medVedtakstidspunkt(LocalDateTime.now())
            .build();
        entityManager.persist(behandlingVedtak);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository()
            .lagre(morsBehandling, repositoryProvider.getBehandlingLåsRepository().taLås(morsBehandling.getId()));
        entityManager.flush();
        entityManager.clear();

        // --- Fars behandling
        var farsBehandling = lagBehandling(AktørId.dummy(), RelasjonsRolleType.FARA, new Saksnummer("7799999"));

        fagsakRelasjonTjeneste.kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak());
        var virksomhetForFar = arbeidsgiver("456");
        var uttakAktivitetForFar = lagUttakAktivitet(virksomhetForFar);
        var uttakResultatPerioderForFar = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForFar, uttakAktivitetForFar, fødseldato.plusWeeks(16), fødseldato.plusWeeks(31).minusDays(1),
            UttakPeriodeType.FEDREKVOTE);

        var behandlingsresultatForFar = farsBehandling.getBehandlingsresultat();
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForFar).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        entityManager.persist(behandlingsresultatForFar);
        var behandlingVedtakFar = BehandlingVedtak.builder()
            .medBehandlingsresultat(behandlingsresultatForFar)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Saksbehadlersen")
            .medVedtakstidspunkt(LocalDateTime.now())
            .build();
        entityManager.persist(behandlingVedtakFar);

        repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(farsBehandling.getId(), uttakResultatPerioderForFar);

        farsBehandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository()
            .lagre(farsBehandling, repositoryProvider.getBehandlingLåsRepository().taLås(farsBehandling.getId()));
        entityManager.flush();
        entityManager.clear();

        when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), any(), any())).thenReturn(Collections.emptyList());

        var termindato = LocalDate.now().minusDays(70);

        var familieHendelseBuilder = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(morsBehandling.getId());
        var søknadHendelse = familieHendelseBuilder.medAntallBarn(1)
            .medTerminbekreftelse(
                familieHendelseBuilder.getTerminbekreftelseBuilder().medTermindato(termindato).medUtstedtDato(LocalDate.now()).medNavnPå("Doktor"));
        repositoryProvider.getFamilieHendelseRepository().lagre(morsBehandling.getId(), søknadHendelse);
        var familieHendelseBuilderFødsel = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(morsBehandling.getId());
        var bekreftetHendelse = familieHendelseBuilderFødsel.medAntallBarn(1).medFødselsDato(termindato);
        repositoryProvider.getFamilieHendelseRepository().lagreRegisterHendelse(morsBehandling.getId(), bekreftetHendelse);

        var familieHendelseBuilderFar = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(farsBehandling.getId());
        var søknadHendelseFar = familieHendelseBuilderFar.medAntallBarn(1)
            .medTerminbekreftelse(familieHendelseBuilderFar.getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor"));
        repositoryProvider.getFamilieHendelseRepository().lagre(farsBehandling.getId(), søknadHendelseFar);

        var prosessTaskDataFar = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskDataFar.setBehandling(farsBehandling.getFagsakId(), farsBehandling.getId(), farsBehandling.getAktørId().getId());

        createTask();
        task.doTask(prosessTaskDataFar);
        assertRevurdering(farsBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
    }

    private void assertRevurdering(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        var revurdering = repositoryProvider.getBehandlingRepository()
            .hentSisteBehandlingAvBehandlingTypeForFagsakId(behandling.getFagsakId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Ingen revurdering").isPresent();
        var behandlingÅrsaker = revurdering.get().getBehandlingÅrsaker();
        assertThat(behandlingÅrsaker).isNotEmpty();
        var årsaker = behandlingÅrsaker.stream().map(BehandlingÅrsak::getBehandlingÅrsakType).toList();
        assertThat(årsaker).contains(behandlingÅrsakType);
        assertThat(revurdering.get().isBehandlingPåVent()).isFalse();
    }

    private void assertKøetRevurdering(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        var revurdering = repositoryProvider.getBehandlingRepository()
            .hentSisteBehandlingAvBehandlingTypeForFagsakId(behandling.getFagsakId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Ingen revurdering").isPresent();
        var behandlingÅrsaker = revurdering.get().getBehandlingÅrsaker();
        assertThat(behandlingÅrsaker).isNotEmpty();
        var årsaker = behandlingÅrsaker.stream().map(BehandlingÅrsak::getBehandlingÅrsakType).toList();
        assertThat(årsaker).contains(behandlingÅrsakType);
        assertThat(revurdering.get().isBehandlingPåVent()).isTrue();
        assertThat(revurdering.get().getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING)).isPresent();
    }

    private void assertIngenRevurdering(Behandling behandling) {
        var revurdering = repositoryProvider.getBehandlingRepository()
            .hentSisteBehandlingAvBehandlingTypeForFagsakId(behandling.getFagsakId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Har revurdering: " + behandling).isNotPresent();
    }

    private void createTask() {
        task = new AutomatiskEtterkontrollTask(repositoryProvider, etterkontrollRepository, historikkRepository, familieHendelseTjeneste,
            personinfoAdapter, taskTjenesteMock, behandlendeEnhetTjeneste);
    }

    @Test
    void skal_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_dersom_fødsel_mangler_i_pdl_uten_bekreftet() {
        var behandling = opprettRevurderingsKandidat(4, 1, true, false, false, false);
        when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), any(), any())).thenReturn(Collections.emptyList());

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
    }

    @Test
    void skal_ikke_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_dersom_fødsel_mangler_i_pdl_med_overstyrt() {
        var behandling = opprettRevurderingsKandidat(4, 1, true, false, true, false);
        when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), any(), any())).thenReturn(Collections.emptyList());

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        createTask();
        task.doTask(prosessTaskData);

        assertIngenRevurdering(behandling);
    }

    @Test
    void skal_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_dersom_fødsel_mangler_i_pdl_med_overstyrt_termin() {
        var behandling = opprettRevurderingsKandidat(4, 1, true, false, false, true);
        when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), any(), any())).thenReturn(Collections.emptyList());

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
    }

    @Test
    void skal_opprette_revurderingsbehandling_når_pdl_lik_overstyrt_men_mangler_bekreftet() {
        var behandling = opprettRevurderingsKandidat(4, 1, true, false, false, true);
        var barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), any(), any())).thenReturn(barn);

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
    }

    @Test
    void skal_opprette_revurderingsbehandling_med_årsak_fødsel_hendelse_dersom_fødsel_i_pdl_og_ikke_i_vedtak() {

        var behandling = opprettRevurderingsKandidat(0, 1, true, false, false, false);
        var barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), any(), any())).thenReturn(barn);

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
    }

    @Test
    void skal_opprette_revurderingsbehandling_med_årsak_avvik_når_pdl_returnere_ulikt_antall_barn() {

        var barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        var behandling = opprettRevurderingsKandidat(0, 2, true, false, true, false);
        when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), any(), any())).thenReturn(barn);

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);
    }

    @Test
    void skal_registrere_fødsler_dersom_de_oppdages_i_pdl() {
        var barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        var behandling = opprettRevurderingsKandidat(0, 1, true, false, false, false);
        when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), any(), any())).thenReturn(barn);

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);

    }

    @Test
    void skal_ikke_opprette_revurdering_dersom_barn_i_pdl_matcher_søknad() {
        var barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        var behandling = opprettRevurderingsKandidat(0, 1, true, true, false, false);
        when(personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(any(), any(), any())).thenReturn(barn);

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        createTask();
        task.doTask(prosessTaskData);

        assertIngenRevurdering(behandling);
    }

    @Test
    void skal_opprette_vurder_konsekvens_oppgave_hvis_det_finnes_åpen_førstegangs_behandling() {
        var behandling = opprettRevurderingsKandidat(0, 2, false, false, false, false);

        var prosessTaskData = ProsessTaskData.forProsessTask(AutomatiskEtterkontrollTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        createTask();
        task.doTask(prosessTaskData);
        assertIngenRevurdering(behandling);
    }

    private Behandling opprettRevurderingsKandidat(int fødselUkerFørTermin,
                                                   int antallBarn,
                                                   boolean avsluttet,
                                                   boolean medBekreftet,
                                                   boolean medOverstyrtFødsel,
                                                   boolean medOverstyrtTermin) {
        var terminDato = LocalDate.now().minusDays(70);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medSøknadDato(terminDato.minusDays(20));

        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(terminDato)
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);

        if (medBekreftet) {
            scenario.medBekreftetHendelse().tilbakestillBarn().medFødselsDato(terminDato, antallBarn).erFødsel().medAntallBarn(antallBarn);
        }

        if (medOverstyrtFødsel) {
            scenario.medOverstyrtHendelse().tilbakestillBarn().medFødselsDato(terminDato, antallBarn).erFødsel().medAntallBarn(antallBarn);
        }

        if (medOverstyrtTermin) {
            scenario.medOverstyrtHendelse()
                .medTerminbekreftelse(scenario.medOverstyrtHendelse()
                    .getTerminbekreftelseBuilder()
                    .medNavnPå("Lege Legesen")
                    .medTermindato(terminDato)
                    .medUtstedtDato(terminDato.minusDays(40)))
                .medAntallBarn(1);
        }

        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT);

        if (avsluttet) {
            scenario.medBehandlingVedtak()
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medVedtakstidspunkt(terminDato.minusWeeks(fødselUkerFørTermin).atStartOfDay())
                .medAnsvarligSaksbehandler("Severin Saksbehandler")
                .build();

            scenario.medBehandlingsresultat(
                Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        }

        var behandling = scenario.lagre(repositoryProvider);

        if (!avsluttet) {
            return behandling;
        }

        behandling.avsluttBehandling();

        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);

        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandling, LocalDate.now().minusYears(1), LocalDate.now(), false);

        entityManager.flush();
        entityManager.clear();
        return entityManager.find(Behandling.class, behandling.getId());
    }

    private FødtBarnInfo byggBaby(LocalDate fødselsdato) {
        return new FødtBarnInfo.Builder().medFødselsdato(fødselsdato).medIdent(PersonIdent.fra("12345678901")).build();
    }

}
