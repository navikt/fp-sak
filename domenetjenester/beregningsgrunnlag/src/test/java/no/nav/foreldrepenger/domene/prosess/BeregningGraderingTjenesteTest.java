package no.nav.foreldrepenger.domene.prosess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;

class BeregningGraderingTjenesteTest {

    private ForeldrepengerUttakTjeneste uttakTjeneste = Mockito.mock(ForeldrepengerUttakTjeneste.class);
    private YtelsesFordelingRepository ytelsesRepo = Mockito.mock(YtelsesFordelingRepository.class);
    private BeregningGraderingTjeneste beregningGraderingTjeneste = new BeregningGraderingTjeneste(uttakTjeneste, ytelsesRepo);

    private ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

    @Test
    void hente_gradering_fra_oppgittfordeling_førstegangsbehandling() {
        var periode = OppgittPeriodeBuilder.ny()
            .medPeriode(LocalDate.of(2019, 5, 27), LocalDate.of(2019, 5, 31))
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .build();
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

        var graderinger = beregningGraderingTjeneste.finnPerioderMedGradering(BehandlingReferanse.fra(behandling));
        var sortertListe = graderinger.stream().sorted(Comparator.comparing(PeriodeMedGradering::fom)).toList();
        assertThat(graderinger).hasSize(4);

        assertThat(sortertListe.get(0).fom()).isEqualTo(gradering1.getFom());
        assertThat(sortertListe.get(0).tom()).isEqualTo(gradering1.getTom());
        assertThat(sortertListe.get(0).arbeidsgiver()).isEqualTo(gradering1.getArbeidsgiver());
        assertThat(sortertListe.get(0).aktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        assertThat(sortertListe.get(0).arbeidsprosent()).isEqualByComparingTo(gradering1.getArbeidsprosent());

        assertThat(sortertListe.get(1).fom()).isEqualTo(gradering2.getFom());
        assertThat(sortertListe.get(1).tom()).isEqualTo(gradering2.getTom());
        assertThat(sortertListe.get(1).arbeidsgiver()).isEqualTo(gradering2.getArbeidsgiver());
        assertThat(sortertListe.get(1).aktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        assertThat(sortertListe.get(1).arbeidsprosent()).isEqualByComparingTo(gradering2.getArbeidsprosent());

        assertThat(sortertListe.get(2).fom()).isEqualTo(gradering3.getFom());
        assertThat(sortertListe.get(2).tom()).isEqualTo(gradering3.getTom());
        assertThat(sortertListe.get(2).arbeidsgiver()).isEqualTo(gradering3.getArbeidsgiver());
        assertThat(sortertListe.get(2).aktivitetStatus()).isEqualTo(AktivitetStatus.ARBEIDSTAKER);
        assertThat(sortertListe.get(2).arbeidsprosent()).isEqualByComparingTo(gradering3.getArbeidsprosent());

        assertThat(sortertListe.get(3).fom()).isEqualTo(gradering4.getFom());
        assertThat(sortertListe.get(3).tom()).isEqualTo(gradering4.getTom());
        assertThat(sortertListe.get(3).arbeidsgiver()).isEqualTo(gradering4.getArbeidsgiver());
        assertThat(sortertListe.get(3).aktivitetStatus()).isEqualTo(AktivitetStatus.FRILANSER);
        assertThat(sortertListe.get(3).arbeidsprosent()).isEqualByComparingTo(gradering4.getArbeidsprosent());
    }

    private void medFordeling(Behandling behandling, OppgittFordelingEntitet fordeling) {
        var ytelseFordelingAggregat = Mockito.mock(YtelseFordelingAggregat.class);
        when(ytelseFordelingAggregat.getGjeldendeFordeling()).thenReturn(fordeling);
        when(ytelsesRepo.hentAggregatHvisEksisterer(behandling.getId())).thenReturn(Optional.of(ytelseFordelingAggregat));

    }



}
