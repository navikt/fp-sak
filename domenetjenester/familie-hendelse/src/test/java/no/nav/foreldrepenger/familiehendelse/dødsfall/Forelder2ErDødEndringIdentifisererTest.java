package no.nav.foreldrepenger.familiehendelse.dødsfall;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.foreldrepenger.domene.typer.AktørId;

class Forelder2ErDødEndringIdentifisererTest {
    private AktørId AKTØRID_SØKER = AktørId.dummy();
    private AktørId AKTØRID_MEDMOR = AktørId.dummy();
    private AktørId AKTØRID_BARN = AktørId.dummy();

    @Test
    void testForelder2Lever() {
        var orginaltGrunnlag = opprettPersonopplysning(null);
        var oppdatertGrunnlag = opprettPersonopplysning(null); //Oppdater opplysninger med dødsdato og lagre på behandlingen.
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, orginaltGrunnlag, oppdatertGrunnlag);

        var erEndret = differ.erForeldreDødsdatoEndret();
        assertThat(erEndret).as("Forventer at informsjon om forelder2 død er uendret.").isFalse();
    }

    @Test
    void testForelder2Dør() {
        final var dødsdato = LocalDate.now().minusDays(10);

        var orginaltGrunnlag = opprettPersonopplysning(null);
        var oppdatertGrunnlag = opprettPersonopplysning(dødsdato);//Oppdater opplysninger med dødsdato og lagre på behandlingen.
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, orginaltGrunnlag, oppdatertGrunnlag);

        var erEndret = differ.erForeldreDødsdatoEndret();
        assertThat(erEndret).as("Forventer at endring om forelder2 død blir detektert.").isTrue();
    }

    @Test
    void testDødsdatoEndret() {
        final var dødsdato = LocalDate.now().minusDays(10);

        var orginaltGrunnlag = opprettPersonopplysning(dødsdato);
        var oppdatertGrunnlag = opprettPersonopplysning(dødsdato.minusDays(1));//Oppdater dødsdato og lagre på behandlingen.
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, orginaltGrunnlag, oppdatertGrunnlag);

        var erEndret = differ.erForeldreDødsdatoEndret();
        assertThat(erEndret).as("Forventer at endring om forelder2 død blir detektert.").isTrue();
    }

    @Test
    void testDødsdatoUendret() {
        final var dødsdato = LocalDate.now().minusDays(10);

        var orginaltGrunnlag = opprettPersonopplysning(dødsdato);
        var oppdatertGrunnlag = opprettPersonopplysning(dødsdato);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, orginaltGrunnlag, oppdatertGrunnlag);

        var erEndret = differ.erForeldreDødsdatoEndret();
        assertThat(erEndret).as("Forventer at informsjon om forelder2 død er uendret.").isFalse();
    }

    @Test
    void skal_detektere_dødsdato_selv_om_registeropplysninger_ikke_finnes_på_originalt_grunnlag() {
        // Arrange
        final var dødsdato = LocalDate.now().minusDays(10);
        var orginaltGrunnlag = opprettTomtPersonopplysningGrunnlag();
        var oppdatertGrunnlag = opprettPersonopplysning(dødsdato);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, orginaltGrunnlag, oppdatertGrunnlag);

        var erEndret = differ.erForeldreDødsdatoEndret();

        // Assert
        assertThat(erEndret).as("Forventer at endring om forelder2 død blir detektert selv om det ikke finnes registeropplysninger på originalt grunnlag.").isTrue();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysning(LocalDate dødsdatoForelder2) {
        final var builder = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_SØKER).medFødselsdato(LocalDate.now().minusYears(30)));
        builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_MEDMOR).medFødselsdato(LocalDate.now().minusYears(28)).medDødsdato(dødsdatoForelder2));
        builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_BARN).medFødselsdato(LocalDate.now().minusYears(1)));
        builder.leggTil(builder.getRelasjonBuilder(AKTØRID_SØKER, AKTØRID_BARN, RelasjonsRolleType.BARN));
        builder.leggTil(builder.getRelasjonBuilder(AKTØRID_BARN, AKTØRID_SØKER, RelasjonsRolleType.MORA));
        builder.leggTil(builder.getRelasjonBuilder(AKTØRID_BARN, AKTØRID_MEDMOR, RelasjonsRolleType.MEDMOR));
        builder.leggTil(builder.getRelasjonBuilder(AKTØRID_MEDMOR, AKTØRID_BARN, RelasjonsRolleType.BARN));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder).medOppgittAnnenPart(new OppgittAnnenPartBuilder().medAktørId(AKTØRID_MEDMOR).build()).build();
    }

    private PersonopplysningGrunnlagEntitet opprettTomtPersonopplysningGrunnlag() {
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).build();
    }
}
