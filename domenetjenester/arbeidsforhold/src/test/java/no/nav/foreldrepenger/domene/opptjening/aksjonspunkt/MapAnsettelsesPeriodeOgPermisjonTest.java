package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static java.time.Month.APRIL;
import static java.time.Month.FEBRUARY;
import static java.time.Month.JANUARY;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

class MapAnsettelsesPeriodeOgPermisjonTest {
    private static final LocalDate A = LocalDate.of(2019, JANUARY, 13);
    private static final LocalDate B = LocalDate.of(2019, FEBRUARY, 5);
    private static final LocalDate C = LocalDate.of(2019, FEBRUARY, 6);
    private static final LocalDate D = LocalDate.of(2019, APRIL, 14);
    private static final LocalDate E = LocalDate.of(2019, APRIL, 15);
    private static final String BESKRIVELSE_1 = "1";
    private static final String BESKRIVELSE_2 = "2";
    private static final String BESKRIVELSE_3 = "3";
    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("a1");
    private static final InternArbeidsforholdRef REF = InternArbeidsforholdRef.nyRef();

    @Test
    void utenTomUtenPermisjon() {
        // Arrange
        var ap1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(A))
                .medBeskrivelse(BESKRIVELSE_1);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(ap1)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdId(REF)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();
        var grunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();

        // Act
        var resultat = MapAnsettelsesPeriodeOgPermisjon.ansettelsesPerioderUtenomFullPermisjon(grunnlag, yrkesaktivitet);

