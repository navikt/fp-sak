package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;

class OppgittPeriodeRelevansTest {

    private static final LocalDate FOM = UtsettelseCore2021.IKRAFT_FRA_DATO;

    @Test
    void skalBeholdeAllePerioderSomKreverSammenhengendeUttak() {
        var opphold = lagPeriode(FOM.minusDays(1), OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER);

        assertThat(OppgittPeriodeRelevans.relevantePerioder(List.of(opphold))).containsExactly(opphold);
    }

    @Test
    void skalBeholdeUttakOgOverføringVedFrittUttak() {
        var uttak = OppgittPeriodeBuilder.ny()
            .medPeriode(FOM, FOM.plusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var overføring = lagPeriode(FOM.plusDays(2), no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak.ALENEOMSORG);

        assertThat(OppgittPeriodeRelevans.relevantePerioder(List.of(uttak, overføring))).containsExactly(uttak, overføring);
    }

    @ParameterizedTest
    @EnumSource(value = UtsettelseÅrsak.class, names = {"SYKDOM", "INSTITUSJON_SØKER", "INSTITUSJON_BARN"})
    void skalBeholdeUtsettelserSomKreverSaksbehandling(UtsettelseÅrsak årsak) {
        var periode = lagPeriode(FOM, årsak);

        assertThat(OppgittPeriodeRelevans.relevantePerioder(List.of(periode))).containsExactly(periode);
    }

    @Test
    void skalKonvertereFørsteUtsettelseOgBeholdeMorsAktivitetSomKreverDokumentasjon() {
        var førstePeriode = OppgittPeriodeBuilder.fraEksisterende(lagPeriode(FOM, UtsettelseÅrsak.ARBEID))
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .build();

        assertThat(OppgittPeriodeRelevans.relevantePerioder(List.of(førstePeriode)))
            .singleElement()
            .satisfies(periode -> {
                assertThat(periode.getÅrsak()).isEqualTo(UtsettelseÅrsak.FRI);
                assertThat(periode.getMorsAktivitet()).isEqualTo(MorsAktivitet.ARBEID);
            });
    }

    @Test
    void skalBeholdeFørsteFriOgFiltrereSenereFri() {
        var førsteFri = lagPeriode(FOM, UtsettelseÅrsak.FRI);
        var uttak = OppgittPeriodeBuilder.ny()
            .medPeriode(FOM.plusDays(2), FOM.plusDays(3))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var senereFri = lagPeriode(FOM.plusDays(4), UtsettelseÅrsak.FRI);

        assertThat(OppgittPeriodeRelevans.relevantePerioder(List.of(førsteFri, uttak, senereFri))).containsExactly(førsteFri, uttak);
    }

    @ParameterizedTest
    @EnumSource(value = UtsettelseÅrsak.class, mode = EnumSource.Mode.EXCLUDE,
        names = {"FRI", "SYKDOM", "INSTITUSJON_SØKER", "INSTITUSJON_BARN"})
    void skalKonvertereFørsteOrdinæreUtsettelseTilFri(UtsettelseÅrsak årsak) {
        var førstePeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(FOM, FOM.plusDays(1))
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medÅrsak(årsak)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .medArbeidsprosent(java.math.BigDecimal.TEN)
            .medDokumentasjonVurdering(new DokumentasjonVurdering(DokumentasjonVurdering.Type.MORS_AKTIVITET_GODKJENT))
            .medMottattDato(FOM.minusDays(1))
            .medTidligstMottattDato(FOM.minusDays(2))
            .medSamtidigUttak(true)
            .medFlerbarnsdager(true)
            .medPeriodeKilde(FordelingPeriodeKilde.TIDLIGERE_VEDTAK)
            .build();

        var resultat = OppgittPeriodeRelevans.relevantePerioder(List.of(førstePeriode));

        assertThat(resultat).singleElement().satisfies(periode -> {
            assertThat(periode.getFom()).isEqualTo(førstePeriode.getFom());
            assertThat(periode.getTom()).isEqualTo(førstePeriode.getTom());
            assertThat(periode.getÅrsak()).isEqualTo(UtsettelseÅrsak.FRI);
            assertThat(periode.getPeriodeType()).isEqualTo(UttakPeriodeType.UDEFINERT);
            assertThat(periode.getPeriodeKilde()).isEqualTo(FordelingPeriodeKilde.TIDLIGERE_VEDTAK);
            assertThat(periode.getMorsAktivitet()).isEqualTo(førstePeriode.getMorsAktivitet());
            assertThat(periode.getDokumentasjonVurdering()).isEqualTo(førstePeriode.getDokumentasjonVurdering());
            assertThat(periode.getMottattDato()).isEqualTo(førstePeriode.getMottattDato());
            assertThat(periode.getTidligstMottattDato()).isEqualTo(førstePeriode.getTidligstMottattDato());
            assertThat(periode.getArbeidsprosent()).isNull();
            assertThat(periode.getArbeidsgiver()).isNull();
            assertThat(periode.getGraderingAktivitetType()).isNull();
            assertThat(periode.getBegrunnelse()).isEmpty();
            assertThat(periode.isSamtidigUttak()).isFalse();
            assertThat(periode.getSamtidigUttaksprosent()).isNull();
            assertThat(periode.isFlerbarnsdager()).isFalse();
        });
    }

    @ParameterizedTest
    @EnumSource(OppholdÅrsak.class)
    void skalKonvertereFørsteOppholdTilFri(OppholdÅrsak årsak) {
        var opphold = lagPeriode(FOM, årsak);

        assertThat(OppgittPeriodeRelevans.relevantePerioder(List.of(opphold)))
            .singleElement()
            .satisfies(periode -> assertThat(periode.getÅrsak()).isEqualTo(UtsettelseÅrsak.FRI));
    }

    @ParameterizedTest
    @EnumSource(value = UtsettelseÅrsak.class, names = {"SYKDOM", "INSTITUSJON_SØKER", "INSTITUSJON_BARN"})
    void skalIkkeKonvertereFørsteUtsettelseSomKreverSaksbehandling(UtsettelseÅrsak årsak) {
        var periode = lagPeriode(FOM, årsak);

        assertThat(OppgittPeriodeRelevans.relevantePerioder(List.of(periode))).containsExactly(periode);
    }

    @Test
    void skalIkkeKonvertereFørstePeriodeFørIkraftdato() {
        var opphold = lagPeriode(FOM.minusDays(1), OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER);

        assertThat(OppgittPeriodeRelevans.relevantePerioder(List.of(opphold))).containsExactly(opphold);
    }

    @Test
    void skalFiltrereOppholdOgIrrelevantUtsettelseVedFrittUttak() {
        var uttak = OppgittPeriodeBuilder.ny()
            .medPeriode(FOM, FOM.plusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var opphold = lagPeriode(FOM.plusDays(2), OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER);
        var utsettelse = lagPeriode(FOM.plusDays(4), UtsettelseÅrsak.FERIE);

        assertThat(OppgittPeriodeRelevans.relevantePerioder(List.of(uttak, opphold, utsettelse))).containsExactly(uttak);
    }

    @ParameterizedTest
    @EnumSource(FordelingPeriodeKilde.class)
    void skalBrukeSammeRegelForAllePeriodekilder(FordelingPeriodeKilde kilde) {
        var uttak = OppgittPeriodeBuilder.ny()
            .medPeriode(FOM, FOM.plusDays(1))
            .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
            .build();
        var periode = OppgittPeriodeBuilder.fraEksisterende(lagPeriode(FOM.plusDays(2), UtsettelseÅrsak.FERIE))
            .medPeriodeKilde(kilde)
            .build();

        assertThat(OppgittPeriodeRelevans.relevantePerioder(List.of(uttak, periode))).containsExactly(uttak);
    }

    private static OppgittPeriodeEntitet lagPeriode(LocalDate fom, Årsak årsak) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fom, fom.plusDays(1))
            .medPeriodeType(UttakPeriodeType.UDEFINERT)
            .medÅrsak(årsak)
            .build();
    }
}
