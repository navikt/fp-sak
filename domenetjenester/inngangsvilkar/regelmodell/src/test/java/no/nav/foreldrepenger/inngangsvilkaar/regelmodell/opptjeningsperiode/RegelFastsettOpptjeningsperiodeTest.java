package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp.RegelFastsettOpptjeningsperiode;

public class RegelFastsettOpptjeningsperiodeTest {


    @Test
    public void første_uttaksdato_og_skjæringsdatoOpptjening_12_uker_før_fødsel() {
        // Arrange
        var terminDato = LocalDate.of(2018, Month.MAY, 1);
        var uttaksDato = LocalDate.of(2018, Month.FEBRUARY, 6);
        var regelmodell = opprettOpptjeningsperiodeGrunnlagForMorFødsel(terminDato, terminDato, uttaksDato);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, uttaksDato);
    }

    @Test
    public void første_uttaksdato_13_uker_før_fødsel_skjæringsdatoOpptjening_12_uker_før_fødsel() {
        // Arrange
        var terminDato = LocalDate.of(2018, Month.MAY, 1);
        var uttaksDato = LocalDate.of(2018, Month.FEBRUARY, 5);
        var regelmodell = opprettOpptjeningsperiodeGrunnlagForMorFødsel(terminDato, terminDato, uttaksDato);

        // Act
        var tidligsteLovligeUttaksdato = terminDato.minusWeeks(12);
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, tidligsteLovligeUttaksdato);
    }

    @Test
    public void skalFastsetteDatoLikTermindatoMinusTreUkerMF() {
        // Arrange
        var terminDato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.JANUARY, 15);
        var regelmodell = opprettOpptjeningsperiodeGrunnlagForMorFødsel(terminDato, terminDato, uttaksDato);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, terminDato.minusWeeks(3));
    }

    @Test
    public void skalFastsetteDatoLikUttaksDatoMF() {
        // Arrange
        var terminDato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.JANUARY, 1);
        var regelmodell = opprettOpptjeningsperiodeGrunnlagForMorFødsel(terminDato, terminDato, uttaksDato);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, uttaksDato);
    }

    @Test
    public void skalFastsetteDatoLikUttaksDatoFA() {
        // Arrange
        var omsorgsDato = LocalDate.of(2018, Month.JANUARY, 15);
        var uttaksDato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.ADOPSJON, RegelSøkerRolle.FARA,
            uttaksDato, omsorgsDato, null, null, LovVersjoner.KLASSISK);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, uttaksDato);
    }

    @Test
    public void skalFastsetteDatoLikOmsorgsovertakelsesDatoFA() {
        // Arrange
        var omsorgsDato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.JANUARY, 15);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.ADOPSJON, RegelSøkerRolle.FARA,
            uttaksDato, omsorgsDato, null, null, LovVersjoner.KLASSISK);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, omsorgsDato);
    }

    @Test
    public void skalFastsetteDatoLikMorsMaksdatoPlusEnDagForFar() {
        // Arrange
        var fødselsdato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.DECEMBER, 15);
        var morsMaksDato = uttaksDato.minusDays(2);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA,
            uttaksDato, fødselsdato, fødselsdato, morsMaksDato, LovVersjoner.KLASSISK);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, morsMaksDato.plusDays(1));
    }

    @Test
    public void skalFastsetteDatoLikFødselsdatoForFar() {
        // Arrange
        var fødselsdato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.DECEMBER, 15);
        var morsMaksDato = uttaksDato.plusWeeks(7);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA,
            uttaksDato, fødselsdato, fødselsdato, morsMaksDato, LovVersjoner.KLASSISK);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, uttaksDato);
    }

    @Test
    public void skalFastsetteDatoLikFørsteUttaksdatoForFar() {
        // Arrange
        var fødselsdato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = fødselsdato.minusDays(1);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA,
            uttaksDato, fødselsdato, fødselsdato, null, LovVersjoner.KLASSISK);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato);
    }

    @Test
    public void farFørFødselUtenTermnJusterTilFødsel() {
        // Arrange
        var fødselsdato = LocalDate.of(2022, Month.AUGUST, 3);
        var uttaksDato = fødselsdato.minusWeeks(3);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA,
            uttaksDato, fødselsdato, null, null, LovVersjoner.PROP15L2122);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato);
    }

    @Test
    public void farEtterFødselBeholdUttakEnUkeEtter() {
        // Arrange
        var fødselsdato = LocalDate.of(2022, Month.AUGUST, 3);
        var uttaksDato = fødselsdato.plusWeeks(1);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA,
            uttaksDato, fødselsdato, null, null, LovVersjoner.PROP15L2122);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato.plusWeeks(1));
    }

    @Test
    public void farTerminFørFødselBeholdUttakEnUkeFør() {
        // Arrange
        var fødselsdato = LocalDate.of(2022, Month.AUGUST, 10);
        var uttaksDato = fødselsdato.minusWeeks(1);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA,
            uttaksDato, fødselsdato, fødselsdato.minusDays(2), null, LovVersjoner.PROP15L2122);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato.minusWeeks(1));
    }

    @Test
    public void farTerminFørFødselJusterTilToUker() {
        // Arrange
        var fødselsdato = LocalDate.of(2022, Month.AUGUST, 10);
        var uttaksDato = fødselsdato.minusWeeks(4);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA,
            uttaksDato, fødselsdato, fødselsdato.minusDays(2), null, LovVersjoner.PROP15L2122);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato.minusWeeks(2).minusDays(2));
    }

    @Test
    public void farTerminEtterFødselBeholdUttakEnUkeFør() {
        // Arrange
        var fødselsdato = LocalDate.of(2022, Month.AUGUST, 10);
        var uttaksDato = fødselsdato.minusWeeks(1);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA,
            uttaksDato, fødselsdato, fødselsdato.plusDays(2), null, LovVersjoner.PROP15L2122);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato.minusWeeks(1));
    }

    @Test
    public void farTerminEtterFødselJusterTilToUker() {
        // Arrange
        var fødselsdato = LocalDate.of(2022, Month.AUGUST, 10);
        var uttaksDato = fødselsdato.minusWeeks(3);
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA,
            uttaksDato, fødselsdato, fødselsdato.plusDays(2), null, LovVersjoner.PROP15L2122);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato.minusWeeks(2).plusDays(2));
    }

    private void assertSkjæringsdato(OpptjeningsPeriode resultat, LocalDate expectedSTP) {
        var tomfraregel = resultat.getOpptjeningsperiodeTom();
        var stpfraregel = tomfraregel.plusDays(1);
        assertThat(stpfraregel).isEqualTo(expectedSTP);
    }


    private OpptjeningsperiodeGrunnlag opprettOpptjeningsperiodeGrunnlagForMorFødsel(LocalDate terminDato, LocalDate hendelsesDato, LocalDate uttaksDato) {
        var regelmodell = new OpptjeningsperiodeGrunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.MORA,
            uttaksDato, hendelsesDato, terminDato, null, LovVersjoner.KLASSISK);
        return regelmodell;
    }
}
