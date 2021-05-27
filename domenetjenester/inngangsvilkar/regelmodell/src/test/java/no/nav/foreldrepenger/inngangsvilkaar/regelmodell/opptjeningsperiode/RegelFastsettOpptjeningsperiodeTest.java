package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.FagsakÅrsak;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsPeriode;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsperiodeGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp.RegelFastsettOpptjeningsperiode;

public class RegelFastsettOpptjeningsperiodeTest {


    @Test
    public void første_uttaksdato_og_skjæringsdatoOpptjening_12_uker_før_fødsel() {
        // Arrange
        var terminDato = LocalDate.of(2018, Month.MAY, 1);
        var uttaksDato = LocalDate.of(2018, Month.FEBRUARY, 6);
        var regelmodell = opprettOpptjeningsperiodeGrunnlagForMorFødsel(terminDato, terminDato, uttaksDato);

        // Act
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, new OpptjeningsPeriode());

        // Assert
        assertThat(regelmodell.getSkjæringsdatoOpptjening()).isEqualTo(uttaksDato);
    }

    @Test
    public void første_uttaksdato_13_uker_før_fødsel_skjæringsdatoOpptjening_12_uker_før_fødsel() {
        // Arrange
        var terminDato = LocalDate.of(2018, Month.MAY, 1);
        var uttaksDato = LocalDate.of(2018, Month.FEBRUARY, 5);
        var regelmodell = opprettOpptjeningsperiodeGrunnlagForMorFødsel(terminDato, terminDato, uttaksDato);

        // Act
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, new OpptjeningsPeriode());

        // Assert
        var tidligsteLovligeUttaksdato = terminDato.minusWeeks(12);
        assertThat(regelmodell.getSkjæringsdatoOpptjening()).isEqualTo(tidligsteLovligeUttaksdato);
    }

    @Test
    public void skalFastsetteDatoLikTermindatoMinusTreUkerMF() {
        // Arrange
        var terminDato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.JANUARY, 15);
        var regelmodell = opprettOpptjeningsperiodeGrunnlagForMorFødsel(terminDato, terminDato, uttaksDato);

        // Act
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, new OpptjeningsPeriode());

        // Assert
        assertThat(regelmodell.getSkjæringsdatoOpptjening()).isEqualTo(terminDato.minusWeeks(3));
        //assertThat(evaluation.getEvaluationProperties()).isNotEmpty();
    }

    @Test
    public void skalFastsetteDatoLikUttaksDatoMF() {
        // Arrange
        var terminDato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.JANUARY, 1);
        var regelmodell = opprettOpptjeningsperiodeGrunnlagForMorFødsel(terminDato, terminDato, uttaksDato);

        // Act
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, new OpptjeningsPeriode());
        // Assert

        assertThat(regelmodell.getSkjæringsdatoOpptjening()).isEqualTo(uttaksDato);
    }

    @Test
    public void skalFastsetteDatoLikUttaksDatoFA() {
        // Arrange
        var omsorgsDato = LocalDate.of(2018, Month.JANUARY, 15);
        var uttaksDato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.ADOPSJON, RegelSøkerRolle.FARA,
            uttaksDato, omsorgsDato, null);
        regelmodell.setPeriodeLengde(Period.parse("P10M"));

        // Act
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, new OpptjeningsPeriode());

        // Assert
        assertThat(regelmodell.getSkjæringsdatoOpptjening()).isEqualTo(uttaksDato);
    }

    @Test
    public void skalFastsetteDatoLikOmsorgsovertakelsesDatoFA() {
        // Arrange
        var omsorgsDato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.JANUARY, 15);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.ADOPSJON, RegelSøkerRolle.FARA,
            uttaksDato, omsorgsDato, null);
        regelmodell.setPeriodeLengde(Period.parse("P10M"));

        // Act
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, new OpptjeningsPeriode());
        // Assert
        assertThat(regelmodell.getSkjæringsdatoOpptjening()).isEqualTo(omsorgsDato);
    }

    @Test
    public void skalFastsetteDatoLikMorsMaksdatoPlusEnDagForFar() {
        // Arrange
        var fødselsdato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.DECEMBER, 15);
        var morsMaksDato = uttaksDato.minusDays(2);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA,
            uttaksDato, fødselsdato, fødselsdato);
        regelmodell.setPeriodeLengde(Period.parse("P10M"));
        regelmodell.setMorsMaksdato(morsMaksDato);

        // Act
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, new OpptjeningsPeriode());
        // Assert
        assertThat(regelmodell.getSkjæringsdatoOpptjening()).isEqualTo(morsMaksDato.plusDays(1));
    }

    @Test
    public void skalFastsetteDatoLikFødselsdatoForFar() {
        // Arrange
        var fødselsdato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.DECEMBER, 15);
        var morsMaksDato = uttaksDato.plusWeeks(7);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA,
            uttaksDato, fødselsdato, fødselsdato);
        regelmodell.setPeriodeLengde(Period.parse("P10M"));
        regelmodell.setMorsMaksdato(morsMaksDato);

        // Act
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, new OpptjeningsPeriode());
        // Assert
        assertThat(regelmodell.getSkjæringsdatoOpptjening()).isEqualTo(uttaksDato);
    }

    @Test
    public void skalFastsetteDatoLikFørsteUttaksdatoForFar() {
        // Arrange
        var fødselsdato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = fødselsdato.minusDays(1);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA,
            uttaksDato, fødselsdato, fødselsdato);
        regelmodell.setPeriodeLengde(Period.parse("P10M"));

        // Act
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, new OpptjeningsPeriode());
        // Assert
        assertThat(regelmodell.getSkjæringsdatoOpptjening()).isEqualTo(fødselsdato);
    }


    private OpptjeningsperiodeGrunnlag opprettOpptjeningsperiodeGrunnlagForMorFødsel(LocalDate terminDato, LocalDate hendelsesDato, LocalDate uttaksDato) {
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.MORA,
            uttaksDato, hendelsesDato, terminDato);
        regelmodell.setPeriodeLengde(Period.parse("P10M"));
        regelmodell.setTidligsteUttakFørFødselPeriode(Period.parse("P12W"));
        return regelmodell;
    }
}
