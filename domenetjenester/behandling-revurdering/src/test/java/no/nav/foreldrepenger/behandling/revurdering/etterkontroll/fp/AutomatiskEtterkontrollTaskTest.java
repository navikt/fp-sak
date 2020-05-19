package no.nav.foreldrepenger.behandling.revurdering.etterkontroll.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.task.AutomatiskEtterkontrollTask;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.tjeneste.EtterkontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.person.tps.TpsFamilieTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.TrekkdagerUtregningUtil;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Periode;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.util.Tuple;

@SuppressWarnings("deprecation")
@RunWith(CdiRunner.class)
public class AutomatiskEtterkontrollTaskTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();


    private FpUttakRepository fpUttakRepository = new FpUttakRepository(repoRule.getEntityManager());
    private TpsFamilieTjeneste tpsFamilieTjenesteMock;
    private FamilieHendelseRepository familieHendelseRepositoryMock;


    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private BehandlingRepository behandlingRepositoryMock;

    @Inject
    @FagsakYtelseTypeRef("FP")
    private RevurderingTjeneste revurderingTjenesteMock;

    @Inject
    private ProsessTaskRepository prosessTaskRepositoryMock;

    @Inject
    private EtterkontrollRepository etterkontrollRepository;

    @Inject
    private ForeldrepengerUttakTjeneste foreldrepengerUttakTjeneste;

    private AutomatiskEtterkontrollTask task;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    private HistorikkRepository historikkRepository;
    EtterkontrollTjeneste  etterkontrollTjeneste;

    @Before
    public void setUp() {
        tpsFamilieTjenesteMock = mock(TpsFamilieTjeneste.class);
        behandlendeEnhetTjeneste = mock(BehandlendeEnhetTjeneste.class);
        familieHendelseRepositoryMock =  mock(FamilieHendelseRepository.class);
        var bkTjeneste = mock(BehandlingskontrollTjeneste.class);
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(new OrganisasjonsEnhet("1234", "Testlokasjon"));

        etterkontrollTjeneste = new EtterkontrollTjeneste(repositoryProvider,prosessTaskRepositoryMock, bkTjeneste,
            foreldrepengerUttakTjeneste);
        this.historikkRepository = mock(HistorikkRepository.class);
    }

    private Behandling lagBehandling(AktørId aktørId, RelasjonsRolleType relasjonsRolleType,Saksnummer saksnummer) {
        NavBruker navBruker = new NavBrukerBuilder().medAktørId(aktørId).build();
        repoRule.getEntityManager().persist(navBruker);
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker, relasjonsRolleType,saksnummer );
        final Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        Behandlingsresultat.opprettFor(behandling);
        repoRule.getEntityManager().persist(behandling.getFagsak());
        repoRule.getEntityManager().flush();
        final BehandlingLås lås = repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId());

        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);

        VilkårResultat vilkårResultat = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .buildFor(behandling);
        repositoryProvider.getBehandlingRepository().lagre(vilkårResultat, lås);
        repositoryProvider.getFagsakRelasjonRepository().opprettRelasjon(behandling.getFagsak(), Dekningsgrad._100);
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandling, LocalDate.now().minusYears(1), LocalDate.now(), false);
        return behandling;
    }
    private Arbeidsgiver arbeidsgiver(String arbeidsgiverIdentifikator) {
        return Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator);
    }

    private UttakAktivitetEntitet lagUttakAktivitet(Arbeidsgiver arbeidsgiver) {
        return new UttakAktivitetEntitet.Builder()
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef())
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .build();
    }
    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            UttakAktivitetEntitet uttakAktivitet,
                            LocalDate fom, LocalDate tom,
                            StønadskontoType stønadskontoType) {
        lagPeriode(uttakResultatPerioder, uttakAktivitet, fom, tom, stønadskontoType, false, false);

    }

    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            UttakAktivitetEntitet uttakAktivitet,
                            LocalDate fom, LocalDate tom,
                            StønadskontoType stønadskontoType,
                            boolean samtidigUttak,
                            boolean flerbarnsdager) {
        lagPeriode(uttakResultatPerioder, fom, tom, stønadskontoType, samtidigUttak, flerbarnsdager, new Tuple<>(uttakAktivitet, Optional.empty()));
    }

    @SafeVarargs
    private void lagPeriode(UttakResultatPerioderEntitet uttakResultatPerioder,
                            LocalDate fom,
                            LocalDate tom,
                            StønadskontoType stønadskontoType,
                            boolean samtidigUttak,
                            boolean flerbarnsdager,
                            Tuple<UttakAktivitetEntitet, Optional<Trekkdager>>... aktiviteter) {

        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medSamtidigUttak(samtidigUttak)
            .medFlerbarnsdager(flerbarnsdager)
            .build();
        uttakResultatPerioder.leggTilPeriode(periode);

        for (Tuple<UttakAktivitetEntitet, Optional<Trekkdager>> aktivitetTuple : aktiviteter) {
            Trekkdager trekkdager;
            if (aktivitetTuple.getElement2().isPresent()) {
                trekkdager = aktivitetTuple.getElement2().get();
            } else {
                trekkdager = new Trekkdager(TrekkdagerUtregningUtil.trekkdagerFor(
                    new Periode(periode.getFom(), periode.getTom()),
                    false,
                    BigDecimal.ZERO,
                    null).decimalValue());
            }

            UttakResultatPeriodeAktivitetEntitet aktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, aktivitetTuple.getElement1())
                .medTrekkdager(trekkdager)
                .medTrekkonto(stønadskontoType)
                .medArbeidsprosent(BigDecimal.ZERO)
                .build();
            periode.leggTilAktivitet(aktivitet);

        }
    }

    @Test
    public void skal_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_og_køe_berørt_behandling_dersom_fødsel_mangler_i_tps() {

        LocalDate fødseldato = LocalDate.of(2018, Month.MAY, 1);

        // mock tps. hentFødteBarn  46
           //
        // --- Mors behandling
        //
        Behandling morsBehandling = lagBehandling(AktørId.dummy(), RelasjonsRolleType.MORA,new Saksnummer("66"));
        Arbeidsgiver virksomhetForMor = arbeidsgiver("123");
        UttakAktivitetEntitet uttakAktivitetForMor = lagUttakAktivitet(virksomhetForMor);
        UttakResultatPerioderEntitet uttakResultatPerioderForMor = new UttakResultatPerioderEntitet();

        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.minusWeeks(3), fødseldato.minusDays(1), StønadskontoType.FORELDREPENGER_FØR_FØDSEL);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato, fødseldato.plusWeeks(6).minusDays(1), StønadskontoType.MØDREKVOTE);
        lagPeriode(uttakResultatPerioderForMor, uttakAktivitetForMor, fødseldato.plusWeeks(6), fødseldato.plusWeeks(16).minusDays(1), StønadskontoType.FELLESPERIODE);

        Behandlingsresultat behandlingsresultatForMor = morsBehandling.getBehandlingsresultat();
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatForMor).medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        repoRule.getRepository().lagre(behandlingsresultatForMor);
        BehandlingVedtak behandlingVedtak = BehandlingVedtak
            .builder()
            .medBehandlingsresultat(behandlingsresultatForMor)
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Saksbehadlersen")
            .medVedtakstidspunkt(LocalDateTime.now())
            .build();
        repoRule.getRepository().lagre(behandlingVedtak);

        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(morsBehandling.getId(), uttakResultatPerioderForMor);
        morsBehandling.avsluttBehandling();
        repoRule.getRepository().lagre(morsBehandling);



        //
        // --- Fars behandling
        //
        Behandling farsBehandling = lagBehandling(AktørId.dummy(), RelasjonsRolleType.FARA,new Saksnummer("77"));

        farsBehandling.avsluttBehandling();
        repositoryProvider.getFagsakRelasjonRepository().kobleFagsaker(morsBehandling.getFagsak(), farsBehandling.getFagsak(), morsBehandling);


        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(Collections.emptyList());


        FamilieHendelseBuilder familieHendelseBuilder = repositoryProvider.getFamilieHendelseRepository().opprettBuilderFor(morsBehandling);
        LocalDate termindato = LocalDate.now().plusMonths(4);


        final FamilieHendelseBuilder søknadHendelse = familieHendelseBuilder
            .medAntallBarn(1)
            .medTerminbekreftelse(familieHendelseBuilder.getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medUtstedtDato(LocalDate.now())
                .medNavnPå("Doktor"));

        repositoryProvider.getFamilieHendelseRepository().lagre(morsBehandling, søknadHendelse);

        FamilieHendelseGrunnlagEntitet familieHendelseAggregat = FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty())
            .medSøknadVersjon(familieHendelseBuilder).build();

        final SøknadEntitet søknad = new SøknadEntitet.Builder()
            .medSøknadsdato(LocalDate.now())
            .medMottattDato(LocalDate.now())
            .medElektroniskRegistrert(true)
            .build();
        repositoryProvider.getSøknadRepository().lagreOgFlush(morsBehandling, søknad);

        when(familieHendelseRepositoryMock.hentAggregat(any())).thenReturn(familieHendelseAggregat);

        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(Collections.emptyList());



        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(morsBehandling.getFagsakId(), morsBehandling.getId(), morsBehandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);
        assertRevurdering(morsBehandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
        // TODO: Hva skal skje videre her? Burde vært noen tester og asserts rundt FAR
    }

    private void assertRevurdering(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        Optional<Behandling> revurdering = behandlingRepositoryMock.hentSisteBehandlingAvBehandlingTypeForFagsakId(behandling.getFagsakId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Ingen revurdering").isPresent();
        List<BehandlingÅrsak> behandlingÅrsaker = revurdering.get().getBehandlingÅrsaker();
        assertThat(behandlingÅrsaker).isNotEmpty();
        List<BehandlingÅrsakType> årsaker = behandlingÅrsaker.stream().map(bå -> bå.getBehandlingÅrsakType()).collect(Collectors.toList());
        assertThat(årsaker).contains(behandlingÅrsakType);
    }

    private void assertIngenRevurdering(Behandling behandling) {
        Optional<Behandling> revurdering = behandlingRepositoryMock.hentSisteBehandlingAvBehandlingTypeForFagsakId(behandling.getFagsakId(), BehandlingType.REVURDERING);
        assertThat(revurdering).as("Har revurdering: " + behandling).isNotPresent();
    }

    private void createTask() {

        Period etterkontrollTpsRegistreringPeriode = Period.parse("P11W");


        task = new AutomatiskEtterkontrollTask(repositoryProvider,
            etterkontrollRepository,
            historikkRepository, tpsFamilieTjenesteMock,
            prosessTaskRepositoryMock, etterkontrollTpsRegistreringPeriode, behandlendeEnhetTjeneste,
            etterkontrollTjeneste);
    }

    @Test
    public void skal_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_dersom_fødsel_mangler_i_tps_uten_bekreftet() {
        Behandling behandling = opprettRevurderingsKandidat(4, 1, true, false, false, false);
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(Collections.emptyList());

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
    }

    @Test
    public void skal_ikke_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_dersom_fødsel_mangler_i_tps_med_overstyrt() {
        Behandling behandling = opprettRevurderingsKandidat(4, 1, true, false, true, false);
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(Collections.emptyList());

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertIngenRevurdering(behandling);
    }

    @Test
    public void skal_opprette_revurderingsbehandling_med_årsak_fødsel_mangler_dersom_fødsel_mangler_i_tps_med_overstyrt_termin() {
        Behandling behandling = opprettRevurderingsKandidat(4, 1, true, false, false, true);
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(Collections.emptyList());

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_MANGLER_FØDSEL);
    }

    @Test
    public void skal_opprette_revurderingsbehandling_når_tps_lik_overstyrt_men_mangler_bekreftet() {
        Behandling behandling = opprettRevurderingsKandidat(4, 1, true, false, false, true);
        List<FødtBarnInfo> barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(barn);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
    }

    @Test
    public void skal_opprette_revurderingsbehandling_med_årsak_fødsel_hendelse_dersom_fødsel_i_tps_og_ikke_i_vedtak() {

        Behandling behandling = opprettRevurderingsKandidat(0, 1, true, false, false, false);
        List<FødtBarnInfo> barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(barn);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);
    }

    @Test
    public void skal_opprette_revurderingsbehandling_med_årsak_avvik_når_TPS_returnere_ulikt_antall_barn() {

        List<FødtBarnInfo> barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        Behandling behandling = opprettRevurderingsKandidat(0, 2, true, false, true, false);
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(barn);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN);
    }

    @Test
    public void skal_registrere_fødsler_dersom_de_oppdages_i_tps() {
        List<FødtBarnInfo> barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        Behandling behandling = opprettRevurderingsKandidat(0, 1, true, false, false, false);
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(barn);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertRevurdering(behandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL);

    }

    @Test
    public void skal_ikke_opprette_revurdering_dersom_barn_i_tps_matcher_søknad() {
        List<FødtBarnInfo> barn = Collections.singletonList(byggBaby(LocalDate.now().minusDays(70)));
        Behandling behandling = opprettRevurderingsKandidat(0, 1, true, true, false, false);
        when(tpsFamilieTjenesteMock.getFødslerRelatertTilBehandling(any(), any())).thenReturn(barn);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);

        assertIngenRevurdering(behandling);
    }

    @Test
    public void skal_opprette_vurder_konsekvens_oppgave_hvis_det_finnes_åpen_førstegangs_behandling() {
        Behandling behandling = opprettRevurderingsKandidat(0, 2, false, false, false, false);

        ProsessTaskData prosessTaskData = new ProsessTaskData(AutomatiskEtterkontrollTask.TASKTYPE);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        prosessTaskData.setSekvens("1");

        createTask();
        task.doTask(prosessTaskData);
        assertIngenRevurdering(behandling);
    }

    private Behandling opprettRevurderingsKandidat(int fødselUkerFørTermin, int antallBarn, boolean avsluttet, boolean medBekreftet, boolean medOverstyrtFødsel, boolean medOverstyrtTermin) {
        LocalDate terminDato = LocalDate.now().minusDays(70);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medSøknadDato(terminDato.minusDays(20));

        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medNavnPå("Lege Legesen")
                .medTermindato(terminDato)
                .medUtstedtDato(terminDato.minusDays(40)))
            .medAntallBarn(1);

        if (medBekreftet) {
            scenario.medBekreftetHendelse()
                .medFødselsDato(terminDato)
                .erFødsel()
                .medAntallBarn(antallBarn);
        }

        if (medOverstyrtFødsel) {
            scenario.medOverstyrtHendelse()
                .medFødselsDato(terminDato)
                .erFødsel()
                .medAntallBarn(antallBarn);
        }

        if (medOverstyrtTermin) {
            scenario.medOverstyrtHendelse()
                .medTerminbekreftelse(scenario.medOverstyrtHendelse().getTerminbekreftelseBuilder()
                    .medNavnPå("Lege Legesen")
                    .medTermindato(terminDato)
                    .medUtstedtDato(terminDato.minusDays(40)))
                .medAntallBarn(1);
        }


        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT);
        scenario.medVilkårResultatType(VilkårResultatType.INNVILGET);

        if (avsluttet) {
            scenario.medBehandlingVedtak()
                .medVedtakResultatType(VedtakResultatType.INNVILGET)
                .medVedtakstidspunkt(terminDato.minusWeeks(fødselUkerFørTermin).atStartOfDay())
                .medAnsvarligSaksbehandler("Severin Saksbehandler")
                .build();

            scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        }

        Behandling behandling = scenario.lagre(repositoryProvider);

        if (!avsluttet) {
            return behandling;
        }

        Whitebox.setInternalState(behandling, "status", BehandlingStatus.AVSLUTTET);

        BehandlingLås lås = behandlingRepositoryMock.taSkriveLås(behandling);
        behandlingRepositoryMock.lagre(behandling, lås);

        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandling, LocalDate.now().minusYears(1), LocalDate.now(), false);

        repoRule.getRepository().flushAndClear();

        return repoRule.getEntityManager().find(Behandling.class, behandling.getId());
    }



    private FødtBarnInfo byggBaby(LocalDate fødselsdato) {
        return new FødtBarnInfo.Builder()
            .medFødselsdato(fødselsdato)
            .medIdent(PersonIdent.fra("12345678901"))
            .medNavn("barn")
            .medNavBrukerKjønn(NavBrukerKjønn.MANN)
            .build();
    }

}
