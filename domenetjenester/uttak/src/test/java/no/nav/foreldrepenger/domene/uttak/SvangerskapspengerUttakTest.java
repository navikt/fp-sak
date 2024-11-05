package no.nav.foreldrepenger.domene.uttak;

import static no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType.AVSLÅTT;
import static no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType.INNVILGET;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.ArbeidsforholdIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;

class SvangerskapspengerUttakTest {

    @Test
    void skal_teste_at_alle_opphørsårsaker_gir_opphør() {
        for (var opphørsårsak : PeriodeIkkeOppfyltÅrsak.opphørsAvslagÅrsaker()) {
            var fom = LocalDate.now();
            var periode = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, fom.plusWeeks(1)).medPeriodeIkkeOppfyltÅrsak(opphørsårsak)
                .medPeriodeResultatType(AVSLÅTT)
                .build();
            var svpUttak = new SvangerskapspengerUttak(new SvangerskapspengerUttakResultatEntitet.Builder(null).medUttakResultatArbeidsforhold(
                new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medPeriode(periode).build()).build());

            assertThat(svpUttak.opphørsdato()).hasValue(fom);
            assertThat(svpUttak.erOpphør()).isTrue();
        }
    }

    @Test
    void skal_returnere_true_når_alle_arbeidsforholdene_er_avslått() {
        var now = LocalDate.of(2024, 10, 31);
        var arbeidsforhold1 = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medPeriode(
                new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(now, now.plusWeeks(2)).medPeriodeResultatType(AVSLÅTT).build())
            .medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak.UTTAK_KUN_PÅ_HELG)
            .build();
        var arbeidsforhold2 = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medPeriode(
                new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(now, now.plusWeeks(2)).medPeriodeResultatType(AVSLÅTT).build())
            .medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE)
            .build();

        var uttakResultat = new SvangerskapspengerUttakResultatEntitet.Builder(null).medUttakResultatArbeidsforhold(arbeidsforhold1)
            .medUttakResultatArbeidsforhold(arbeidsforhold2)
            .build();
        var uttak = new SvangerskapspengerUttak(uttakResultat);

        var resultat = uttak.erOpphør();

        assertThat(resultat).isTrue();
    }

    @Test
    void skal_sjekke_siste_periode_pr_arbeidsforhold_for_spesifikke_avslagsårsaker_og_ikke_gi_opphør_når_siste_ikke_er_på_lista() {
        // Arrange
        var arbeidsforhold1 = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medArbeidsforholdIkkeOppfyltÅrsak(
                ArbeidsforholdIkkeOppfyltÅrsak.INGEN)
            .medPeriode(
                opprettPeriode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), PeriodeIkkeOppfyltÅrsak._8304, AVSLÅTT)) // Er ikke siste
            .medPeriode(opprettPeriode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(1), PeriodeIkkeOppfyltÅrsak.INGEN, AVSLÅTT))
            .build();
        var arbeidsforhold2 = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medArbeidsforholdIkkeOppfyltÅrsak(
                ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE)
            .medPeriode(opprettPeriode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), PeriodeIkkeOppfyltÅrsak.INGEN, AVSLÅTT))
            .medPeriode(opprettPeriode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(1), PeriodeIkkeOppfyltÅrsak._8311,
                AVSLÅTT)) // Er ikke på lista
            .build();

        var uttakResultat = new SvangerskapspengerUttakResultatEntitet.Builder(null).medUttakResultatArbeidsforhold(arbeidsforhold1)
            .medUttakResultatArbeidsforhold(arbeidsforhold2)
            .build();
        var uttakResultatHolder = new SvangerskapspengerUttak(uttakResultat);

        // Act
        var resultat = uttakResultatHolder.erOpphør();

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void skal_ikke_opphøre_hvis_ett_arbeidsforhold_er_innvilget_selv_om_andre_er_opphørt() {
        var d = LocalDate.of(2024, 10, 31);
        var innvilgetArbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medPeriode(
            opprettPeriode(d.minusDays(10), d, PeriodeIkkeOppfyltÅrsak.INGEN, INNVILGET)).build();
        var opphørtArbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medArbeidsforholdIkkeOppfyltÅrsak(
                ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE)
            .medPeriode(opprettPeriode(d.minusDays(10), d, PeriodeIkkeOppfyltÅrsak._8305, AVSLÅTT))
            .build();

        var uttak = new SvangerskapspengerUttak(
            new SvangerskapspengerUttakResultatEntitet.Builder(null).medUttakResultatArbeidsforhold(innvilgetArbeidsforhold)
                .medUttakResultatArbeidsforhold(opphørtArbeidsforhold)
                .build());

        assertThat(uttak.erOpphør()).isFalse();
        assertThat(uttak.opphørsdato()).isEmpty();
    }

    @Test
    void opphørsdato_skal_være_første_fom_ved_flere_perioder() {
        var d = LocalDate.of(2024, 10, 31);
        var førsteOpphørsdato = d.minusDays(10);
        var opphørtArbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medArbeidsforholdIkkeOppfyltÅrsak(
                ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE)
            .medPeriode(opprettPeriode(førsteOpphørsdato, d, PeriodeIkkeOppfyltÅrsak._8305, AVSLÅTT))
            .medPeriode(opprettPeriode(d.plusDays(1), d.plusWeeks(1), PeriodeIkkeOppfyltÅrsak._8305, AVSLÅTT))
            .build();

        var uttak = new SvangerskapspengerUttak(
            new SvangerskapspengerUttakResultatEntitet.Builder(null).medUttakResultatArbeidsforhold(opphørtArbeidsforhold).build());

        assertThat(uttak.erOpphør()).isTrue();
        assertThat(uttak.opphørsdato()).hasValue(førsteOpphørsdato);
    }

    @Test
    void skal_ikke_opphøre_hvis_opphørsperiode_er_etterfulgt_av_innvilget_periode() {
        var d = LocalDate.of(2024, 10, 31);
        var arbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medPeriode(
                opprettPeriode(d.minusDays(10), d, PeriodeIkkeOppfyltÅrsak._8304, AVSLÅTT))
            .medPeriode(opprettPeriode(d.plusDays(1), d.plusWeeks(1), PeriodeIkkeOppfyltÅrsak.INGEN, INNVILGET))
            .build();

        var uttak = new SvangerskapspengerUttak(
            new SvangerskapspengerUttakResultatEntitet.Builder(null).medUttakResultatArbeidsforhold(arbeidsforhold).build());

        assertThat(uttak.erOpphør()).isFalse();
        assertThat(uttak.opphørsdato()).isEmpty();
    }

    private SvangerskapspengerUttakResultatPeriodeEntitet opprettPeriode(LocalDate fom,
                                                                         LocalDate tom,
                                                                         PeriodeIkkeOppfyltÅrsak årsak,
                                                                         PeriodeResultatType periodeResultatType) {
        return new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, tom).medRegelInput("{}")
            .medRegelEvaluering("{}")
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medPeriodeIkkeOppfyltÅrsak(årsak)
            .medPeriodeResultatType(periodeResultatType)
            .build();
    }

}
