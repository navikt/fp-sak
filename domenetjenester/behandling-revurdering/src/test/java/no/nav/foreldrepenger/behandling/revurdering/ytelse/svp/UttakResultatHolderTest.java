package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.ArbeidsforholdIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;

public class UttakResultatHolderTest {

    @Test
    public void skal_returnere_false_når_det_ikke_er_noe_uttaksresultat() {
        // Arrange
        UttakResultatHolderSVP uttakResultatHolder = new UttakResultatHolderSVP(Optional.empty(), null);

        // Act
        boolean resultat = uttakResultatHolder.kontrollerErSisteUttakAvslåttMedÅrsak();

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    public void skal_returnere_true_når_alle_arbeidsforholdene_er_avslått() {
        // Arrange
        SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold1 = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak.UTTAK_KUN_PÅ_HELG)
            .build();
        SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold2 = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE)
            .build();

        SvangerskapspengerUttakResultatEntitet uttakResultat = new SvangerskapspengerUttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class))
            .medUttakResultatArbeidsforhold(arbeidsforhold1)
            .medUttakResultatArbeidsforhold(arbeidsforhold2)
            .build();
        UttakResultatHolderSVP uttakResultatHolder = new UttakResultatHolderSVP(Optional.of(uttakResultat), null);

        // Act
        boolean resultat = uttakResultatHolder.kontrollerErSisteUttakAvslåttMedÅrsak();

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_sjekke_siste_periode_pr_arbeidsforhold_for_spesifikke_avslagsårsaker_og_gi_true_ved_funn() {
        // Arrange
        SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold1 = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak.INGEN)
            .medPeriode(opprettPeriode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5),
                PeriodeIkkeOppfyltÅrsak._8304)) // Er ikke siste
            .medPeriode(opprettPeriode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(1),
                PeriodeIkkeOppfyltÅrsak.INGEN))
            .build();
        SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold2 = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE)
            .medPeriode(opprettPeriode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5),
                PeriodeIkkeOppfyltÅrsak.INGEN))
            .medPeriode(opprettPeriode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(1),
                PeriodeIkkeOppfyltÅrsak._8306)) // Skal gi true
            .build();

        SvangerskapspengerUttakResultatEntitet uttakResultat = new SvangerskapspengerUttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class))
            .medUttakResultatArbeidsforhold(arbeidsforhold1)
            .medUttakResultatArbeidsforhold(arbeidsforhold2)
            .build();
        UttakResultatHolderSVP uttakResultatHolder = new UttakResultatHolderSVP(Optional.of(uttakResultat), null);

        // Act
        boolean resultat = uttakResultatHolder.kontrollerErSisteUttakAvslåttMedÅrsak();

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    public void skal_sjekke_siste_periode_pr_arbeidsforhold_for_spesifikke_avslagsårsaker_og_gi_false_når_siste_ikke_er_på_lista() {
        // Arrange
        SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold1 = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak.INGEN)
            .medPeriode(opprettPeriode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5),
                PeriodeIkkeOppfyltÅrsak._8304)) // Er ikke siste
            .medPeriode(opprettPeriode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(1),
                PeriodeIkkeOppfyltÅrsak.INGEN))
            .build();
        SvangerskapspengerUttakResultatArbeidsforholdEntitet arbeidsforhold2 = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak.ARBEIDSGIVER_KAN_TILRETTELEGGE)
            .medPeriode(opprettPeriode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5),
                PeriodeIkkeOppfyltÅrsak.INGEN))
            .medPeriode(opprettPeriode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(1),
                PeriodeIkkeOppfyltÅrsak._8311)) // Er ikke på lista
            .build();

        SvangerskapspengerUttakResultatEntitet uttakResultat = new SvangerskapspengerUttakResultatEntitet.Builder(
            mock(Behandlingsresultat.class))
            .medUttakResultatArbeidsforhold(arbeidsforhold1)
            .medUttakResultatArbeidsforhold(arbeidsforhold2)
            .build();
        UttakResultatHolderSVP uttakResultatHolder = new UttakResultatHolderSVP(Optional.of(uttakResultat), null);

        // Act
        boolean resultat = uttakResultatHolder.kontrollerErSisteUttakAvslåttMedÅrsak();

        // Assert
        assertThat(resultat).isFalse();
    }

    private SvangerskapspengerUttakResultatPeriodeEntitet opprettPeriode(LocalDate fom,
                                                                         LocalDate tom,
                                                                         PeriodeIkkeOppfyltÅrsak årsak) {
        return new SvangerskapspengerUttakResultatPeriodeEntitet
            .Builder(fom, tom)
            .medRegelInput("{}")
            .medRegelEvaluering("{}")
            .medUtbetalingsgrad(BigDecimal.valueOf(100L))
            .medPeriodeIkkeOppfyltÅrsak(årsak)
            .medPeriodeResultatType(PeriodeResultatType.AVSLÅTT)
            .build();
    }
}
