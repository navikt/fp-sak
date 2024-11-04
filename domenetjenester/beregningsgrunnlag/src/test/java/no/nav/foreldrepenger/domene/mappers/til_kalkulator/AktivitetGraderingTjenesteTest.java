package no.nav.foreldrepenger.domene.mappers.til_kalkulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.folketrygdloven.kalkulator.modell.gradering.AndelGradering;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.prosess.BeregningGraderingTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriodeAktivitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;

class AktivitetGraderingTjenesteTest {
    private ForeldrepengerUttakTjeneste uttakTjeneste = Mockito.mock(ForeldrepengerUttakTjeneste.class);
    private YtelsesFordelingRepository ytelsesRepo = Mockito.mock(YtelsesFordelingRepository.class);
    private BeregningGraderingTjeneste beregningGraderingTjeneste = new BeregningGraderingTjeneste(uttakTjeneste, ytelsesRepo);
    private AktivitetGraderingTjeneste tjeneste = new AktivitetGraderingTjeneste(beregningGraderingTjeneste);

    private ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

    @Test
    void hente_gradering_fra_oppgittfordeling_førstegangsbehandling() {
        var periode = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.of(2019, 5, 27), LocalDate.of(2019, 5, 31))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var arbeidsgiver1 = Arbeidsgiver.virksomhet("123");
        var gradering1 = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medArbeidsprosent(BigDecimal.TEN)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medPeriode(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 20))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var gradering2 = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medArbeidsprosent(BigDecimal.valueOf(20))
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medPeriode(LocalDate.of(2019, 6, 21), LocalDate.of(2019, 6, 25))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var arbeidsgiver2 = Arbeidsgiver.virksomhet("456");
        var gradering3 = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("456"))
            .medArbeidsprosent(BigDecimal.TEN)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medPeriode(LocalDate.of(2019, 6, 26), LocalDate.of(2019, 6, 27))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var gradering4 = OppgittPeriodeBuilder.ny()
            .medGraderingAktivitetType(GraderingAktivitetType.FRILANS)
            .medArbeidsprosent(BigDecimal.TEN)
            .medPeriode(LocalDate.of(2019, 6, 28), LocalDate.of(2019, 6, 28))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        var perioder = List.of(periode, gradering1, gradering2, gradering3, gradering4);
        var fordeling = new OppgittFordelingEntitet(perioder, false);
        var behandling = scenario.lagMocked();
        medFordeling(behandling, fordeling);

        var andelGraderingList = tjeneste.finnAktivitetGraderingerKalkulus(BehandlingReferanse.fra(behandling)).getAndelGradering();

        assertThat(andelGraderingList).hasSize(3);
        var andelGraderingArbeidsgiver1 = forArbeidsgiver(andelGraderingList, arbeidsgiver1, AktivitetStatus.ARBEIDSTAKER);
        assertThat(andelGraderingArbeidsgiver1).hasSize(1);
        var graderingerArbeidsgiver1 = andelGraderingArbeidsgiver1.get(0).getGraderinger();
        assertThat(graderingerArbeidsgiver1).hasSize(2);
        assertThat(graderingerArbeidsgiver1).anySatisfy(gradering -> assertThat(gradering.getPeriode()).isEqualTo(
            Intervall.fraOgMedTilOgMed(gradering1.getFom(), gradering1.getTom())));
        assertThat(graderingerArbeidsgiver1).anySatisfy(gradering -> assertThat(gradering.getPeriode()).isEqualTo(Intervall.fraOgMedTilOgMed(gradering2.getFom(), gradering2.getTom())));
        assertThat(andelGraderingArbeidsgiver1.get(0).getArbeidsgiver().getIdentifikator()).isEqualTo(arbeidsgiver1.getIdentifikator());
        assertThat(graderingerArbeidsgiver1.get(0).getArbeidstidProsent().verdi()).isIn(gradering1.getArbeidsprosent(), gradering2.getArbeidsprosent());
        assertThat(graderingerArbeidsgiver1.get(1).getArbeidstidProsent().verdi()).isIn(gradering1.getArbeidsprosent(), gradering2.getArbeidsprosent());

        var andelGraderingArbeidsgiver2 = forArbeidsgiver(andelGraderingList, arbeidsgiver2, AktivitetStatus.ARBEIDSTAKER);
        assertThat(andelGraderingArbeidsgiver2).hasSize(1);
        var graderingerArbeidsgiver2 = andelGraderingArbeidsgiver2.get(0).getGraderinger();
        assertThat(graderingerArbeidsgiver2).hasSize(1);
        assertThat(graderingerArbeidsgiver2.get(0).getPeriode()).isEqualTo(Intervall.fraOgMedTilOgMed(gradering3.getFom(), gradering3.getTom()));
        assertThat(andelGraderingArbeidsgiver2.get(0).getArbeidsgiver().getIdentifikator()).isEqualTo(arbeidsgiver2.getIdentifikator());
        assertThat(graderingerArbeidsgiver2.get(0).getArbeidstidProsent().verdi()).isEqualTo(gradering3.getArbeidsprosent());
        var andelGraderingFrilans = forArbeidsgiver(andelGraderingList, null, AktivitetStatus.FRILANSER);
        assertThat(andelGraderingFrilans).hasSize(1);
        var graderingerFrilans = andelGraderingFrilans.get(0).getGraderinger();
        assertThat(graderingerFrilans).hasSize(1);
        assertThat(graderingerFrilans.get(0).getPeriode()).isEqualTo(Intervall.fraOgMedTilOgMed(gradering4.getFom(), gradering4.getTom()));
        assertThat(graderingerFrilans.get(0).getArbeidstidProsent().verdi()).isEqualTo(gradering3.getArbeidsprosent());
        assertThat(andelGraderingFrilans.get(0).getArbeidsgiver()).isNull();
    }

    private void medFordeling(Behandling behandling, OppgittFordelingEntitet fordeling) {
        var ytelseFordelingAggregat = Mockito.mock(YtelseFordelingAggregat.class);
        when(ytelseFordelingAggregat.getGjeldendeFordeling()).thenReturn(fordeling);
        when(ytelsesRepo.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(ytelseFordelingAggregat));

    }

    @Test
    void hente_gradering_fra_oppgittfordeling_med_uttaksresultat_fra_original_behandling() {
        // Skal hente gradering fra uttak fram til der oppgittfordeling starter
        var originalScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var uttaksperiodeUtenGradering = uttaksperiode(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 5, 5));

        var aktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder().medTrekkonto(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN)
            .medSøktGraderingForAktivitetIPeriode(true)
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, arbeidsgiver, null))
            .build();
        var uttaksperiodeMedGradering = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(LocalDate.of(2019, 5, 10), LocalDate.of(2019, 6, 20))
            .medGraderingInnvilget(true)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER)
            .medAktiviteter(List.of(aktivitet, ugradertFrilansAktivitet()))
            .build();

        var originalBehandling = originalScenario.lagMocked();
        medUttak(originalBehandling, List.of(uttaksperiodeMedGradering, uttaksperiodeUtenGradering));

        scenario.medBehandlingType(BehandlingType.REVURDERING);
        scenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
        var oppgittPeriodeUtenGradering = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.of(2019, 5, 15), LocalDate.of(2019, 5, 20))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
        var oppgittPeriodeMedGradering = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medArbeidsprosent(BigDecimal.TEN)
            .medGraderingAktivitetType(GraderingAktivitetType.ARBEID)
            .medPeriode(LocalDate.of(2019, 5, 21), LocalDate.of(2019, 6, 25))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        var oppgittFordeling = List.of(oppgittPeriodeUtenGradering, oppgittPeriodeMedGradering);
        var fordeling = new OppgittFordelingEntitet(oppgittFordeling, false);
        var behandling = scenario.lagMocked();
        medFordeling(behandling, fordeling);

        var andelGraderingList = tjeneste.finnAktivitetGraderingerKalkulus(BehandlingReferanse.fra(behandling)).getAndelGradering();

        assertThat(andelGraderingList).hasSize(1);
        var andelGraderingArbeidsgiver = forArbeidsgiver(andelGraderingList, arbeidsgiver, AktivitetStatus.ARBEIDSTAKER);
        assertThat(andelGraderingArbeidsgiver).hasSize(1);
        var graderinger = andelGraderingArbeidsgiver.get(0).getGraderinger();
        assertThat(graderinger).hasSize(2);
        // Andre periode kuttes av oppgittfordeling
        assertThat(graderinger).anySatisfy(gradering -> {
            var forventet = Intervall.fraOgMedTilOgMed(uttaksperiodeMedGradering.getFom(), oppgittPeriodeUtenGradering.getFom().minusDays(1));
            assertThat(gradering.getPeriode()).isEqualTo(forventet);
        });
        assertThat(graderinger).anySatisfy(gradering -> assertThat(gradering.getPeriode()).isEqualTo(
            Intervall.fraOgMedTilOgMed(oppgittPeriodeMedGradering.getFom(), oppgittPeriodeMedGradering.getTom())));
        assertThat(andelGraderingArbeidsgiver.get(0).getArbeidsgiver().getIdentifikator()).isEqualTo(arbeidsgiver.getIdentifikator());
    }

    private void medUttak(Behandling behandling, List<ForeldrepengerUttakPeriode> perioder) {
        var uttak = new ForeldrepengerUttak(perioder);
        when(uttakTjeneste.hentUttakHvisEksisterer(behandling.getId())).thenReturn(Optional.of(uttak));
    }

    @Test
    void hente_gradering_fra_uttaksresultat_uten_oppgittfordeling() {
        var originalScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");
        var aktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder().medTrekkonto(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN)
            .medSøktGraderingForAktivitetIPeriode(true)
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.ORDINÆRT_ARBEID, arbeidsgiver, null))
            .build();
        var gradering = new ForeldrepengerUttakPeriode.Builder().medTidsperiode(LocalDate.of(2019, 5, 27), LocalDate.of(2019, 5, 31))
            .medGraderingInnvilget(true)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER)
            .medAktiviteter(List.of(aktivitet, ugradertFrilansAktivitet()))
            .build();
        var periodeUtenGradering = uttaksperiode(LocalDate.of(2019, 6, 1), LocalDate.of(2019, 6, 1));

        var originalBehandling = originalScenario.lagMocked();
        medUttak(originalBehandling, List.of(gradering, periodeUtenGradering));

        scenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        scenario.medBehandlingType(BehandlingType.REVURDERING);
        var behandling = scenario.lagMocked();
        medFordeling(behandling, new OppgittFordelingEntitet(Collections.emptyList(), false));

        var andelGraderingList = tjeneste.finnAktivitetGraderingerKalkulus(BehandlingReferanse.fra(behandling)).getAndelGradering();

        assertThat(andelGraderingList).hasSize(1);
        var graderingArbeidsgiver = forArbeidsgiver(andelGraderingList, arbeidsgiver, AktivitetStatus.ARBEIDSTAKER);
        assertThat(graderingArbeidsgiver).hasSize(1);
        var graderinger = graderingArbeidsgiver.get(0).getGraderinger();
        assertThat(graderinger).hasSize(1);
        assertThat(graderinger.get(0).getPeriode().getFomDato()).isEqualTo(gradering.getFom());
        assertThat(graderinger.get(0).getPeriode().getTomDato()).isEqualTo(gradering.getTom());
        assertThat(graderingArbeidsgiver.get(0).getArbeidsgiver().getIdentifikator()).isEqualTo(arbeidsgiver.getIdentifikator());
    }

    @Test
    void hente_gradering_fra_oppgittfordeling_med_uttaksresultat_fra_original_behandling_frilans() {
        // Skal hente gradering fra uttak fram til der oppgittfordeling starter
        var originalScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var uttaksperiodeMedGradering = gradertUttaksperiode(null, UttakArbeidType.FRILANS, LocalDate.of(2019, 5, 15), LocalDate.of(2019, 5, 20));
        var originalBehandling = originalScenario.lagMocked();
        medUttak(originalBehandling, List.of(uttaksperiodeMedGradering));

        scenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        scenario.medBehandlingType(BehandlingType.REVURDERING);
        var oppgittPeriodeMedGradering = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medArbeidsprosent(BigDecimal.TEN)
            .medGraderingAktivitetType(GraderingAktivitetType.FRILANS)
            .medPeriode(LocalDate.of(2019, 5, 21), LocalDate.of(2019, 6, 25))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        var oppgittFordeling = List.of(oppgittPeriodeMedGradering);
        var fordeling = new OppgittFordelingEntitet(oppgittFordeling, false);
        var behandling = scenario.lagMocked();
        medFordeling(behandling, fordeling);

        var andelGraderingList = tjeneste.finnAktivitetGraderingerKalkulus(BehandlingReferanse.fra(behandling)).getAndelGradering();

        assertThat(andelGraderingList).hasSize(1);
        var andelGraderingArbeidsgiver = forArbeidsgiver(andelGraderingList, null, AktivitetStatus.FRILANSER);
        assertThat(andelGraderingArbeidsgiver).hasSize(1);
        var graderinger = andelGraderingArbeidsgiver.get(0).getGraderinger();
        assertThat(graderinger).hasSize(2);
        assertThat(graderinger).anySatisfy(gradering -> assertThat(gradering.getPeriode()).isEqualTo(
            Intervall.fraOgMedTilOgMed(uttaksperiodeMedGradering.getFom(), uttaksperiodeMedGradering.getTom())));
        assertThat(graderinger).anySatisfy(gradering -> assertThat(gradering.getPeriode()).isEqualTo(
            Intervall.fraOgMedTilOgMed(oppgittPeriodeMedGradering.getFom(), oppgittPeriodeMedGradering.getTom())));
    }

    @Test
    void hente_gradering_fra_oppgittfordeling_med_uttaksresultat_fra_original_behandling_sn() {
        // Skal hente gradering fra uttak fram til der oppgittfordeling starter
        var originalScenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var uttaksperiodeMedGradering = gradertUttaksperiode(null, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, LocalDate.of(2019, 5, 15),
            LocalDate.of(2019, 5, 20));
        var originalBehandling = originalScenario.lagMocked();
        medUttak(originalBehandling, List.of(uttaksperiodeMedGradering));

        scenario.medOriginalBehandling(originalBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING);
        scenario.medBehandlingType(BehandlingType.REVURDERING);
        var oppgittPeriodeMedGradering = OppgittPeriodeBuilder.ny()
            .medArbeidsgiver(Arbeidsgiver.virksomhet("123"))
            .medArbeidsprosent(BigDecimal.TEN)
            .medGraderingAktivitetType(GraderingAktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE)
            .medPeriode(LocalDate.of(2019, 5, 21), LocalDate.of(2019, 6, 25))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();

        var oppgittFordeling = List.of(oppgittPeriodeMedGradering);
        var fordeling = new OppgittFordelingEntitet(oppgittFordeling, false);
        var behandling = scenario.lagMocked();
        medFordeling(behandling, fordeling);

        var andelGraderingList = tjeneste.finnAktivitetGraderingerKalkulus(BehandlingReferanse.fra(behandling)).getAndelGradering();

        assertThat(andelGraderingList).hasSize(1);
        var andelGraderingArbeidsgiver = forArbeidsgiver(andelGraderingList, null, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertThat(andelGraderingArbeidsgiver).hasSize(1);
        var graderinger = andelGraderingArbeidsgiver.get(0).getGraderinger();
        assertThat(graderinger).hasSize(2);
        assertThat(graderinger).anySatisfy(gradering -> assertThat(gradering.getPeriode()).isEqualTo(
            Intervall.fraOgMedTilOgMed(uttaksperiodeMedGradering.getFom(), uttaksperiodeMedGradering.getTom())));
        assertThat(graderinger).anySatisfy(gradering -> assertThat(gradering.getPeriode()).isEqualTo(
            Intervall.fraOgMedTilOgMed(oppgittPeriodeMedGradering.getFom(), oppgittPeriodeMedGradering.getTom())));
    }

    private ForeldrepengerUttakPeriode gradertUttaksperiode(Arbeidsgiver arbeidsgiver,
                                                            UttakArbeidType uttakArbeidType,
                                                            LocalDate fom,
                                                            LocalDate tom) {
        var aktivitet = new ForeldrepengerUttakPeriodeAktivitet.Builder().medTrekkonto(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN)
            .medSøktGraderingForAktivitetIPeriode(true)
            .medAktivitet(new ForeldrepengerUttakAktivitet(uttakArbeidType, arbeidsgiver, null))
            .build();
        return new ForeldrepengerUttakPeriode.Builder().medTidsperiode(fom, tom)
            .medGraderingInnvilget(true)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER)
            .medAktiviteter(List.of(aktivitet))
            .build();
    }

    private ForeldrepengerUttakPeriodeAktivitet ugradertFrilansAktivitet() {
        return new ForeldrepengerUttakPeriodeAktivitet.Builder().medTrekkonto(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.ZERO)
            .medSøktGraderingForAktivitetIPeriode(false)
            .medAktivitet(new ForeldrepengerUttakAktivitet(UttakArbeidType.FRILANS))
            .build();
    }

    private ForeldrepengerUttakPeriode uttaksperiode(LocalDate fom, LocalDate tom) {
        var sn = new ForeldrepengerUttakPeriodeAktivitet.Builder().medAktivitet(
                new ForeldrepengerUttakAktivitet(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, null, null))
            .medTrekkonto(UttakPeriodeType.MØDREKVOTE)
            .medArbeidsprosent(BigDecimal.TEN)
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medSøktGraderingForAktivitetIPeriode(false)
            .build();
        var frilans = ugradertFrilansAktivitet();
        return new ForeldrepengerUttakPeriode.Builder().medTidsperiode(fom, tom)
            .medGraderingInnvilget(true)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .medResultatÅrsak(PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER)
            .medAktiviteter(List.of(sn, frilans))
            .build();
    }

    private List<AndelGradering> forArbeidsgiver(Collection<AndelGradering> andelGraderingList, Arbeidsgiver arbeidsgiver, AktivitetStatus status) {
        return andelGraderingList.stream().filter(ag -> {
            var arbeidsgier1 = arbeidsgiver == null ? null : arbeidsgiver.getIdentifikator();
            var arbeidsgiver2 = ag.getArbeidsgiver() == null ? null : ag.getArbeidsgiver().getIdentifikator();
            return ag.getAktivitetStatus().equals(status) && Objects.equals(arbeidsgier1, arbeidsgiver2);
        }).toList();
    }

}
