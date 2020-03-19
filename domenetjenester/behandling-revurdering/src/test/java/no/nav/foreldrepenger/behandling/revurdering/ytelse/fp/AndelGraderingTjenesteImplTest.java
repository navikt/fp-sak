package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;

import no.nav.folketrygdloven.kalkulator.gradering.AndelGradering;
import no.nav.folketrygdloven.kalkulator.modell.virksomhet.Arbeidsgiver;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.AktivitetStatus;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;

public class AndelGraderingTjenesteImplTest {

    private UttakRepository uttakRepository = Mockito.mock(UttakRepository.class);
    private YtelsesFordelingRepository ytelsesRepo = Mockito.mock(YtelsesFordelingRepository.class);
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste = Mockito.mock(HentOgLagreBeregningsgrunnlagTjeneste.class);

    private AndelGraderingTjeneste tjeneste = new AndelGraderingTjeneste(uttakRepository, ytelsesRepo, beregningsgrunnlagTjeneste);

    private ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

    @Test
    public void hente_gradering_fra_oppgittfordeling_førstegangsbehandling() {
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.of(2019, 5, 27), LocalDate.of(2019, 5, 31))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        Arbeidsgiver arbeidsgiver1 = Arbeidsgiver.virksomhet("123");
        OppgittPeriodeEntitet gradering1 = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver.virksomhet("123"))
            .medArbeidsprosent(BigDecimal.TEN)
            .medErArbeidstaker(true)
            .medPeriode(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 20))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        OppgittPeriodeEntitet gradering2 = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver.virksomhet("123"))
            .medArbeidsprosent(BigDecimal.valueOf(20))
            .medErArbeidstaker(true)
            .medPeriode(LocalDate.of(2019, 6, 21), LocalDate.of(2019, 6, 25))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        Arbeidsgiver arbeidsgiver2 = Arbeidsgiver.virksomhet("456");
        OppgittPeriodeEntitet gradering3 = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver.virksomhet("456"))
            .medArbeidsprosent(BigDecimal.TEN)
            .medErArbeidstaker(true)
            .medPeriode(LocalDate.of(2019, 6, 26), LocalDate.of(2019, 6, 27))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        OppgittPeriodeEntitet gradering4 = OppgittPeriodeBuilder.ny()
            .medErFrilanser(true)
            .medArbeidsprosent(BigDecimal.TEN)
            .medPeriode(LocalDate.of(2019, 6, 28), LocalDate.of(2019, 6, 28))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        List<OppgittPeriodeEntitet> perioder = List.of(periode, gradering1, gradering2, gradering3, gradering4);
        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(perioder, false);
        Behandling behandling = scenario.lagMocked();
        medFordeling(behandling, fordeling);

        Set<AndelGradering> andelGraderingList = tjeneste.utled(BehandlingReferanse.fra(behandling)).getAndelGradering();

        assertThat(andelGraderingList).hasSize(3);
        List<AndelGradering> andelGraderingArbeidsgiver1 = forArbeidsgiver(andelGraderingList, arbeidsgiver1, AktivitetStatus.ARBEIDSTAKER);
        assertThat(andelGraderingArbeidsgiver1).hasSize(1);
        List<AndelGradering.Gradering> graderingerArbeidsgiver1 = andelGraderingArbeidsgiver1.get(0).getGraderinger();
        assertThat(graderingerArbeidsgiver1).hasSize(2);
        assertThat(graderingerArbeidsgiver1).anySatisfy(gradering -> {
            assertThat(gradering.getPeriode()).isEqualTo(Intervall.fraOgMedTilOgMed(gradering1.getFom(), gradering1.getTom()));
        });
        assertThat(graderingerArbeidsgiver1).anySatisfy(gradering -> {
            assertThat(gradering.getPeriode()).isEqualTo(Intervall.fraOgMedTilOgMed(gradering2.getFom(), gradering2.getTom()));
        });
        assertThat(andelGraderingArbeidsgiver1.get(0).getArbeidsgiver()).isEqualTo(arbeidsgiver1);
        assertThat(graderingerArbeidsgiver1.get(0).getArbeidstidProsent()).isIn(gradering1.getArbeidsprosent(), gradering2.getArbeidsprosent());
        assertThat(graderingerArbeidsgiver1.get(1).getArbeidstidProsent()).isIn(gradering1.getArbeidsprosent(), gradering2.getArbeidsprosent());

        List<AndelGradering> andelGraderingArbeidsgiver2 = forArbeidsgiver(andelGraderingList, arbeidsgiver2, AktivitetStatus.ARBEIDSTAKER);
        assertThat(andelGraderingArbeidsgiver2).hasSize(1);
        List<AndelGradering.Gradering> graderingerArbeidsgiver2 = andelGraderingArbeidsgiver2.get(0).getGraderinger();
        assertThat(graderingerArbeidsgiver2).hasSize(1);
        assertThat(graderingerArbeidsgiver2.get(0).getPeriode()).isEqualTo(Intervall.fraOgMedTilOgMed(gradering3.getFom(), gradering3.getTom()));
        assertThat(andelGraderingArbeidsgiver2.get(0).getArbeidsgiver()).isEqualTo(arbeidsgiver2);
        assertThat(graderingerArbeidsgiver2.get(0).getArbeidstidProsent()).isEqualTo(gradering3.getArbeidsprosent());
        List<AndelGradering> andelGraderingFrilans = forArbeidsgiver(andelGraderingList, null, AktivitetStatus.FRILANSER);
        assertThat(andelGraderingFrilans).hasSize(1);
        List<AndelGradering.Gradering> graderingerFrilans = andelGraderingFrilans.get(0).getGraderinger();
        assertThat(graderingerFrilans).hasSize(1);
        assertThat(graderingerFrilans.get(0).getPeriode()).isEqualTo(Intervall.fraOgMedTilOgMed(gradering4.getFom(), gradering4.getTom()));
        assertThat(graderingerFrilans.get(0).getArbeidstidProsent()).isEqualTo(gradering3.getArbeidsprosent());
        assertThat(andelGraderingFrilans.get(0).getArbeidsgiver()).isNull();
    }

    private void medFordeling(Behandling behandling, OppgittFordelingEntitet fordeling) {
        YtelseFordelingAggregat ytelseFordelingAggregat = Mockito.mock(YtelseFordelingAggregat.class);
        when(ytelseFordelingAggregat.getGjeldendeSøknadsperioder()).thenReturn(fordeling);
        when(ytelsesRepo.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(ytelseFordelingAggregat));

    }

    @Test
    public void hente_gradering_fra_oppgittfordeling_med_uttaksresultat_fra_original_behandling() {
        // Skal hente gradering fra uttak fram til der oppgittfordeling starter
        ScenarioMorSøkerForeldrepenger originalScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("123");
        UttakResultatPeriodeEntitet uttaksperiodeUtenGradering = uttaksperiode(LocalDate.of(2019, 5, 1),
            LocalDate.of(2019, 5, 5));
        UttakResultatPeriodeEntitet uttaksperiodeMedGradering = gradertUttaksperiode(arbeidsgiver,
            UttakArbeidType.ORDINÆRT_ARBEID, LocalDate.of(2019, 5, 10), LocalDate.of(2019, 6, 20));
        leggTilUgradertFrilans(uttaksperiodeMedGradering);
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(uttaksperiodeMedGradering);
        perioder.leggTilPeriode(uttaksperiodeUtenGradering);
        Behandling originalBehandling = originalScenario.lagMocked();
        medUttak(originalBehandling, perioder);

        scenario.medBehandlingType(BehandlingType.REVURDERING);
        scenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        OppgittPeriodeEntitet oppgittPeriodeUtenGradering = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.of(2019, 5, 15), LocalDate.of(2019, 5, 20))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        OppgittPeriodeEntitet oppgittPeriodeMedGradering = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver.virksomhet("123"))
            .medArbeidsprosent(BigDecimal.TEN)
            .medErArbeidstaker(true)
            .medPeriode(LocalDate.of(2019, 5, 21), LocalDate.of(2019, 6, 25))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        List<OppgittPeriodeEntitet> oppgittFordeling = List.of(oppgittPeriodeUtenGradering, oppgittPeriodeMedGradering);
        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(oppgittFordeling, false);
        Behandling behandling = scenario.lagMocked();
        medFordeling(behandling, fordeling);

        Set<AndelGradering> andelGraderingList = tjeneste.utled(BehandlingReferanse.fra(behandling)).getAndelGradering();

        assertThat(andelGraderingList).hasSize(1);
        List<AndelGradering> andelGraderingArbeidsgiver = forArbeidsgiver(andelGraderingList, arbeidsgiver, AktivitetStatus.ARBEIDSTAKER);
        assertThat(andelGraderingArbeidsgiver).hasSize(1);
        List<AndelGradering.Gradering> graderinger = andelGraderingArbeidsgiver.get(0).getGraderinger();
        assertThat(graderinger).hasSize(2);
        // Andre periode kuttes av oppgittfordeling
        assertThat(graderinger).anySatisfy(gradering -> {
            Intervall forventet = Intervall.fraOgMedTilOgMed(uttaksperiodeMedGradering.getFom(),
                oppgittPeriodeUtenGradering.getFom().minusDays(1));
            assertThat(gradering.getPeriode()).isEqualTo(forventet);
        });
        assertThat(graderinger).anySatisfy(gradering -> {
            assertThat(gradering.getPeriode())
                .isEqualTo(Intervall.fraOgMedTilOgMed(oppgittPeriodeMedGradering.getFom(), oppgittPeriodeMedGradering.getTom()));
        });
        assertThat(andelGraderingArbeidsgiver.get(0).getArbeidsgiver()).isEqualTo(arbeidsgiver);
    }

    private void medUttak(Behandling behandling, UttakResultatPerioderEntitet perioder) {
        var uttak = new UttakResultatEntitet.Builder(Mockito.mock(Behandlingsresultat.class))
            .medOpprinneligPerioder(perioder).build();
        when(uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(uttak));
    }

    @Test
    public void hente_gradering_fra_uttaksresultat_uten_oppgittfordeling() {
        ScenarioMorSøkerForeldrepenger originalScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("123");
        UttakResultatPeriodeEntitet gradering = gradertUttaksperiode(arbeidsgiver, UttakArbeidType.ORDINÆRT_ARBEID, LocalDate.of(2019, 5, 27),
            LocalDate.of(2019, 5, 31));
        leggTilUgradertFrilans(gradering);
        UttakResultatPeriodeEntitet periodeUtenGradering = uttaksperiode(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 1));
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(gradering);
        perioder.leggTilPeriode(periodeUtenGradering);
        Behandling originalBehandling = originalScenario.lagMocked();
        medUttak(originalBehandling, perioder);

        scenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        scenario.medBehandlingType(BehandlingType.REVURDERING);
        Behandling behandling = scenario.lagMocked();
        medFordeling(behandling, new OppgittFordelingEntitet(Collections.emptyList(), false));

        Set<AndelGradering> andelGraderingList = tjeneste.utled(BehandlingReferanse.fra(behandling)).getAndelGradering();

        assertThat(andelGraderingList).hasSize(1);
        List<AndelGradering> graderingArbeidsgiver = forArbeidsgiver(andelGraderingList, arbeidsgiver, AktivitetStatus.ARBEIDSTAKER);
        assertThat(graderingArbeidsgiver).hasSize(1);
        List<AndelGradering.Gradering> graderinger = graderingArbeidsgiver.get(0).getGraderinger();
        assertThat(graderinger).hasSize(1);
        assertThat(graderinger.get(0).getPeriode().getFomDato()).isEqualTo(gradering.getFom());
        assertThat(graderinger.get(0).getPeriode().getTomDato()).isEqualTo(gradering.getTom());
        assertThat(graderingArbeidsgiver.get(0).getArbeidsgiver()).isEqualTo(arbeidsgiver);
    }

    @Test
    public void hente_gradering_fra_oppgittfordeling_med_uttaksresultat_fra_original_behandling_frilans() {
        // Skal hente gradering fra uttak fram til der oppgittfordeling starter
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("123");
        ScenarioMorSøkerForeldrepenger originalScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        UttakResultatPeriodeEntitet uttaksperiodeMedGradering = gradertUttaksperiode(null,
            UttakArbeidType.FRILANS, LocalDate.of(2019, 5, 15), LocalDate.of(2019, 5, 20));
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(uttaksperiodeMedGradering);
        Behandling originalBehandling = originalScenario.lagMocked();
        medUttak(originalBehandling, perioder);

        scenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        scenario.medBehandlingType(BehandlingType.REVURDERING);
        OppgittPeriodeEntitet oppgittPeriodeMedGradering = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver.virksomhet("123"))
            .medArbeidsprosent(BigDecimal.TEN)
            .medErFrilanser(true)
            .medPeriode(LocalDate.of(2019, 5, 21), LocalDate.of(2019, 6, 25))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        List<OppgittPeriodeEntitet> oppgittFordeling = List.of(oppgittPeriodeMedGradering);
        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(oppgittFordeling, false);
        Behandling behandling = scenario.lagMocked();
        medFordeling(behandling, fordeling);

        Set<AndelGradering> andelGraderingList = tjeneste.utled(BehandlingReferanse.fra(behandling)).getAndelGradering();

        assertThat(andelGraderingList).hasSize(1);
        List<AndelGradering> andelGraderingArbeidsgiver = forArbeidsgiver(andelGraderingList, null, AktivitetStatus.FRILANSER);
        assertThat(andelGraderingArbeidsgiver).hasSize(1);
        List<AndelGradering.Gradering> graderinger = andelGraderingArbeidsgiver.get(0).getGraderinger();
        assertThat(graderinger).hasSize(2);
        assertThat(graderinger).anySatisfy(gradering -> {
            assertThat(gradering.getPeriode())
                .isEqualTo(Intervall.fraOgMedTilOgMed(uttaksperiodeMedGradering.getFom(), uttaksperiodeMedGradering.getTom()));
        });
        assertThat(graderinger).anySatisfy(gradering -> {
            assertThat(gradering.getPeriode())
                .isEqualTo(Intervall.fraOgMedTilOgMed(oppgittPeriodeMedGradering.getFom(), oppgittPeriodeMedGradering.getTom()));
        });
    }

    @Test
    public void hente_gradering_fra_oppgittfordeling_med_uttaksresultat_fra_original_behandling_sn() {
        // Skal hente gradering fra uttak fram til der oppgittfordeling starter
        ScenarioMorSøkerForeldrepenger originalScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("123");
        UttakResultatPeriodeEntitet uttaksperiodeMedGradering = gradertUttaksperiode(null,
            UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, LocalDate.of(2019, 5, 15), LocalDate.of(2019, 5, 20));
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(uttaksperiodeMedGradering);
        Behandling originalBehandling = originalScenario.lagMocked();
        medUttak(originalBehandling, perioder);

        scenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        scenario.medBehandlingType(BehandlingType.REVURDERING);
        OppgittPeriodeEntitet oppgittPeriodeMedGradering = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver.virksomhet("123"))
            .medArbeidsprosent(BigDecimal.TEN)
            .medErSelvstendig(true)
            .medPeriode(LocalDate.of(2019, 5, 21), LocalDate.of(2019, 6, 25))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        List<OppgittPeriodeEntitet> oppgittFordeling = List.of(oppgittPeriodeMedGradering);
        OppgittFordelingEntitet fordeling = new OppgittFordelingEntitet(oppgittFordeling, false);
        Behandling behandling = scenario.lagMocked();
        medFordeling(behandling, fordeling);

        Set<AndelGradering> andelGraderingList = tjeneste.utled(BehandlingReferanse.fra(behandling)).getAndelGradering();

        assertThat(andelGraderingList).hasSize(1);
        List<AndelGradering> andelGraderingArbeidsgiver = forArbeidsgiver(andelGraderingList, null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(andelGraderingArbeidsgiver).hasSize(1);
        List<AndelGradering.Gradering> graderinger = andelGraderingArbeidsgiver.get(0).getGraderinger();
        assertThat(graderinger).hasSize(2);
        assertThat(graderinger).anySatisfy(gradering -> {
            assertThat(gradering.getPeriode())
                .isEqualTo(Intervall.fraOgMedTilOgMed(uttaksperiodeMedGradering.getFom(), uttaksperiodeMedGradering.getTom()));
        });
        assertThat(graderinger).anySatisfy(gradering -> {
            assertThat(gradering.getPeriode())
                .isEqualTo(Intervall.fraOgMedTilOgMed(oppgittPeriodeMedGradering.getFom(), oppgittPeriodeMedGradering.getTom()));
        });
    }


    private UttakResultatPeriodeEntitet gradertUttaksperiode(Arbeidsgiver arbeidsgiver, UttakArbeidType uttakArbeidType, LocalDate fom, LocalDate tom) {
        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medGraderingInnvilget(true)
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.UTTAK_OPPFYLT)
            .build();
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(uttakArbeidType)
            .medArbeidsforhold(no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver.virksomhet("123"), null)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN)
            .medErSøktGradering(true)
            .build();
        return periode;
    }

    private void leggTilUgradertFrilans(UttakResultatPeriodeEntitet periode) {
        UttakAktivitetEntitet frilans = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.FRILANS)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode, frilans)
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medErSøktGradering(false)
            .build();
    }

    private UttakResultatPeriodeEntitet uttaksperiode(LocalDate fom, LocalDate tom) {
        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medGraderingInnvilget(true)
            .medPeriodeResultat(PeriodeResultatType.INNVILGET, InnvilgetÅrsak.UTTAK_OPPFYLT)
            .build();
        UttakAktivitetEntitet uttakAktivitet = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)
            .build();
        new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivitet)
            .medTrekkonto(StønadskontoType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN)
            .medErSøktGradering(false)
            .build();
        leggTilUgradertFrilans(periode);
        return periode;
    }

    private List<AndelGradering> forArbeidsgiver(Collection<AndelGradering> andelGraderingList, Arbeidsgiver arbeidsgiver, AktivitetStatus status) {
        return andelGraderingList.stream()
            .filter(ag -> Objects.equals(arbeidsgiver, ag.getArbeidsgiver()) && ag.getAktivitetStatus().equals(status))
            .collect(Collectors.toList());
    }

}
