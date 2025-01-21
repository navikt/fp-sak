package no.nav.foreldrepenger.domene.iay.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;

class YrkesaktivitetEntitetTest {

    ArbeidsforholdInformasjonBuilder builder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());

    @Test
    void skal_legge_overstyrt_periode_når_flere_aktivitetesavtaler_er_lik() {

        // Arrange
        var fom = LocalDate.of(2015, 8, 1);
        var tom = Tid.TIDENES_ENDE;

        var aa1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var aa2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        var overstyrtTom = LocalDate.of(2019, 8, 1);
        var entitet = ArbeidsforholdOverstyringBuilder.ny()
                .medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE)
                .leggTilOverstyrtPeriode(fom, overstyrtTom);

        var ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aa1)
                .leggTilAktivitetsAvtale(aa2)
                .build();

        var yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        var ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getTomDato()).isEqualTo(overstyrtTom);

    }

    @Test
    void skal_legge_overstyrt_periode_på_riktig_aktivitetsavtale_som_er_løpende_og_har_matchende_fom_når_det_finnes_flere() {

        // Arrange
        var fom1 = LocalDate.of(2009, 1, 1);
        var tom1 = LocalDate.of(2009, 12, 31);
        var aa1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));

        var fom2 = LocalDate.of(2010, 1, 1);
        var tom2 = LocalDate.of(2010, 12, 31);
        var aa2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));

        var fom3 = LocalDate.of(2011, 1, 1);
        var tom3 = LocalDate.of(2011, 12, 31);
        var aa3 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3));

        var fom4 = LocalDate.of(2012, 1, 1);
        var tom4 = Tid.TIDENES_ENDE;
        var aa4 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom4, tom4));

        var overstyrtTom = LocalDate.of(2015, 1, 1);
        var entitet = ArbeidsforholdOverstyringBuilder.ny()
                .medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE)
                .leggTilOverstyrtPeriode(fom1, tom1)
                .leggTilOverstyrtPeriode(fom2, tom2)
                .leggTilOverstyrtPeriode(fom3, tom3)
                .leggTilOverstyrtPeriode(fom4, overstyrtTom);

        var ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aa1)
                .leggTilAktivitetsAvtale(aa2)
                .leggTilAktivitetsAvtale(aa3)
                .leggTilAktivitetsAvtale(aa4)
                .build();

        var yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        var ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder)
            .hasSize(4)
            .anySatisfy(p -> {
                assertThat(p.getPeriode().getFomDato()).isEqualTo(fom1);
                assertThat(p.getPeriode().getTomDato()).isEqualTo(tom1);
            }).anySatisfy(p -> {
                assertThat(p.getPeriode().getFomDato()).isEqualTo(fom2);
                assertThat(p.getPeriode().getTomDato()).isEqualTo(tom2);
            }).anySatisfy(p -> {
                assertThat(p.getPeriode().getFomDato()).isEqualTo(fom3);
                assertThat(p.getPeriode().getTomDato()).isEqualTo(tom3);
            }).anySatisfy(p -> {
                assertThat(p.getPeriode().getFomDato()).isEqualTo(fom4);
                assertThat(p.getPeriode().getTomDato()).isEqualTo(overstyrtTom);
            });
    }

    @Test
    void skal_legge_overstyrt_periode_på_aktivitetsavtale_som_er_løpende_og_har_matchende_fom() {

        // Arrange
        var fom = LocalDate.of(2015, 8, 1);
        var tom = Tid.TIDENES_ENDE;
        var overstyrtTom = LocalDate.of(2019, 8, 1);

        var entitet = ArbeidsforholdOverstyringBuilder.ny()
                .medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE)
                .leggTilOverstyrtPeriode(fom, overstyrtTom);
        var aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .build();

        var yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        var ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getTomDato()).isEqualTo(overstyrtTom);

    }

    @Test
    void skal_ikke_legge_overstyrt_periode_på_aktivitetsavtale_som_ikke_er_løpende_og_har_matchende_fom() {

        // Arrange
        var fom = LocalDate.of(2015, 8, 1);
        var tom = LocalDate.of(2020, 8, 1);
        var overstyrtTom = LocalDate.of(2019, 8, 1);

        var entitet = ArbeidsforholdOverstyringBuilder.ny()
                .medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE)
                .leggTilOverstyrtPeriode(fom, overstyrtTom);
        var aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .build();

        var yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        var ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getTomDato()).isEqualTo(tom);

    }

    @Test
    void skal_ikke_legge_overstyrt_periode_på_aktivitetsavtale_som_er_løpende_men_har_ikke_matchende_fom() {

        // Arrange
        var fom = LocalDate.of(2015, 8, 1);
        var tom = Tid.TIDENES_ENDE;
        var overstyrtFom = LocalDate.of(2014, 8, 1);
        var overstyrtTom = LocalDate.of(2019, 8, 1);

        var entitet = ArbeidsforholdOverstyringBuilder.ny()
                .medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE)
                .leggTilOverstyrtPeriode(overstyrtFom, overstyrtTom);
        var aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .build();

        var yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        var ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getTomDato()).isEqualTo(tom);

    }

    @Test
    void skal_ikke_legge_overstyrt_periode_når_overstyrt_handling_ikke_er_BRUK_MED_OVERSTYRT_PERIODE() {

        // Arrange
        var fom = LocalDate.of(2015, 8, 1);
        var tom = Tid.TIDENES_ENDE;
        var overstyrtTom = LocalDate.of(2019, 8, 1);

        var entitet = ArbeidsforholdOverstyringBuilder.ny()
                .medHandling(ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING)
                .leggTilOverstyrtPeriode(fom, overstyrtTom);
        var aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        var ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .build();

        var yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        var ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getTomDato()).isEqualTo(tom);

    }

    private void overstyrYrkesaktivitet(ArbeidsforholdOverstyringBuilder overstyring) {
        builder.leggTil(overstyring);
    }

    private List<AktivitetsAvtale> getAnsettelsesPerioder(Yrkesaktivitet ya) {
        return new YrkesaktivitetFilter(builder.build(), ya).getAnsettelsesPerioder(ya);
    }

}
