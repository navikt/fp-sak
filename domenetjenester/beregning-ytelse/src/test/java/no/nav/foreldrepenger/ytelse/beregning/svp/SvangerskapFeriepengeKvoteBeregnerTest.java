package no.nav.foreldrepenger.ytelse.beregning.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
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
        BeregningsresultatEntitet bgr = lagBgr();
        lagTYPeriode(dagerEtter(0), dagerEtter(64), bgr, true);

        // Act
        var resultat = beregner.beregn(bgr, Collections.emptyList());

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(64);
    }

    @Test
    void skal_beregne_maks_kvote_når_tidligere_ty_ikke_har_feriepenger() {
        // Arrange
        BeregningsresultatEntitet nyYtelse = lagBgr();
        BeregningsresultatEntitet tidligereYtelse = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(64), nyYtelse, true);
        lagTYPeriode(dagerFør(50), dagerFør(30), tidligereYtelse, false);

        // Act
        var resultat = beregner.beregn(nyYtelse, Arrays.asList(tidligereYtelse));

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(64);
    }

    @Test
    void skal_ikke_trekke_fra_kvoter_på_ytelse_som_starter_etter_den_som_nå_beregnes() {
        // Arrange
        BeregningsresultatEntitet nyYtelse = lagBgr();
        BeregningsresultatEntitet tidligereYtelse = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerEtter(50), dagerEtter(60), tidligereYtelse, true);
        lagFPPeriode(dagerEtter(50), dagerEtter(60), tidligereYtelse);

        // Act
        var resultat = beregner.beregn(nyYtelse, Arrays.asList(tidligereYtelse));

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(64);
    }

    @Test
    void skal_trekke_fra_kvote_når_en_ytelse_er_tidligere_innvilget() {
        // Arrange
        BeregningsresultatEntitet nyYtelse = lagBgr();
        BeregningsresultatEntitet tidligereYtelse = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerFør(50), dagerFør(40), tidligereYtelse, true); // 7 virkedager
        lagFPPeriode(dagerFør(50), dagerFør(40), tidligereYtelse);

        // Act
        var resultat = beregner.beregn(nyYtelse, Arrays.asList(tidligereYtelse));

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(57);
    }

    @Test
    void skal_trekke_fra_kvote_når_to_ytelser_er_tidligere_innvilget() {
        // Arrange
        BeregningsresultatEntitet nyYtelse = lagBgr();
        BeregningsresultatEntitet tidligereYtelse1 = lagBgr();
        BeregningsresultatEntitet tidligereYtelse2 = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerFør(50), dagerFør(40), tidligereYtelse1, true); // 7 virkedager
        lagTYPeriode(dagerFør(30), dagerFør(20), tidligereYtelse2, true); // 8 virkedager
        lagFPPeriode(dagerFør(50), dagerFør(40), tidligereYtelse1);
        lagFPPeriode(dagerFør(30), dagerFør(20), tidligereYtelse2);

        // Act
        var resultat = beregner.beregn(nyYtelse, Arrays.asList(tidligereYtelse1, tidligereYtelse2));

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(49);
    }

    @Test
    void skal_trekke_fra_kvote_når_tre_ytelser_er_tidligere_innvilget() {
        // Arrange
        BeregningsresultatEntitet nyYtelse = lagBgr();
        BeregningsresultatEntitet tidligereYtelse1 = lagBgr();
        BeregningsresultatEntitet tidligereYtelse2 = lagBgr();
        BeregningsresultatEntitet tidligereYtelse3 = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerFør(50), dagerFør(40), tidligereYtelse1, true); // 7 virkedager
        lagTYPeriode(dagerFør(30), dagerFør(20), tidligereYtelse2, true); // 8 virkedager
        lagTYPeriode(dagerFør(15), dagerFør(10), tidligereYtelse3, true); // 4 virkedager
        lagFPPeriode(dagerFør(50), dagerFør(40), tidligereYtelse1);
        lagFPPeriode(dagerFør(30), dagerFør(20), tidligereYtelse2);
        lagFPPeriode(dagerFør(15), dagerFør(10), tidligereYtelse3);

        // Act
        var resultat = beregner.beregn(nyYtelse, Arrays.asList(tidligereYtelse1, tidligereYtelse2, tidligereYtelse3));

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(45);
    }

    @Test
    void skal_ikke_beregne_når_det_ikke_er_grunnlag_for_feriepenger_på_ny_behandling() {
        // Arrange
        BeregningsresultatEntitet nyYtelse = lagBgr();
        BeregningsresultatEntitet tidligereYtelse1 = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, false);
        lagTYPeriode(dagerFør(150), dagerFør(100), tidligereYtelse1, true);
        lagFPPeriode(dagerFør(150), dagerFør(100), tidligereYtelse1);

        // Act
        var resultat = beregner.beregn(nyYtelse, Arrays.asList(tidligereYtelse1));

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_kun_trekke_fra_periode_før_ny_beregnet_ytelse() {
        // Arrange
        BeregningsresultatEntitet nyYtelse = lagBgr();
        BeregningsresultatEntitet tidligereYtelse1 = lagBgr();
        BeregningsresultatEntitet tidligereYtelse2 = lagBgr();
        BeregningsresultatEntitet tidligereYtelse3 = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerFør(50), dagerFør(40), tidligereYtelse1, true); // 7 virkedager
        lagTYPeriode(dagerEtter(50), dagerEtter(60), tidligereYtelse2, true); // 7 virkedager, skal ikke trekkes fra da de kommer etter ytelse som beregnes
        lagTYPeriode(dagerFør(15), dagerFør(10), tidligereYtelse3, true); // 4 virkedager
        lagFPPeriode(dagerFør(50), dagerFør(40), tidligereYtelse1);
        lagFPPeriode(dagerFør(30), dagerFør(20), tidligereYtelse2);
        lagFPPeriode(dagerFør(15), dagerFør(10), tidligereYtelse3);

        // Act
        var resultat = beregner.beregn(nyYtelse, Arrays.asList(tidligereYtelse1, tidligereYtelse2, tidligereYtelse3));

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(53);
    }

    @Test
    void skal_trekke_fra_kvote_når_en_ytelse_er_tidligere_innvilget_delt_periode() {
        // Arrange
        BeregningsresultatEntitet nyYtelse = lagBgr();
        BeregningsresultatEntitet tidligereYtelse = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerFør(50), dagerFør(40), tidligereYtelse, true); // 7 virkedager
        lagTYPeriode(dagerFør(30), dagerFør(20), tidligereYtelse, true); // 8 virkedager
        lagFPPeriode(dagerFør(50), dagerFør(20), tidligereYtelse); // Ferieperiode dekker opphold i ytelsen

        // Act
        var resultat = beregner.beregn(nyYtelse, Arrays.asList(tidligereYtelse));

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(49);
    }

    /**
     * De to tidligere ytelsen bryter ikke kvoten hver for seg, men gjør det tilsammen.
     * Beregning av en tredje ytelse bør gi feil siden kvoten allerede er oppbrukt på tidligere saker
     */
    @Test
    void skal_kaste_feil_om_feriekvote_overskrider_tillatt_sum_i_tidligere_behandlinger() {
        // Arrange
        BeregningsresultatEntitet nyYtelse = lagBgr();
        BeregningsresultatEntitet tidligereYtelse1 = lagBgr();
        BeregningsresultatEntitet tidligereYtelse2 = lagBgr();

        lagTYPeriode(dagerEtter(0), dagerEtter(20), nyYtelse, true);
        lagTYPeriode(dagerFør(100), dagerFør(50), tidligereYtelse1, true); // 37 virkedager
        lagTYPeriode(dagerFør(49), dagerFør(20), tidligereYtelse2, true); // 21 virkedager
        lagTYPeriode(dagerFør(19), dagerFør(5), tidligereYtelse2, true); // 10 virkedager
        lagFPPeriode(dagerFør(100), dagerFør(50), tidligereYtelse1); // Ferieperiode dekker opphold i ytelsen
        lagFPPeriode(dagerFør(49), dagerFør(5), tidligereYtelse2); // Ferieperiode dekker opphold i ytelsen

        // Act
        Exception exception = assertThrows(IllegalStateException.class, () -> beregner.beregn(nyYtelse, Arrays.asList(tidligereYtelse1, tidligereYtelse2)));

        String forventetFeilmelding = "Brukte feriedager overstiger kvote! Tidligere saker må revurderes først. Brukte feriedager var 68";

        assertThat(exception.getMessage()).isEqualTo(forventetFeilmelding);
    }


    private LocalDate dagerFør(int i) {
        return FØRSTE_UTTAK.minusDays(i);
    }


    private BeregningsresultatEntitet lagBgr() {
        return BeregningsresultatEntitet.builder().medRegelInput("").medRegelSporing("").build();
    }

    private void lagFPPeriode(LocalDate fom, LocalDate tom, BeregningsresultatEntitet bgr) {
        BeregningsresultatFeriepenger.builder().medFeriepengerPeriodeFom(fom).medFeriepengerPeriodeTom(tom).medFeriepengerRegelInput("").medFeriepengerRegelSporing("").build(bgr);
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
