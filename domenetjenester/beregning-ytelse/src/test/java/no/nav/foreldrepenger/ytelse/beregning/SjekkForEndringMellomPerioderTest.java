package no.nav.foreldrepenger.ytelse.beregning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

public class SjekkForEndringMellomPerioderTest {

    private SjekkForEndringMellomPerioder sjekkForEndringMellomPerioder;
    private BeregningsresultatEntitet brFørstegangsbehandling;
    private BeregningsresultatEntitet brRevurdering;
    private final SjekkForIngenAndelerOgAndelerUtenDagsats sjekkForIngenAndelerOgAndelerUtenDagsats =
        mock(SjekkForIngenAndelerOgAndelerUtenDagsats.class);
    private final SjekkOmPerioderHarEndringIAndeler sjekkOmPerioderHarEndringIAndeler =
        mock(SjekkOmPerioderHarEndringIAndeler.class);

    @BeforeEach
    void oppsett(){
        brRevurdering = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        brFørstegangsbehandling = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        sjekkForEndringMellomPerioder = new SjekkForEndringMellomPerioder(
            sjekkForIngenAndelerOgAndelerUtenDagsats,
            sjekkOmPerioderHarEndringIAndeler
        );
    }

    @Test
    public void skal_kaste_exception_når_både_ny_og_gammel_periode_er_lik_null(){
        assertThatThrownBy(() -> sjekkForEndringMellomPerioder.sjekk(null, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Utviklerfeil: Både ny og gammel periode kan ikke være null");
    }

    @Test
    public void ingen_endring_med_ingen_andel_eller_andel_uten_dagsats_og_gammelPeriode_og_ingen_nyPeriode(){
        // Arrange
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, LocalDate.now());
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(true);
        when(sjekkOmPerioderHarEndringIAndeler.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(null, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void endring_med_andel_eller_andel_med_dagsats_og_gammelPeriode_og_ingen_nyPeriode(){
        // Arrange
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, LocalDate.now());
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(false);
        when(sjekkOmPerioderHarEndringIAndeler.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(null, gammel);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void ingen_endring_med_ingen_andel_eller_andel_uten_dagsats_og_nyPeriode_og_ingen_gammelPeriode(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, LocalDate.now());
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(true);
        when(sjekkOmPerioderHarEndringIAndeler.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, null);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void endring_med_andel_eller_andel_med_dagsats_og_nyPeriode_og_ingen_gammelPeriode(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, LocalDate.now());
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(false);
        when(sjekkOmPerioderHarEndringIAndeler.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, null);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void ingen_endring_med_ingen_andel_eller_andel_uten_dagsats_med_ny_og_gammel_periode_med_lik_fom_og_andeler(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, LocalDate.now());
        BeregningsresultatPeriode gammel = opprettPeriode(brRevurdering, LocalDate.now());
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(true);
        when(sjekkOmPerioderHarEndringIAndeler.sjekk(any(), any())).thenReturn(false);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void ingen_endring_med_andel_eller_andel_med_dagsats_med_ny_og_gammel_periode_med_lik_fom_og_andeler(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, LocalDate.now());
        BeregningsresultatPeriode gammel = opprettPeriode(brRevurdering, LocalDate.now());
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(false);
        when(sjekkOmPerioderHarEndringIAndeler.sjekk(any(), any())).thenReturn(false);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void ingen_endring_med_ingen_andel_eller_andel_uten_dagsats_med_ny_og_gammel_periode_med_ulik_fom_og_andeler(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, LocalDate.now());
        BeregningsresultatPeriode gammel = opprettPeriode(brRevurdering, LocalDate.now().plusDays(1));
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(true);
        when(sjekkOmPerioderHarEndringIAndeler.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void endring_med_andel_eller_andel_med_dagsats_med_ny_og_gammel_periode_med_ulik_fom_og_andeler(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, LocalDate.now());
        BeregningsresultatPeriode gammel = opprettPeriode(brRevurdering, LocalDate.now().plusDays(1));
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(false);
        when(sjekkOmPerioderHarEndringIAndeler.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isTrue();
    }

    private BeregningsresultatPeriode opprettPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom){
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, LocalDate.now().plusMonths(1))
            .build(beregningsresultat);
    }

}
