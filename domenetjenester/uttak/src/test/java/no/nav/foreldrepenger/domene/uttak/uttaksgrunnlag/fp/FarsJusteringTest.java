package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;

class FarsJusteringTest {

    static Set<UttakPeriodeType> typerStønadskonto() {
        return EnumSet.of(UttakPeriodeType.FEDREKVOTE, UttakPeriodeType.FORELDREPENGER);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselEtterTermin(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(2).minusDays(1));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(uttakPeriodeType);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselLikTermin(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 12);
        var farsJustering = new FarsJustering(termindato, termindato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(søktPeriode.getFom());
        assertThat(justert.get(0).getTom()).isEqualTo(søktPeriode.getTom());
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void søktFørTerminSkalIkkeJustereVedFødselEtterTermin(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato.minusDays(2), termindato.plusWeeks(2).minusDays(3), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));
        assertThat(justert).hasSize(1);
        assertThat(justert.get(0)).isEqualTo(søktPeriode);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselEtterTerminFlerePerioderSkalIkkeJusteres1(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode1 = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1), uttakPeriodeType);
        var søktPeriode2 = periodeForFarRundtFødsel(termindato.plusWeeks(4), termindato.plusWeeks(6).minusDays(1), uttakPeriodeType);
        var periodeEtterUke6 = OppgittPeriodeBuilder.ny()
            .medPeriode(termindato.plusWeeks(6), termindato.plusWeeks(10).minusDays(1))
            .medPeriodeType(uttakPeriodeType)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();

        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode1, søktPeriode2, periodeEtterUke6));
        assertThat(justert).hasSize(3);
        assertThat(justert.get(0)).isEqualTo(søktPeriode1);
        assertThat(justert.get(1)).isEqualTo(søktPeriode2);
        assertThat(justert.get(2)).isEqualTo(periodeEtterUke6);

    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselEtterTerminFlerePerioderSkalIkkeJusteres2(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode1 = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1), uttakPeriodeType);
        var søktPeriode2 = periodeForFarRundtFødsel(termindato.plusWeeks(4), termindato.plusWeeks(6).minusDays(1), uttakPeriodeType);

        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode1, søktPeriode2));
        assertThat(justert).hasSize(2);
        assertThat(justert.get(0)).isEqualTo(søktPeriode1);
        assertThat(justert.get(1)).isEqualTo(søktPeriode2);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselLengeEtterTerminJusteresHvisIngenOverlappMedSenerePeriode(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(5);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(2).minusDays(1));
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselLengeEtterTerminSkalForkortesAvSenerePeriode(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.plusWeeks(5);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var periodeEtterUke6 = OppgittPeriodeBuilder.ny()
            .medPeriode(termindato.plusWeeks(6), termindato.plusWeeks(10).minusDays(1))
            .medPeriodeType(uttakPeriodeType)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();

        var søktPeriode1 = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode1, periodeEtterUke6));

        assertThat(justert).hasSize(2);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(periodeEtterUke6.getFom().minusDays(1));

        assertThat(justert.get(1).getFom()).isEqualTo(periodeEtterUke6.getFom());
        assertThat(justert.get(1).getTom()).isEqualTo(periodeEtterUke6.getTom());
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselEtterTerminFødtIHelg(UttakPeriodeType uttakPeriodeType) {
        //torsdag
        var termindato = LocalDate.of(2022, 8, 10);

        //søndag
        var fødselsdato = LocalDate.of(2022, 8, 14);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(LocalDate.of(2022, 8, 15));
        assertThat(justert.get(0).getTom()).isEqualTo(LocalDate.of(2022, 8, 26));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(uttakPeriodeType);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselEtterTerminTermindatoIHelg(UttakPeriodeType uttakPeriodeType) {
        //lørdag
        var termindato = LocalDate.of(2022, 8, 13);

        //tirsdag
        var fødselsdato = LocalDate.of(2022, 8, 16);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(2).minusDays(1));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(uttakPeriodeType);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselEtterTerminBådeTermindatoOgFødselISammeHelg(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 27);
        var fødselsdato = LocalDate.of(2022, 8, 28);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(LocalDate.of(2022, 8, 29), LocalDate.of(2022, 9, 6), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(søktPeriode.getFom());
        assertThat(justert.get(0).getTom()).isEqualTo(søktPeriode.getTom());
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(uttakPeriodeType);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselFørTermin(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 10);
        var fødselsdato = termindato.minusWeeks(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(2).minusDays(1));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(uttakPeriodeType);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselFørTerminPeriodeEtterUke6(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.minusDays(3);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode1 = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1), uttakPeriodeType);
        var søktPeriode2 = periodeForFarRundtFødsel(termindato.plusWeeks(6), termindato.plusWeeks(10).minusDays(1), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode1, søktPeriode2));

        assertThat(justert).hasSize(2);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(2).minusDays(1));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(uttakPeriodeType);

        assertThat(justert.get(1).getFom()).isEqualTo(søktPeriode2.getFom());
        assertThat(justert.get(1).getTom()).isEqualTo(søktPeriode2.getTom());
        assertThat(justert.get(1).getPeriodeType()).isEqualTo(uttakPeriodeType);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselFørTerminFlereUkerRundtFødsel(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 12);
        var fødselsdato = termindato.minusDays(4);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode1 = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(1).minusDays(1), uttakPeriodeType);
        var søktPeriode2 = periodeForFarRundtFødsel(termindato.plusWeeks(4), termindato.plusWeeks(5).minusDays(1), uttakPeriodeType);

        var perioder = List.of(søktPeriode1, søktPeriode2);
        assertThatThrownBy(() -> farsJustering.justerVedFødselEtterTermin(perioder)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselFørTerminTermindatoIHelg(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 20);
        var fødselsdato = termindato.minusDays(4);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(2).minusDays(1), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(fødselsdato.plusWeeks(2).minusDays(1));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(uttakPeriodeType);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselFørTerminFødselIHelg(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 16);
        var fødselsdato = LocalDate.of(2022, 8, 7);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(termindato, termindato.plusWeeks(1).minusDays(1), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(LocalDate.of(2022, 8, 8));
        assertThat(justert.get(0).getTom()).isEqualTo(LocalDate.of(2022, 8, 12));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(uttakPeriodeType);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselFørTerminBådeTermindatoOgFødselIHelg(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 27);
        var fødselsdato = LocalDate.of(2022, 8, 21);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(LocalDate.of(2022, 8, 29), LocalDate.of(2022, 8, 31), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(LocalDate.of(2022, 8, 22));
        assertThat(justert.get(0).getTom()).isEqualTo(LocalDate.of(2022, 8, 24));
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(uttakPeriodeType);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselFørTerminBådeTermindatoOgFødselISammeHelg(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 8, 28);
        var fødselsdato = LocalDate.of(2022, 8, 27);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode = periodeForFarRundtFødsel(LocalDate.of(2022, 8, 29), LocalDate.of(2022, 9, 6), uttakPeriodeType);
        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode));

        assertThat(justert).hasSize(1);
        assertThat(justert.get(0).getFom()).isEqualTo(søktPeriode.getFom());
        assertThat(justert.get(0).getTom()).isEqualTo(søktPeriode.getTom());
        assertThat(justert.get(0).getPeriodeType()).isEqualTo(uttakPeriodeType);
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselEtterTerminFlereLikePerioderKanJusteres(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 12, 5);
        var fødselsdato = termindato.plusDays(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode1 = periodeForFarRundtFødsel(termindato, termindato, uttakPeriodeType);
        var søktPeriode2 = periodeForFarRundtFødsel(termindato.plusDays(1), LocalDate.of(2022, 12, 16), uttakPeriodeType);
        var periodeEtterUke6 = OppgittPeriodeBuilder.ny()
            .medPeriode(termindato.plusWeeks(6), termindato.plusWeeks(10).minusDays(1))
            .medPeriodeType(uttakPeriodeType)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();

        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode1, søktPeriode2, periodeEtterUke6));
        assertThat(justert).hasSize(2);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(LocalDate.of(2022, 12, 19));
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselFørTerminFlereLikePerioderKanJusteres(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 12, 6);
        var fødselsdato = termindato.minusDays(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode1 = periodeForFarRundtFødsel(termindato, termindato, uttakPeriodeType);
        var søktPeriode2 = periodeForFarRundtFødsel(termindato.plusDays(1), LocalDate.of(2022, 12, 16), uttakPeriodeType);
        var periodeEtterUke6 = OppgittPeriodeBuilder.ny()
            .medPeriode(termindato.plusWeeks(6), termindato.plusWeeks(10).minusDays(1))
            .medPeriodeType(uttakPeriodeType)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();

        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode1, søktPeriode2, periodeEtterUke6));
        assertThat(justert).hasSize(2);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(LocalDate.of(2022, 12, 15));
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselEtterTerminFlereLikePerioderBortsettFraTidligstMottattDatoKanJusteres(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 12, 5);
        var fødselsdato = termindato.plusDays(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode1 = periodeForFarRundtFødselBuilder(termindato, termindato, uttakPeriodeType).medTidligstMottattDato(termindato).build();
        var søktPeriode2 = periodeForFarRundtFødselBuilder(termindato.plusDays(1), LocalDate.of(2022, 12, 16),
            uttakPeriodeType).medTidligstMottattDato(fødselsdato).build();
        var periodeEtterUke6 = OppgittPeriodeBuilder.ny()
            .medPeriode(termindato.plusWeeks(6), termindato.plusWeeks(10).minusDays(1))
            .medPeriodeType(uttakPeriodeType)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();

        var justert = farsJustering.justerVedFødselEtterTermin(List.of(søktPeriode1, søktPeriode2, periodeEtterUke6));
        assertThat(justert).hasSize(2);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(LocalDate.of(2022, 12, 19));
    }

    @ParameterizedTest
    @MethodSource(value = "typerStønadskonto")
    void fødselFørTerminFlereLikePerioderBortsettFraTidligstMottattDatoKanJusteres(UttakPeriodeType uttakPeriodeType) {
        var termindato = LocalDate.of(2022, 12, 6);
        var fødselsdato = termindato.minusDays(1);
        var farsJustering = new FarsJustering(termindato, fødselsdato, true);

        var søktPeriode1 = periodeForFarRundtFødselBuilder(termindato, termindato, uttakPeriodeType).medTidligstMottattDato(termindato).build();
        var søktPeriode2 = periodeForFarRundtFødselBuilder(termindato.plusDays(1), LocalDate.of(2022, 12, 16),
            uttakPeriodeType).medTidligstMottattDato(fødselsdato).build();
        var periodeEtterUke6 = OppgittPeriodeBuilder.ny()
            .medPeriode(termindato.plusWeeks(6), termindato.plusWeeks(10).minusDays(1))
            .medPeriodeType(uttakPeriodeType)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .build();

        var justert = farsJustering.justerVedFødselFørTermin(List.of(søktPeriode1, søktPeriode2, periodeEtterUke6));
        assertThat(justert).hasSize(2);
        assertThat(justert.get(0).getFom()).isEqualTo(fødselsdato);
        assertThat(justert.get(0).getTom()).isEqualTo(LocalDate.of(2022, 12, 15));
    }

    private OppgittPeriodeEntitet periodeForFarRundtFødsel(LocalDate fom, LocalDate tom, UttakPeriodeType uttakPeriodeType) {
        return periodeForFarRundtFødselBuilder(fom, tom, uttakPeriodeType).build();
    }

    private OppgittPeriodeBuilder periodeForFarRundtFødselBuilder(LocalDate fom, LocalDate tom, UttakPeriodeType uttakPeriodeType) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, tom)
            .medSamtidigUttak(true)
            .medSamtidigUttaksprosent(new SamtidigUttaksprosent(100))
            .medPeriodeType(uttakPeriodeType)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medFlerbarnsdager(false);
    }
}
