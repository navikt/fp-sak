package no.nav.foreldrepenger.ytelse.beregning.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;

class SvangerskapFeriepengeKvoteBeregnerTest {
    private static final LocalDate FØRSTE_UTTAK = LocalDate.of(2022,7,1);
    private final SvangerskapFeriepengeKvoteBeregner beregner = new SvangerskapFeriepengeKvoteBeregner(64);

    @Test
    void skal_beregne_maks_kvote_når_ingen_dager_brukt() {
        // Arrange
        var bgr = lagBgr();
        lagTYPeriode(dagerEtter(0), dagerEtter(64), bgr, true);

        // Act
        var resultat = beregner.beregn(bgr, Collections.emptyList());

        // Assert
        assertThat(resultat)
            .isPresent()
            .contains(64);
    }

    @Test
    void skal_beregne_maks_kvote_når_tidligere_ty_ikke_har_feriepenger() {
        // Arrange
        var nyYtelse = lagBgr();
        var tidligereYtelse = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(64), nyYtelse, true);
        lagTYPeriode(dagerFør(50), dagerFør(30), tidligereYtelse, false);

        // Act
        var resultat = beregner.beregn(nyYtelse, List.of(lagBrR(tidligereYtelse)));

        // Assert
        assertThat(resultat)
            .isPresent()
            .contains(64);
    }

    @Test
    void skal_ikke_trekke_fra_kvoter_på_ytelse_som_starter_etter_den_som_nå_beregnes() {
        // Arrange
        var nyYtelse = lagBgr();
        var tidligereYtelse = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerEtter(50), dagerEtter(60), tidligereYtelse, true);
        var ferie = lagFPPeriode(dagerEtter(50), dagerEtter(60));

        // Act
        var resultat = beregner.beregn(nyYtelse, List.of(lagBrR(tidligereYtelse, ferie)));

        // Assert
        assertThat(resultat)
            .isPresent()
            .contains(64);
    }

    @Test
    void skal_trekke_fra_kvote_når_en_ytelse_er_tidligere_innvilget() {
        // Arrange
        var nyYtelse = lagBgr();
        var tidligereYtelse = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerFør(50), dagerFør(40), tidligereYtelse, true); // 7 virkedager
        var ferie = lagFPPeriode(dagerFør(50), dagerFør(40));

        // Act
        var resultat = beregner.beregn(nyYtelse, List.of(lagBrR(tidligereYtelse, ferie)));

        // Assert
        assertThat(resultat)
            .isPresent()
            .contains(57);
    }

    @Test
    void skal_trekke_fra_kvote_når_to_ytelser_er_tidligere_innvilget() {
        // Arrange
        var nyYtelse = lagBgr();
        var tidligereYtelse1 = lagBgr();
        var tidligereYtelse2 = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerFør(50), dagerFør(40), tidligereYtelse1, true); // 7 virkedager
        lagTYPeriode(dagerFør(30), dagerFør(20), tidligereYtelse2, true); // 8 virkedager
        var ferie1 = lagFPPeriode(dagerFør(50), dagerFør(40));
        var ferie2 = lagFPPeriode(dagerFør(30), dagerFør(20));

        // Act
        var resultat = beregner.beregn(nyYtelse, List.of(lagBrR(tidligereYtelse1, ferie1), lagBrR(tidligereYtelse2, ferie2)));

        // Assert
        assertThat(resultat)
            .isPresent()
            .contains(49);
    }

    @Test
    void skal_trekke_fra_kvote_når_tre_ytelser_er_tidligere_innvilget() {
        // Arrange
        var nyYtelse = lagBgr();
        var tidligereYtelse1 = lagBgr();
        var tidligereYtelse2 = lagBgr();
        var tidligereYtelse3 = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerFør(50), dagerFør(40), tidligereYtelse1, true); // 7 virkedager
        lagTYPeriode(dagerFør(30), dagerFør(20), tidligereYtelse2, true); // 8 virkedager
        lagTYPeriode(dagerFør(15), dagerFør(10), tidligereYtelse3, true); // 4 virkedager
        var ferie1 = lagFPPeriode(dagerFør(50), dagerFør(40));
        var ferie2 = lagFPPeriode(dagerFør(30), dagerFør(20));
        var ferie3 = lagFPPeriode(dagerFør(15), dagerFør(10));

        // Act
        var resultat = beregner.beregn(nyYtelse, List.of(lagBrR(tidligereYtelse1, ferie1), lagBrR(tidligereYtelse2, ferie2), lagBrR(tidligereYtelse3, ferie3)));

        // Assert
        assertThat(resultat)
            .isPresent()
            .contains(45);
    }

    @Test
    void skal_ikke_beregne_når_det_ikke_er_grunnlag_for_feriepenger_på_ny_behandling() {
        // Arrange
        var nyYtelse = lagBgr();
        var tidligereYtelse1 = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, false);
        lagTYPeriode(dagerFør(150), dagerFør(100), tidligereYtelse1, true);
        var ferie1 = lagFPPeriode(dagerFør(150), dagerFør(100));

        // Act
        var resultat = beregner.beregn(nyYtelse, List.of(lagBrR(tidligereYtelse1, ferie1)));

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_kun_trekke_fra_periode_før_ny_beregnet_ytelse() {
        // Arrange
        var nyYtelse = lagBgr();
        var tidligereYtelse1 = lagBgr();
        var tidligereYtelse2 = lagBgr();
        var tidligereYtelse3 = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerFør(50), dagerFør(40), tidligereYtelse1, true); // 7 virkedager
        lagTYPeriode(dagerEtter(50), dagerEtter(60), tidligereYtelse2, true); // 7 virkedager, skal ikke trekkes fra da de kommer etter ytelse som beregnes
        lagTYPeriode(dagerFør(15), dagerFør(10), tidligereYtelse3, true); // 4 virkedager
        var ferie1 = lagFPPeriode(dagerFør(50), dagerFør(40));
        var ferie2 = lagFPPeriode(dagerFør(30), dagerFør(20));
        var ferie3 = lagFPPeriode(dagerFør(15), dagerFør(10));

        // Act
        var resultat = beregner.beregn(nyYtelse, List.of(lagBrR(tidligereYtelse1, ferie1), lagBrR(tidligereYtelse2, ferie2), lagBrR(tidligereYtelse3, ferie3)));

        // Assert
        assertThat(resultat)
            .isPresent()
            .contains(53);
    }

    @Test
    void skal_trekke_fra_kvote_når_en_ytelse_er_tidligere_innvilget_delt_periode() {
        // Arrange
        var nyYtelse = lagBgr();
        var tidligereYtelse = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerFør(50), dagerFør(40), tidligereYtelse, true); // 7 virkedager
        lagTYPeriode(dagerFør(30), dagerFør(20), tidligereYtelse, true); // 8 virkedager
        var ferie = lagFPPeriode(dagerFør(50), dagerFør(20)); // Ferieperiode dekker opphold i ytelsen

        // Act
        var resultat = beregner.beregn(nyYtelse, List.of(lagBrR(tidligereYtelse, ferie)));

        // Assert
        assertThat(resultat)
            .isPresent()
            .contains(49);
    }

    /**
     * De to tidligere ytelsen bryter ikke kvoten hver for seg, men gjør det tilsammen.
     * Beregning av en tredje ytelse bør gi feil siden kvoten allerede er oppbrukt på tidligere saker
     */
    @Test
    void skal_kaste_feil_om_feriekvote_overskrider_tillatt_sum_i_tidligere_behandlinger() {
        // Arrange
        var nyYtelse = lagBgr();
        var tidligereYtelse1 = lagBgr();
        var tidligereYtelse2 = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerFør(100), dagerFør(50), tidligereYtelse1, true); // 37 virkedager
        lagTYPeriode(dagerFør(49), dagerFør(20), tidligereYtelse2, true); // 21 virkedager
        lagTYPeriode(dagerFør(19), dagerFør(5), tidligereYtelse2, true); // 10 virkedager
        var ferie1 = lagFPPeriode(dagerFør(100), dagerFør(50)); // Ferieperiode dekker opphold i ytelsen
        var ferie2 = lagFPPeriode(dagerFør(49), dagerFør(5)); // Ferieperiode dekker opphold i ytelsen

        // Act
        Exception exception = assertThrows(IllegalStateException.class,
            () -> beregner.beregn(nyYtelse, List.of(lagBrR(tidligereYtelse1, ferie1), lagBrR(tidligereYtelse2, ferie2))));

        var forventetFeilmelding = "Brukte feriedager overstiger kvote! Tidligere saker må revurderes først. Brukte feriedager var 68";

        assertThat(exception.getMessage()).isEqualTo(forventetFeilmelding);
    }


    private LocalDate dagerFør(int i) {
        return FØRSTE_UTTAK.minusDays(i);
    }


    private BeregningsresultatEntitet lagBgr() {
        return BeregningsresultatEntitet.builder().medRegelInput("").medRegelSporing("").build();
    }

    private BehandlingBeregningsresultatEntitet lagBrR(BeregningsresultatEntitet beregningsresultatEntitet) {
        return lagBrR(beregningsresultatEntitet, null);
    }

    private BehandlingBeregningsresultatEntitet lagBrR(BeregningsresultatEntitet beregningsresultat, BeregningsresultatFeriepenger ferie) {
        return BehandlingBeregningsresultatBuilder.oppdatere(Optional.empty())
            .medBgBeregningsresultatFP(beregningsresultat)
            .medBeregningsresultatFeriepenger(ferie)
            .build(1L);
    }

    private BeregningsresultatFeriepenger lagFPPeriode(LocalDate fom, LocalDate tom) {
        return BeregningsresultatFeriepenger.builder().medFeriepengerPeriodeFom(fom).medFeriepengerPeriodeTom(tom).medFeriepengerRegelInput("").medFeriepengerRegelSporing("").build();
    }

    private LocalDate dagerEtter(int i) {
        return FØRSTE_UTTAK.plusDays(i);
    }

    private void lagTYPeriode(LocalDate fom, LocalDate tom, BeregningsresultatEntitet bgr, boolean medFeriepenger) {
        var brp = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(fom, tom).build(bgr);
        if (medFeriepenger) {
            lagAndel(AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, 100).build(brp);
        } else {
            lagAndel(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE, 100).build(brp);
        }
    }

    private BeregningsresultatAndel.Builder lagAndel(AktivitetStatus status, Inntektskategori kategori, int dagsats) {
        return BeregningsresultatAndel.builder()
            .medAktivitetStatus(status)
            .medInntektskategori(kategori)
            .medDagsats(dagsats)
            .medBrukerErMottaker(true)
            .medDagsatsFraBg(dagsats)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medStillingsprosent(BigDecimal.ONE);
    }

}
