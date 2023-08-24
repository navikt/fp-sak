package no.nav.foreldrepenger.familiehendelse.dødsfall;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.*;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadAnnenPartType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.foreldrepenger.domene.typer.AktørId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ForelderErDødEndringIdentifisererTest {
    private AktørId AKTØRID = AktørId.dummy();
    private AktørId AKTØRID_ANNEN_PART = AktørId.dummy();


    @Test
    void testDødsdatoUendret() {
        var dødsdato = LocalDate.now().minusDays(10);
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(dødsdato);
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erForeldreDødsdatoEndret();
        assertThat(erEndret).as("Forventer at informsjon om brukers død er uendret").isFalse();
    }

    @Test
    void testSøkerDør() {
        var dødsdato = LocalDate.now().minusDays(10);

        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(null);
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erForeldreDødsdatoEndret();
        assertThat(erEndret).as("Forventer at endring om brukers død blir detektert.").isTrue();
    }

    @Test
    void testAnnenPartDør() {
        LocalDate dødsdatoSøker = null;
        LocalDate dødsdatoAnnenPart1 = null;
        var dødsdatoAnnenPart2 = LocalDate.now();
        var personopplysningGrunnlag1 = opprettPersonopplysningMedAnnenPart(dødsdatoSøker, dødsdatoAnnenPart1);
        var personopplysningGrunnlag2 = opprettPersonopplysningMedAnnenPart(dødsdatoSøker, dødsdatoAnnenPart2);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erForeldreDødsdatoEndret();
        assertThat(erEndret).as("Forventer at endring om annen parts død blir detektert.").isTrue();
    }

    @Test
    void testDødsdatoEndret() {
        var dødsdato = LocalDate.now().minusDays(10);

        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(dødsdato.minusDays(1));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erForeldreDødsdatoEndret();
        assertThat(erEndret).as("Forventer at endring om brukers død blir detektert.").isTrue();
    }

    @Test
    void skal_detektere_brukes_dødsdato_selv_om_registeropplysninger_ikke_finnes_på_originalt_grunnlag() {
        // Arrange
        var dødsdato = LocalDate.now().minusDays(10);
        var personopplysningGrunnlag1 = opprettTomtPersonopplysningGrunnlag();
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato);

        // Act
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);
        var erEndret = differ.erForeldreDødsdatoEndret();

        // Assert
        assertThat(erEndret).as("Forventer at endring om brukers død blir detektert selv om det ikke finnes registeropplysninger på originalt grunnlag.").isTrue();
    }

    @Test
    void skal_detektere_annen_parts_dødsdato_selv_om_registeropplysninger_ikke_finnes_på_originalt_grunnlag() {
        // Arrange
        LocalDate dødsdatoSøker = null;
        var dødsdatoAnnenPart2 = LocalDate.now();
        var personopplysningGrunnlag1 = opprettTomtPersonopplysningGrunnlag();
        var personopplysningGrunnlag2 = opprettPersonopplysningMedAnnenPart(dødsdatoSøker, dødsdatoAnnenPart2);

        // Act
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);
        var erEndret = differ.erForeldreDødsdatoEndret();

        // Assert
        assertThat(erEndret).as("Forventer at endring om annen parts død blir detektert selv om det ikke finnes registeropplysninger på originalt grunnlag.").isTrue();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(LocalDate dødsdato) {
        var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID).medDødsdato(dødsdato));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningMedAnnenPart(LocalDate dødsdatoSøker, LocalDate dødsdatoAnnenPart) {
        var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID).medDødsdato(dødsdatoSøker));
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID_ANNEN_PART).medDødsdato(dødsdatoAnnenPart));
        var annenPartBuilder = new OppgittAnnenPartBuilder()
            .medAktørId(AKTØRID_ANNEN_PART)
            .medType(SøknadAnnenPartType.FAR);
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegistrertVersjon(builder1)
            .medOppgittAnnenPart(annenPartBuilder.build())
            .build();
    }

    private PersonopplysningGrunnlagEntitet opprettTomtPersonopplysningGrunnlag() {
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).build();
    }
}
