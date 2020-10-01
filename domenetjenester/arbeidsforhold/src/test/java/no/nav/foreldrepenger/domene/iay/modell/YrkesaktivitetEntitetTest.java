package no.nav.foreldrepenger.domene.iay.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.vedtak.konfig.Tid;

public class YrkesaktivitetEntitetTest {

    ArbeidsforholdInformasjonBuilder builder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());

    @Test
    public void skal_legge_overstyrt_periode_når_flere_aktivitetesavtaler_er_lik() {

        // Arrange
        LocalDate fom = LocalDate.of(2015, 8, 1);
        LocalDate tom = Tid.TIDENES_ENDE;

        AktivitetsAvtaleBuilder aa1 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        AktivitetsAvtaleBuilder aa2 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        LocalDate overstyrtTom = LocalDate.of(2019, 8, 1);
        ArbeidsforholdOverstyringBuilder entitet = ArbeidsforholdOverstyringBuilder.ny()
            .medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE)
            .leggTilOverstyrtPeriode(fom, overstyrtTom);

        Yrkesaktivitet ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aa1)
            .leggTilAktivitetsAvtale(aa2)
            .build();

        Yrkesaktivitet yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        List<AktivitetsAvtale> ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getTomDato()).isEqualTo(overstyrtTom);

    }

    @Test
    public void skal_legge_overstyrt_periode_på_riktig_aktivitetsavtale_som_er_løpende_og_har_matchende_fom_når_det_finnes_flere() {

        // Arrange
        LocalDate fom1 = LocalDate.of(2009, 1, 1);
        LocalDate tom1 = LocalDate.of(2009, 12, 31);
        AktivitetsAvtaleBuilder aa1 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1));

        LocalDate fom2 = LocalDate.of(2010, 1, 1);
        LocalDate tom2 = LocalDate.of(2010, 12, 31);
        AktivitetsAvtaleBuilder aa2 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));

        LocalDate fom3 = LocalDate.of(2011, 1, 1);
        LocalDate tom3 = LocalDate.of(2011, 12, 31);
        AktivitetsAvtaleBuilder aa3 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom3, tom3));

        LocalDate fom4 = LocalDate.of(2012, 1, 1);
        LocalDate tom4 = Tid.TIDENES_ENDE;
        AktivitetsAvtaleBuilder aa4 = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom4, tom4));

        LocalDate overstyrtTom = LocalDate.of(2015, 1, 1);
        ArbeidsforholdOverstyringBuilder entitet = ArbeidsforholdOverstyringBuilder.ny()
            .medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE)
            .leggTilOverstyrtPeriode(fom1, tom1)
            .leggTilOverstyrtPeriode(fom2, tom2)
            .leggTilOverstyrtPeriode(fom3, tom3)
            .leggTilOverstyrtPeriode(fom4, overstyrtTom);

        Yrkesaktivitet ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aa1)
            .leggTilAktivitetsAvtale(aa2)
            .leggTilAktivitetsAvtale(aa3)
            .leggTilAktivitetsAvtale(aa4)
            .build();

        Yrkesaktivitet yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        List<AktivitetsAvtale> ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(4);
        assertThat(ansettelsesPerioder).anySatisfy(p -> {
            assertThat(p.getPeriode().getFomDato()).isEqualTo(fom1);
            assertThat(p.getPeriode().getTomDato()).isEqualTo(tom1);
        });
        assertThat(ansettelsesPerioder).anySatisfy(p -> {
            assertThat(p.getPeriode().getFomDato()).isEqualTo(fom2);
            assertThat(p.getPeriode().getTomDato()).isEqualTo(tom2);
        });
        assertThat(ansettelsesPerioder).anySatisfy(p -> {
            assertThat(p.getPeriode().getFomDato()).isEqualTo(fom3);
            assertThat(p.getPeriode().getTomDato()).isEqualTo(tom3);
        });
        assertThat(ansettelsesPerioder).anySatisfy(p -> {
            assertThat(p.getPeriode().getFomDato()).isEqualTo(fom4);
            assertThat(p.getPeriode().getTomDato()).isEqualTo(overstyrtTom);
        });

    }

    @Test
    public void skal_legge_overstyrt_periode_på_aktivitetsavtale_som_er_løpende_og_har_matchende_fom() {

        // Arrange
        LocalDate fom = LocalDate.of(2015, 8, 1);
        LocalDate tom = Tid.TIDENES_ENDE;
        LocalDate overstyrtTom = LocalDate.of(2019, 8, 1);

        ArbeidsforholdOverstyringBuilder entitet = ArbeidsforholdOverstyringBuilder.ny()
            .medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE)
            .leggTilOverstyrtPeriode(fom, overstyrtTom);
        AktivitetsAvtaleBuilder aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        Yrkesaktivitet ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .build();

        Yrkesaktivitet yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        List<AktivitetsAvtale> ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getTomDato()).isEqualTo(overstyrtTom);

    }

    @Test
    public void skal_ikke_legge_overstyrt_periode_på_aktivitetsavtale_som_ikke_er_løpende_og_har_matchende_fom() {

        // Arrange
        LocalDate fom = LocalDate.of(2015, 8, 1);
        LocalDate tom = LocalDate.of(2020, 8, 1);
        LocalDate overstyrtTom = LocalDate.of(2019, 8, 1);

        ArbeidsforholdOverstyringBuilder entitet = ArbeidsforholdOverstyringBuilder.ny()
            .medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE)
            .leggTilOverstyrtPeriode(fom, overstyrtTom);
        AktivitetsAvtaleBuilder aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        Yrkesaktivitet ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .build();

        Yrkesaktivitet yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        List<AktivitetsAvtale> ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getTomDato()).isEqualTo(tom);

    }

    @Test
    public void skal_ikke_legge_overstyrt_periode_på_aktivitetsavtale_som_er_løpende_men_har_ikke_matchende_fom() {

        // Arrange
        LocalDate fom = LocalDate.of(2015, 8, 1);
        LocalDate tom = Tid.TIDENES_ENDE;
        LocalDate overstyrtFom = LocalDate.of(2014, 8, 1);
        LocalDate overstyrtTom = LocalDate.of(2019, 8, 1);

        ArbeidsforholdOverstyringBuilder entitet = ArbeidsforholdOverstyringBuilder.ny()
            .medHandling(ArbeidsforholdHandlingType.BRUK_MED_OVERSTYRT_PERIODE)
            .leggTilOverstyrtPeriode(overstyrtFom, overstyrtTom);
        AktivitetsAvtaleBuilder aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        Yrkesaktivitet ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .build();

        Yrkesaktivitet yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        List<AktivitetsAvtale> ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
        assertThat(ansettelsesPerioder).hasSize(1);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getFomDato()).isEqualTo(fom);
        assertThat(ansettelsesPerioder.get(0).getPeriode().getTomDato()).isEqualTo(tom);

    }

    @Test
    public void skal_ikke_legge_overstyrt_periode_når_overstyrt_handling_ikke_er_BRUK_MED_OVERSTYRT_PERIODE() {

        // Arrange
        LocalDate fom = LocalDate.of(2015, 8, 1);
        LocalDate tom = Tid.TIDENES_ENDE;
        LocalDate overstyrtTom = LocalDate.of(2019, 8, 1);

        ArbeidsforholdOverstyringBuilder entitet = ArbeidsforholdOverstyringBuilder.ny()
            .medHandling(ArbeidsforholdHandlingType.BRUK_UTEN_INNTEKTSMELDING)
            .leggTilOverstyrtPeriode(fom, overstyrtTom);
        AktivitetsAvtaleBuilder aktivitetsAvtale = AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        Yrkesaktivitet ya = YrkesaktivitetBuilder.oppdatere(Optional.empty())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .build();

        Yrkesaktivitet yrkesaktivitet = new Yrkesaktivitet(ya);

        // Act
        overstyrYrkesaktivitet(entitet);

        // Arrange
        List<AktivitetsAvtale> ansettelsesPerioder = getAnsettelsesPerioder(yrkesaktivitet);
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
