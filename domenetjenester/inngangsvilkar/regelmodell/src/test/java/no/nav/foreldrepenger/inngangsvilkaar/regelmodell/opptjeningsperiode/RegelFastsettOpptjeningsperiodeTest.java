package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.RegelSøkerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjeningsperiode.fp.RegelFastsettOpptjeningsperiode;

class RegelFastsettOpptjeningsperiodeTest {


    @Test
    void første_uttaksdato_og_skjæringsdatoOpptjening_12_uker_før_fødsel() {
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
    void første_uttaksdato_13_uker_før_fødsel_skjæringsdatoOpptjening_12_uker_før_fødsel() {
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
    void skalFastsetteDatoLikTermindatoMinusTreUkerMF() {
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
    void skalFastsetteDatoLikUttaksDatoMF() {
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
    void skalFastsetteDatoLikUttaksDatoFA() {
        // Arrange
        var omsorgsDato = LocalDate.of(2018, Month.JANUARY, 15);
        var uttaksDato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var regelmodell = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.ADOPSJON, RegelSøkerRolle.FARA, LovVersjoner.KLASSISK)
            .medFørsteUttaksDato(uttaksDato)
            .medHendelsesDato(omsorgsDato);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, uttaksDato);
    }

    @Test
    void skalFastsetteDatoLikOmsorgsovertakelsesDatoFA() {
        // Arrange
        var omsorgsDato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.JANUARY, 15);
        var regelmodell = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.ADOPSJON, RegelSøkerRolle.FARA, LovVersjoner.KLASSISK)
            .medFørsteUttaksDato(uttaksDato)
            .medHendelsesDato(omsorgsDato);


        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, omsorgsDato);
    }

    @Test
    void skalFastsetteDatoLikMorsMaksdatoPlusEnDagForFar() {
        // Arrange
        var fødselsdato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.DECEMBER, 15);
        var morsMaksDato = uttaksDato.minusDays(2);
        var regelmodell = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA, LovVersjoner.KLASSISK)
            .medFørsteUttaksDato(uttaksDato)
            .medHendelsesDato(fødselsdato)
            .medTerminDato(fødselsdato)
            .medMorsMaksdato(morsMaksDato);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, morsMaksDato.plusDays(1));
    }

    @Test
    void skalFastsetteDatoLikFødselsdatoForFar() {
        // Arrange
        var fødselsdato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = LocalDate.of(2018, Month.DECEMBER, 15);
        var morsMaksDato = uttaksDato.plusWeeks(7);
        var regelmodell = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA, LovVersjoner.KLASSISK)
            .medFørsteUttaksDato(uttaksDato)
            .medHendelsesDato(fødselsdato)
            .medTerminDato(fødselsdato)
            .medMorsMaksdato(morsMaksDato);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, uttaksDato);
    }

    @Test
    void skalFastsetteDatoLikFørsteUttaksdatoForFar() {
        // Arrange
        var fødselsdato = LocalDate.of(2018, Month.FEBRUARY, 1);
        var uttaksDato = fødselsdato.minusDays(1);
        var regelmodell = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA, LovVersjoner.KLASSISK)
            .medFørsteUttaksDato(uttaksDato)
            .medHendelsesDato(fødselsdato)
            .medTerminDato(fødselsdato);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato);
    }

    @Test
    void farFørFødselUtenTermnJusterTilFødsel() {
        // Arrange
        var fødselsdato = LocalDate.of(2022, Month.AUGUST, 3);
        var uttaksDato = fødselsdato.minusWeeks(3);
        var regelmodell = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA, LovVersjoner.PROP15L2122)
            .medFørsteUttaksDato(uttaksDato)
            .medHendelsesDato(fødselsdato);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato);
    }

    @Test
    void farEtterFødselBeholdUttakEnUkeEtter() {
        // Arrange
        var fødselsdato = LocalDate.of(2022, Month.AUGUST, 3);
        var uttaksDato = fødselsdato.plusWeeks(1);
        var regelmodell = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA, LovVersjoner.PROP15L2122)
            .medFørsteUttaksDato(uttaksDato)
            .medHendelsesDato(fødselsdato);

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato.plusWeeks(1));
    }

    @Test
    void farTerminFørFødselBeholdUttakEnUkeFør() {
        // Arrange
        var fødselsdato = LocalDate.of(2022, Month.AUGUST, 10);
        var uttaksDato = fødselsdato.minusWeeks(1);
        var regelmodell = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA, LovVersjoner.PROP15L2122)
            .medFørsteUttaksDato(uttaksDato)
            .medHendelsesDato(fødselsdato)
            .medTerminDato(fødselsdato.minusDays(2));

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato.minusWeeks(1));
    }

    @Test
    void farTerminFørFødselJusterTilToUker() {
        // Arrange
        var fødselsdato = LocalDate.of(2022, Month.AUGUST, 10);
        var uttaksDato = fødselsdato.minusWeeks(4);
        var regelmodell = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA, LovVersjoner.PROP15L2122)
            .medFørsteUttaksDato(uttaksDato)
            .medHendelsesDato(fødselsdato)
            .medTerminDato(fødselsdato.minusDays(2));

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato.minusWeeks(2).minusDays(2));
    }

    @Test
    void farTerminEtterFødselBeholdUttakEnUkeFør() {
        // Arrange
        var fødselsdato = LocalDate.of(2022, Month.AUGUST, 10);
        var uttaksDato = fødselsdato.minusWeeks(1);
        var regelmodell = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA, LovVersjoner.PROP15L2122)
            .medFørsteUttaksDato(uttaksDato)
            .medHendelsesDato(fødselsdato)
            .medTerminDato(fødselsdato.plusDays(2));

        // Act
        var resultat = new OpptjeningsPeriode();
        new RegelFastsettOpptjeningsperiode().evaluer(regelmodell, resultat);
        // Assert
        assertSkjæringsdato(resultat, fødselsdato.minusWeeks(1));
    }

    @Test
    void farTerminEtterFødselJusterTilToUker() {
        // Arrange
        var fødselsdato = LocalDate.of(2022, Month.AUGUST, 10);
        var uttaksDato = fødselsdato.minusWeeks(3);
        var regelmodell = OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.FARA, LovVersjoner.PROP15L2122)
            .medFørsteUttaksDato(uttaksDato)
            .medHendelsesDato(fødselsdato)
            .medTerminDato(fødselsdato.plusDays(2));

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
        return OpptjeningsperiodeGrunnlag.grunnlag(FagsakÅrsak.FØDSEL, RegelSøkerRolle.MORA, LovVersjoner.KLASSISK)
            .medFørsteUttaksDato(uttaksDato)
            .medHendelsesDato(hendelsesDato)
            .medTerminDato(terminDato);
    }
}
