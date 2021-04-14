package no.nav.foreldrepenger.ytelse.beregning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

public class FinnEndringsdatoMellomPeriodeListerTest {

    private static final LocalDate IDAG = LocalDate.now();

    private FinnEndringsdatoMellomPeriodeLister finnEndringsdatoMellomPeriodeLister;
    private SjekkForEndringMellomPerioder sjekkForEndringMellomPerioder = Mockito.mock(SjekkForEndringMellomPerioder.class);
    private BeregningsresultatEntitet brFørstegangsbehandling;
    private BeregningsresultatEntitet brRevurdering;

    @BeforeEach
    public void oppsett() {
        finnEndringsdatoMellomPeriodeLister = new FinnEndringsdatoMellomPeriodeLister(
                sjekkForEndringMellomPerioder);
        brRevurdering = BeregningsresultatEntitet.builder()
                .medRegelInput("clob1")
                .medRegelSporing("clob2")
                .build();
        brFørstegangsbehandling = BeregningsresultatEntitet.builder()
                .medRegelInput("clob1")
                .medRegelSporing("clob2")
                .build();
    }

    @Test
    public void skal_finne_endringsdato_og_sorterer_perioder_som_ikke_kommer_kronologisk() {

        // Arrange : førstegangsbehandling
        var p1 = opprettPeriode(brFørstegangsbehandling, IDAG, IDAG.plusDays(3));
        var p2 = opprettPeriode(brFørstegangsbehandling, IDAG.plusDays(4), IDAG.plusDays(7));

        // Arrange : revurdering
        var p3 = opprettPeriode(brRevurdering, IDAG, IDAG.plusDays(3));
        var p4 = opprettPeriode(brRevurdering, IDAG.plusDays(4), IDAG.plusDays(7));

        when(sjekkForEndringMellomPerioder.sjekk(any(), any())).thenReturn(true, false);

        // Act
        var endringsdato = finnEndringsdatoMellomPeriodeLister.finnEndringsdato(
                List.of(p2, p1), List.of(p4, p3));

        // Assert
        assertThat(endringsdato).hasValueSatisfying(ed -> assertThat(ed).isEqualTo(IDAG));

    }

    @Test
    public void skal_finne_endringsdato_og_sorterer_perioder_som_kommer_kronologisk() {

        // Arrange : førstegangsbehandling
        var p1 = opprettPeriode(brFørstegangsbehandling, IDAG, IDAG.plusDays(3));
        var p2 = opprettPeriode(brFørstegangsbehandling, IDAG.plusDays(4), IDAG.plusDays(7));

        // Arrange : revurdering
        var p3 = opprettPeriode(brRevurdering, IDAG, IDAG.plusDays(3));
        var p4 = opprettPeriode(brRevurdering, IDAG.plusDays(4), IDAG.plusDays(7));

        when(sjekkForEndringMellomPerioder.sjekk(any(), any())).thenReturn(false, true);

        // Act
        var endringsdato = finnEndringsdatoMellomPeriodeLister.finnEndringsdato(
                List.of(p1, p2), List.of(p3, p4));

        // Assert
        assertThat(endringsdato).hasValueSatisfying(ed -> assertThat(ed).isEqualTo(IDAG.plusDays(4)));

    }

    @Test
    public void skal_finne_tom_endringsdato_og_sorterer_perioder_som_ikke_kommer_kronologisk() {

        // Arrange : førstegangsbehandling
        var p1 = opprettPeriode(brFørstegangsbehandling, IDAG, IDAG.plusDays(3));
        var p2 = opprettPeriode(brFørstegangsbehandling, IDAG.plusDays(4), IDAG.plusDays(7));

        // Arrange : revurdering
        var p3 = opprettPeriode(brRevurdering, IDAG, IDAG.plusDays(3));
        var p4 = opprettPeriode(brRevurdering, IDAG.plusDays(4), IDAG.plusDays(7));

        when(sjekkForEndringMellomPerioder.sjekk(any(), any())).thenReturn(false, false);

        // Act
        var endringsdato = finnEndringsdatoMellomPeriodeLister.finnEndringsdato(
                List.of(p2, p1), List.of(p3, p4));

        // Assert
        assertThat(endringsdato).isEmpty();

    }

    @Test
    public void finn_endringsdato_hvor_revurderingen_kommer_kronologisk_og_førstegangsbehandlingen_kommer_ikke_kronologisk_med_endring_kun_i_andre_periode() {

        // Arrange : førstegangsbehandling
        var p1 = opprettPeriode(brFørstegangsbehandling, IDAG, IDAG.plusDays(5));
        var p2 = opprettPeriode(brFørstegangsbehandling, IDAG.plusDays(6), IDAG.plusDays(9));

        // Arrange : revurdering
        var p3 = opprettPeriode(brRevurdering, IDAG, IDAG.plusDays(5));
        var p4 = opprettPeriode(brRevurdering, IDAG.plusDays(6), IDAG.plusDays(9));

        when(sjekkForEndringMellomPerioder.sjekk(any(), any())).thenReturn(false, true);

        // Act
        var endringsdato = finnEndringsdatoMellomPeriodeLister.finnEndringsdato(
                List.of(p1, p2), List.of(p4, p3));

        // Assert
        assertThat(endringsdato).hasValueSatisfying(ed -> assertThat(ed).isEqualTo(IDAG.plusDays(6)));

    }

    private BeregningsresultatPeriode opprettPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom, LocalDate tom) {
        return BeregningsresultatPeriode.builder()
                .medBeregningsresultatPeriodeFomOgTom(fom, tom)
                .build(beregningsresultat);
    }

}
