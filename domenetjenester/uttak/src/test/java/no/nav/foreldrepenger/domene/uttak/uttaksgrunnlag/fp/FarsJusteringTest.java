package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;

class FarsJusteringTest {

    @Test
    void fødselEtterTermin() {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1));
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(2).minusDays(1));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
    }

    @Test
    void fødselLikTermin() {
        var termindato = LocalDate.of(2022, 8, 12);
        var farsJustering = new FarsJustering(termindato, termindato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1));
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(søktPeriode.getFom());
        assertThat(justert.get(0).getTom()).isEqualTo(søktPeriode.getTom());
    }

    @Test
    void søktFørTerminSkalIkkeJustereVedFødselEtterTermin() {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato.minusDays(2), termindato.plusWeeks(2).minusDays(3));
        assertThatThrownBy(() -> farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fødselEtterTerminFlerePerioderSkalIkkeJusteres1() {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode1 = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1));
        var søktPeriode2 = periodeForFarRundtFødsel(termindato.plusWeeks(4), termindato.plusWeeks(6).minusDays(1));
        var fedrekvoteEtterUke6 = OppgittPeriodeBuilder.ny().medPeriode(termindato.plusWeeks(6), termindato.plusWeeks(10).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();

        assertThatThrownBy(() -> farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode1, søktPeriode2, fedrekvoteEtterUke6)))
            .isInstanceOf(IllegalStateException.class);

    }

    @Test
    void fødselEtterTerminFlerePerioderSkalIkkeJusteres2() {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode1 = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1));
        var søktPeriode2 = periodeForFarRundtFødsel(termindato.plusWeeks(4), termindato.plusWeeks(6).minusDays(1));

        assertThatThrownBy(() -> farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode1, søktPeriode2))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fødselEtterTerminFlerbarnsdagerSkalIkkeJusteres() {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødselBuilder(termindato, termindato.plusWeeks(2).minusDays(1))
            .medFlerbarnsdager(true)
            .build();

        assertThatThrownBy(() -> farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fødselLengeEtterTerminJusteresHvisIngenOverlappMedSenerePeriode() {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(5);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1));
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(2).minusDays(1));
    }

    @Test
    void fødselLengeEtterTerminSkalForkortesAvSenerePeriode() {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(5);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var fedrekvoteEtterUke6 = OppgittPeriodeBuilder.ny().medPeriode(termindato.plusWeeks(6), termindato.plusWeeks(10).minusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();

        var søktPeriode1 = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1));
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode1, fedrekvoteEtterUke6));

        assertThat(justert).hasSize(2);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fedrekvoteEtterUke6.getFom().minusDays(1));

        assertThat(justert.get(1).getFom()).isEqualTo(fedrekvoteEtterUke6.getFom());
        assertThat(justert.get(1).getTom()).isEqualTo(fedrekvoteEtterUke6.getTom());
    }

    @Test
    void fødselEtterTerminFødtIHelg() {
        //torsdag
        var termindato = LocalDate.of(2022, 8, 10);

        //søndag
        var fødselsdato = LocalDate.of(2022, 8, 14);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1));
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(LocalDate.of(2022, 8, 15));
        assertThat(justert.get(0).getTom()).isEqualTo(LocalDate.of(2022, 8, 26));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
    }

    @Test
    void fødselEtterTerminTermindatoIHelg() {
        //lørdag
        var termindato = LocalDate.of(2022, 8, 13);

        //tirsdag
        var fødselsdato = LocalDate.of(2022, 8, 16);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1));
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(2).minusDays(1));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
    }

    @Test
    void fødselEtterTerminBådeTermindatoOgFødselISammeHelg() {
        var termindato = LocalDate.of(2022, 8, 27);
        var fødselsdato = LocalDate.of(2022, 8, 28);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(LocalDate.of(2022, 8, 29), LocalDate.of(2022, 9, 6));
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(søktPeriode.getFom());
        assertThat(justert.get(0).getTom()).isEqualTo(søktPeriode.getTom());
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
    }

    @Test
    void fødselFørTermin() {
        var termindato = LocalDate.of(2022, 8, 10);
        var fødselsdato = termindato.minusWeeks(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1));
        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(2).minusDays(1));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
    }

    @Test
    void fødselFørTerminPeriodeEtterUke6() {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.minusDays(3);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode1 = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1));
        var søktPeriode2 = periodeForFarRundtFødsel(termindato.plusWeeks(6), termindato.plusWeeks(10).minusDays(1));
        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode1, søktPeriode2));

        assertThat(justert).hasSize(2);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(2).minusDays(1));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);

        assertThat(justert.get(1).getFom()).isEqualTo(søktPeriode2.getFom());
        assertThat(justert.get(1).getTom()).isEqualTo(søktPeriode2.getTom());
        assertThat(justert.get(1).getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
    }

    @Test
    void fødselFørTerminFlereUkerRundtFødsel() {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.minusDays(4);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode1 = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(1).minusDays(1));
        var søktPeriode2 = periodeForFarRundtFødsel(termindato.plusWeeks(4), termindato.plusWeeks(5).minusDays(1));

        assertThatThrownBy(() -> farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode1, søktPeriode2))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fødselFørTerminTermindatoIHelg() {
        var termindato = LocalDate.of(2022, 8, 20);
        var fødselsdato = termindato.minusDays(4);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1));
        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(2).minusDays(1));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
    }

    @Test
    void fødselFørTerminFødselIHelg() {
        var termindato = LocalDate.of(2022, 8, 16);
        var fødselsdato = LocalDate.of(2022, 8, 7);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(1).minusDays(1));
        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(LocalDate.of(2022, 8, 8));
        assertThat(justert.get(0).getTom()).isEqualTo(LocalDate.of(2022, 8, 12));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
    }

    @Test
    void fødselFørTerminBådeTermindatoOgFødselIHelg() {
        var termindato = LocalDate.of(2022, 8, 27);
        var fødselsdato = LocalDate.of(2022, 8, 21);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(LocalDate.of(2022, 8, 29), LocalDate.of(2022, 8, 31));
        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(LocalDate.of(2022, 8, 22));
        assertThat(justert.get(0).getTom()).isEqualTo(LocalDate.of(2022, 8, 24));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
    }

    @Test
    void fødselFørTerminBådeTermindatoOgFødselISammeHelg() {
        var termindato = LocalDate.of(2022, 8, 28);
        var fødselsdato = LocalDate.of(2022, 8, 27);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(LocalDate.of(2022, 8, 29), LocalDate.of(2022, 9, 6));
        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(søktPeriode.getFom());
        assertThat(justert.get(0).getTom()).isEqualTo(søktPeriode.getTom());
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(UttakPeriodeType.FEDREKVOTE);
    }

    private OppgittPeriodeEntitet periodeForFarRundtFødsel(LocalDate fom, LocalDate tom) {
        return periodeForFarRundtFødselBuilder(fom, tom).build();
    }

    private OppgittPeriodeBuilder periodeForFarRundtFødselBuilder(LocalDate fom, LocalDate tom) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(100))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medFlerbarnsdager(false);
    }
}
