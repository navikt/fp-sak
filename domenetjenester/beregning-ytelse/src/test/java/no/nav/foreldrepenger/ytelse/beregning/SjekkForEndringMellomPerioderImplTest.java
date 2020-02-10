package no.nav.foreldrepenger.ytelse.beregning;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.SjekkForEndringMellomAndelerOgFOM;
import no.nav.foreldrepenger.ytelse.beregning.SjekkForEndringMellomPerioder;
import no.nav.foreldrepenger.ytelse.beregning.SjekkForIngenAndelerOgAndelerUtenDagsats;

public class SjekkForEndringMellomPerioderImplTest {

    private static final LocalDate IDAG = LocalDate.now();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private SjekkForEndringMellomPerioder sjekkForEndringMellomPerioder;
    private BeregningsresultatEntitet brFørstegangsbehandling;
    private BeregningsresultatEntitet brRevurdering;
    private SjekkForIngenAndelerOgAndelerUtenDagsats sjekkForIngenAndelerOgAndelerUtenDagsats
        = Mockito.mock(SjekkForIngenAndelerOgAndelerUtenDagsats.class);
    private SjekkForEndringMellomAndelerOgFOM sjekkForEndringMellomAndelerOgFOM
        = Mockito.mock(SjekkForEndringMellomAndelerOgFOM.class);

    @Before
    public void oppsett(){
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
            sjekkForEndringMellomAndelerOgFOM
        );
    }

    @Test
    public void skal_kaste_exception_når_både_ny_og_gammel_periode_er_lik_null(){
        // Arrange
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Utviklerfeil: Både ny og gammel periode kan ikke være null");
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(null, null);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void ingen_endring_med_ingen_andel_eller_andel_uten_dagsats_og_gammelPeriode_og_ingen_nyPeriode(){
        // Arrange
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, IDAG);
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(true);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(null, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void endring_med_andel_eller_andel_med_dagsats_og_gammelPeriode_og_ingen_nyPeriode(){
        // Arrange
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, IDAG);
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(false);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(null, gammel);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void ingen_endring_med_ingen_andel_eller_andel_uten_dagsats_og_nyPeriode_og_ingen_gammelPeriode(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, IDAG);
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(true);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, null);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void endring_med_andel_eller_andel_med_dagsats_og_nyPeriode_og_ingen_gammelPeriode(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, IDAG);
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(false);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, null);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void ingen_endring_med_ingen_andel_eller_andel_uten_dagsats_med_ny_og_gammel_periode_med_lik_fom_og_andeler(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, IDAG);
        BeregningsresultatPeriode gammel = opprettPeriode(brRevurdering, IDAG);
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(true);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(false);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void ingen_endring_med_andel_eller_andel_med_dagsats_med_ny_og_gammel_periode_med_lik_fom_og_andeler(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, IDAG);
        BeregningsresultatPeriode gammel = opprettPeriode(brRevurdering, IDAG);
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(false);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(false);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void ingen_endring_med_ingen_andel_eller_andel_uten_dagsats_med_ny_og_gammel_periode_med_ulik_fom_og_andeler(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, IDAG);
        BeregningsresultatPeriode gammel = opprettPeriode(brRevurdering, IDAG.plusDays(1));
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(true);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void endring_med_andel_eller_andel_med_dagsats_med_ny_og_gammel_periode_med_ulik_fom_og_andeler(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, IDAG);
        BeregningsresultatPeriode gammel = opprettPeriode(brRevurdering, IDAG.plusDays(1));
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(false);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isTrue();
    }

    private BeregningsresultatPeriode opprettPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom){
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, IDAG.plusMonths(1))
            .build(beregningsresultat);
    }

}