        // Assert
        assertThat(resultat).containsExactly(ap1.build());
    }

    @Test
    void utenTomMedPermisjonIMidten() {
        // Arrange
        var ap1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(A))
                .medBeskrivelse(BESKRIVELSE_1);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(ap1)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdId(REF)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(YrkesaktivitetBuilder.nyPermisjonBuilder()
                        .medPeriode(C, D)
                        .medProsentsats(BigDecimal.valueOf(100))
                        .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                        .build())
                .build();
        var grunnlag = grunnlagMedBekreftetPermisjon(C, D);

        // Act
        var resultat = MapAnsettelsesPeriodeOgPermisjon.ansettelsesPerioderUtenomFullPermisjon(grunnlag, yrkesaktivitet);

        // Assert
        assertThat(resultat)
            .hasSize(2)
            .anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(A);
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(B);
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_1);
            }).anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(E);
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_1);
            });
    }

    @Test
    void utenTomMedPermisjonIStarten() {
        // Arrange
        var ap1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(A))
                .medBeskrivelse(BESKRIVELSE_1);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(ap1)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdId(REF)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(YrkesaktivitetBuilder.nyPermisjonBuilder()
                        .medPeriode(A, B)
                        .medProsentsats(BigDecimal.valueOf(100))
                        .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                        .build())
                .build();
        var grunnlag = grunnlagMedBekreftetPermisjon(A, B);

        // Act
        var resultat = MapAnsettelsesPeriodeOgPermisjon.ansettelsesPerioderUtenomFullPermisjon(grunnlag, yrkesaktivitet);

        // Assert
        assertThat(resultat)
            .hasSize(1)
            .anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(C);
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_1);
            });
    }

    @Test
    void utenTomMedPermisjonISlutten() {
        // Arrange
        var ap1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(A))
                .medBeskrivelse(BESKRIVELSE_1);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(ap1)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdId(REF)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(YrkesaktivitetBuilder.nyPermisjonBuilder()
                        .medPeriode(E, Tid.TIDENES_ENDE)
                        .medProsentsats(BigDecimal.valueOf(100))
                        .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                        .build())
                .build();
        var grunnlag = grunnlagMedBekreftetPermisjon(E, Tid.TIDENES_ENDE);

        // Act
        var resultat = MapAnsettelsesPeriodeOgPermisjon.ansettelsesPerioderUtenomFullPermisjon(grunnlag, yrkesaktivitet);

        // Assert
        assertThat(resultat)
            .hasSize(1)
            .anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(A);
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(D);
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_1);
            });
    }

    @Test
    void utenTomMedPermisjonHelePerioden() {
        // Arrange
        var ap1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(A))
                .medBeskrivelse(BESKRIVELSE_1);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(ap1)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdId(REF)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(YrkesaktivitetBuilder.nyPermisjonBuilder()
                        .medPeriode(A, Tid.TIDENES_ENDE)
                        .medProsentsats(BigDecimal.valueOf(100))
                        .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                        .build())
                .build();
        var grunnlag = grunnlagMedBekreftetPermisjon(A, Tid.TIDENES_ENDE);

        // Act
        var resultat = MapAnsettelsesPeriodeOgPermisjon.ansettelsesPerioderUtenomFullPermisjon(grunnlag, yrkesaktivitet);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void toAnsettelsesPerioderIngenPermisjon() {
        // Arrange
        var ap1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(A, B))
                .medBeskrivelse(BESKRIVELSE_1);
        var ap2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(C, D))
                .medBeskrivelse(BESKRIVELSE_2);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(ap1)
                .leggTilAktivitetsAvtale(ap2)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdId(REF)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .build();
        var grunnlag = InntektArbeidYtelseGrunnlagBuilder.nytt().build();

        // Act
        var resultat = MapAnsettelsesPeriodeOgPermisjon.ansettelsesPerioderUtenomFullPermisjon(grunnlag, yrkesaktivitet);

        // Assert
        assertThat(resultat).containsExactlyInAnyOrder(ap1.build(), ap2.build());
    }

    @Test
    void toAnsettelsesPerioderPermisjonIMidten() {
        // Arrange
        var ap1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(A, B))
                .medBeskrivelse(BESKRIVELSE_1);
        var ap2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(C))
                .medBeskrivelse(BESKRIVELSE_2);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(ap1)
                .leggTilAktivitetsAvtale(ap2)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdId(REF)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(YrkesaktivitetBuilder.nyPermisjonBuilder()
                        .medPeriode(B, D)
                        .medProsentsats(BigDecimal.valueOf(100))
                        .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                        .build())
                .build();
        var grunnlag = grunnlagMedBekreftetPermisjon(B, D);

        // Act
        var resultat = MapAnsettelsesPeriodeOgPermisjon.ansettelsesPerioderUtenomFullPermisjon(grunnlag, yrkesaktivitet);

        // Assert
        assertThat(resultat)
            .hasSize(2)
            .anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(A);
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(LocalDate.of(2019, FEBRUARY, 4));
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_1);
            }).anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(E);
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_2);
            });
    }

    @Test
    void treAnsettelsesPerioderPermisjonIMidten() {
        // Arrange
        var ap1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(A, B))
                .medBeskrivelse(BESKRIVELSE_1);
        var ap2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(C, D))
                .medBeskrivelse(BESKRIVELSE_2);
        var ap3 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(E))
                .medBeskrivelse(BESKRIVELSE_3);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(ap1)
                .leggTilAktivitetsAvtale(ap2)
                .leggTilAktivitetsAvtale(ap3)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdId(REF)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(YrkesaktivitetBuilder.nyPermisjonBuilder()
                        .medPeriode(LocalDate.of(2019, Month.MARCH, 1),
                            LocalDate.of(2019, Month.MARCH, 31))
                        .medProsentsats(BigDecimal.valueOf(100))
                        .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                        .build())
                .build();
        var grunnlag = grunnlagMedBekreftetPermisjon(LocalDate.of(2019, Month.MARCH, 1),
                LocalDate.of(2019, Month.MARCH, 31));

        // Act
        var resultat = MapAnsettelsesPeriodeOgPermisjon.ansettelsesPerioderUtenomFullPermisjon(grunnlag, yrkesaktivitet);

        // Assert
        assertThat(resultat)
            .hasSize(4)
            .anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(A);
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(B);
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_1);
            }).anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(C);
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(LocalDate.of(2019, FEBRUARY, 28));
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_2);
            }).anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(LocalDate.of(2019, APRIL, 1));
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(D);
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_2);
            }).anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(E);
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_3);
            });
    }

    @Test
    void toAnsettelsesPerioderPermisjonIStarten() {
        // Arrange
        var ap1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(A, B))
                .medBeskrivelse(BESKRIVELSE_1);
        var ap2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(C))
                .medBeskrivelse(BESKRIVELSE_2);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(ap1)
                .leggTilAktivitetsAvtale(ap2)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdId(REF)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(YrkesaktivitetBuilder.nyPermisjonBuilder()
                        .medPeriode(A, D)
                        .medProsentsats(BigDecimal.valueOf(100))
                        .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                        .build())
                .build();
        var grunnlag = grunnlagMedBekreftetPermisjon(A, D);

        // Act
        var resultat = MapAnsettelsesPeriodeOgPermisjon.ansettelsesPerioderUtenomFullPermisjon(grunnlag, yrkesaktivitet);

        // Assert
        assertThat(resultat)
            .hasSize(1)
            .anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(E);
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_2);
            });
    }

    @Test
    void toAnsettelsesPerioderPermisjonISlutten() {
        // Arrange
        var ap1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(A, B))
                .medBeskrivelse(BESKRIVELSE_1);
        var ap2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(C))
                .medBeskrivelse(BESKRIVELSE_2);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(ap1)
                .leggTilAktivitetsAvtale(ap2)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdId(REF)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(YrkesaktivitetBuilder.nyPermisjonBuilder()
                        .medPeriode(E, Tid.TIDENES_ENDE)
                        .medProsentsats(BigDecimal.valueOf(100))
                        .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                        .build())
                .build();
        var grunnlag = grunnlagMedBekreftetPermisjon(E, Tid.TIDENES_ENDE);

        // Act
        var resultat = MapAnsettelsesPeriodeOgPermisjon.ansettelsesPerioderUtenomFullPermisjon(grunnlag, yrkesaktivitet);

        // Assert
        assertThat(resultat)
            .hasSize(2)
            .anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(A);
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(B);
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_1);
            }).anySatisfy(ansettelsesperiode -> {
                assertThat(ansettelsesperiode.getPeriode().getFomDato()).isEqualTo(C);
                assertThat(ansettelsesperiode.getPeriode().getTomDato()).isEqualTo(D);
                assertThat(ansettelsesperiode.getBeskrivelse()).isEqualTo(BESKRIVELSE_2);
            });
    }

    @Test
    void toAnsettelsesPerioderMedPermisjonHelePerioden() {
        // Arrange
        var ap1 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(A, B))
                .medBeskrivelse(BESKRIVELSE_1);
        var ap2 = AktivitetsAvtaleBuilder.ny()
                .medPeriode(DatoIntervallEntitet.fraOgMed(C))
                .medBeskrivelse(BESKRIVELSE_2);
        var yrkesaktivitet = YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .leggTilAktivitetsAvtale(ap1)
                .leggTilAktivitetsAvtale(ap2)
                .medArbeidsgiver(ARBEIDSGIVER)
                .medArbeidsforholdId(REF)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .leggTilPermisjon(YrkesaktivitetBuilder.nyPermisjonBuilder()
                        .medPeriode(A, Tid.TIDENES_ENDE)
                        .medProsentsats(BigDecimal.valueOf(100))
                        .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                        .build())
                .build();
        var grunnlag = grunnlagMedBekreftetPermisjon(A, Tid.TIDENES_ENDE);

        // Act
        var resultat = MapAnsettelsesPeriodeOgPermisjon.ansettelsesPerioderUtenomFullPermisjon(grunnlag, yrkesaktivitet);

        // Assert
        assertThat(resultat).isEmpty();
    }

    private InntektArbeidYtelseGrunnlag grunnlagMedBekreftetPermisjon(LocalDate fom, LocalDate tom) {
        var arbeidsforholdInformasjonBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        var overstyringBuilder = arbeidsforholdInformasjonBuilder.getOverstyringBuilderFor(ARBEIDSGIVER, REF);
        overstyringBuilder.medBekreftetPermisjon(new BekreftetPermisjon(fom, tom, BekreftetPermisjonStatus.BRUK_PERMISJON));
        return InntektArbeidYtelseGrunnlagBuilder.nytt()
                .medInformasjon(arbeidsforholdInformasjonBuilder
                        .leggTil(overstyringBuilder)
                        .build())
                .build();
    }
}
