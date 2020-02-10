package no.nav.foreldrepenger.ytelse.beregning;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.SjekkForEndringMellomAndelerOgFOM;
import no.nav.foreldrepenger.ytelse.beregning.SjekkOmPerioderInneholderSammeAndeler;

public class SjekkForEndringMellomAndelerOgFOMImplTest {

    private BeregningsresultatEntitet brFørstegangsbehandling;
    private BeregningsresultatEntitet brRevurdering;
    private SjekkForEndringMellomAndelerOgFOM sjekkForEndringMellomAndelerOgFOM;
    private SjekkOmPerioderInneholderSammeAndeler sjekkOmPerioderInneholderSammeAndeler
        = Mockito.mock(SjekkOmPerioderInneholderSammeAndeler.class);
    private LocalDate fom;
    private LocalDate tom;

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
        sjekkForEndringMellomAndelerOgFOM = new SjekkForEndringMellomAndelerOgFOM(sjekkOmPerioderInneholderSammeAndeler);
        fom = LocalDate.now();
        tom = fom.plusMonths(1);
    }

    @Test
    public void finn_ingen_endring_når_lik_FOM_og_samme_andeler_og_lik_TOM(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, fom, tom);
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, fom, tom);
        when(sjekkOmPerioderInneholderSammeAndeler.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomAndelerOgFOM.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void finn_endring_når_lik_FOM_og_ulike_andeler_og_lik_TOM(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, fom, tom);
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, fom, tom);
        when(sjekkOmPerioderInneholderSammeAndeler.sjekk(any(), any())).thenReturn(false);
        // Act
        boolean erEndring = sjekkForEndringMellomAndelerOgFOM.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void finn_endring_når_ulik_FOM_og_samme_andeler_og_lik_TOM(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, fom, tom);
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, fom.minusDays(1), tom);
        when(sjekkOmPerioderInneholderSammeAndeler.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomAndelerOgFOM.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void finn_endring_når_ulik_FOM_og_ulike_andeler_og_lik_TOM(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, fom, tom);
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, fom.minusDays(1), tom);
        when(sjekkOmPerioderInneholderSammeAndeler.sjekk(any(), any())).thenReturn(false);
        // Act
        boolean erEndring = sjekkForEndringMellomAndelerOgFOM.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void finn_ingen_endring_når_lik_FOM_og_samme_andeler_og_ulik_TOM(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, fom, tom);
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, fom, tom.plusDays(1));
        when(sjekkOmPerioderInneholderSammeAndeler.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomAndelerOgFOM.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void finn_endring_når_lik_FOM_og_ulike_andeler_og_ulik_TOM(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, fom, tom.plusDays(1));
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, fom, tom);
        when(sjekkOmPerioderInneholderSammeAndeler.sjekk(any(), any())).thenReturn(false);
        // Act
        boolean erEndring = sjekkForEndringMellomAndelerOgFOM.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void finn_endring_når_ulik_FOM_og_samme_andeler_og_ulik_TOM(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, fom, tom);
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, fom.minusDays(1), tom.minusDays(1));
        when(sjekkOmPerioderInneholderSammeAndeler.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomAndelerOgFOM.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void finn_endring_når_ulik_FOM_og_ulike_andeler_og_ulik_TOM(){
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, fom, tom.minusDays(1));
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, fom.minusDays(1), tom);
        when(sjekkOmPerioderInneholderSammeAndeler.sjekk(any(), any())).thenReturn(false);
        // Act
        boolean erEndring = sjekkForEndringMellomAndelerOgFOM.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isTrue();
    }

    private BeregningsresultatPeriode opprettPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom, LocalDate tom){
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
    }

}
