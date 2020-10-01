package no.nav.foreldrepenger.familiehendelse.dødsfall;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class MorErDødEndringIdentifisererTest {
    private AktørId AKTØRID_SØKER = AktørId.dummy();
    private AktørId AKTØRID_MOR = AktørId.dummy();
    private AktørId AKTØRID_BARN = AktørId.dummy();

    @Test
    public void testMorLever() {
        PersonopplysningGrunnlagEntitet orginaltGrunnlag = opprettPersonopplysning(null);
        PersonopplysningGrunnlagEntitet oppdatertGrunnlag = opprettPersonopplysning(null); //Oppdater opplysninger med dødsdato og lagre på behandlingen.
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, orginaltGrunnlag, oppdatertGrunnlag);

        boolean erEndret = differ.erForeldreDødsdatoEndret();
        assertThat(erEndret).as("Forventer at informsjon om mors død er uendret.").isFalse();
    }

    @Test
    public void testMorDør() {
        final LocalDate dødsdato = LocalDate.now().minusDays(10);

        PersonopplysningGrunnlagEntitet orginaltGrunnlag = opprettPersonopplysning(null);
        PersonopplysningGrunnlagEntitet oppdatertGrunnlag = opprettPersonopplysning(dødsdato);//Oppdater opplysninger med dødsdato og lagre på behandlingen.
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, orginaltGrunnlag, oppdatertGrunnlag);

        boolean erEndret = differ.erForeldreDødsdatoEndret();
        assertThat(erEndret).as("Forventer at endring om mors død blir detektert.").isTrue();
    }

    @Test
    public void testDødsdatoEndret() {
        final LocalDate dødsdato = LocalDate.now().minusDays(10);

        PersonopplysningGrunnlagEntitet orginaltGrunnlag = opprettPersonopplysning(dødsdato);
        PersonopplysningGrunnlagEntitet oppdatertGrunnlag = opprettPersonopplysning(dødsdato.minusDays(1));//Oppdater dødsdato og lagre på behandlingen.
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, orginaltGrunnlag, oppdatertGrunnlag);

        boolean erEndret = differ.erForeldreDødsdatoEndret();
        assertThat(erEndret).as("Forventer at endring om mors død blir detektert.").isTrue();
    }

    @Test
    public void testDødsdatoUendret() {
        final LocalDate dødsdato = LocalDate.now().minusDays(10);

        PersonopplysningGrunnlagEntitet orginaltGrunnlag = opprettPersonopplysning(dødsdato);
        PersonopplysningGrunnlagEntitet oppdatertGrunnlag = opprettPersonopplysning(dødsdato);//Oppdater dødsdato og lagre på behandlingen.
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, orginaltGrunnlag, oppdatertGrunnlag);

        boolean erEndret = differ.erForeldreDødsdatoEndret();
        assertThat(erEndret).as("Forventer at informsjon om mors død er uendret.").isFalse();
    }

    @Test
    public void skal_detektere_dødsdato_selv_om_registeropplysninger_ikke_finnes_på_originalt_grunnlag() {
        // Arrange
        final LocalDate dødsdato = LocalDate.now().minusDays(10);
        PersonopplysningGrunnlagEntitet orginaltGrunnlag = opprettTomtPersonopplysningGrunnlag();
        PersonopplysningGrunnlagEntitet oppdatertGrunnlag = opprettPersonopplysning(dødsdato);//Oppdater opplysninger med dødsdato og lagre på behandlingen.

        // Act
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, orginaltGrunnlag, oppdatertGrunnlag);
        boolean erEndret = differ.erForeldreDødsdatoEndret();

        // Assert
        assertThat(erEndret).as("Forventer at endring om mors død blir detektert selv om det ikke finnes registeropplysninger på originalt grunnlag.").isTrue();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysning(LocalDate dødsdatoMor) {
        final PersonInformasjonBuilder builder = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_SØKER).medFødselsdato(LocalDate.now().minusYears(30)));
        builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_MOR).medFødselsdato(LocalDate.now().minusYears(28)).medDødsdato(dødsdatoMor));
        builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_BARN).medFødselsdato(LocalDate.now().minusYears(1)));
        builder.leggTil(builder.getRelasjonBuilder(AKTØRID_BARN, AKTØRID_SØKER, RelasjonsRolleType.FARA));
        builder.leggTil(builder.getRelasjonBuilder(AKTØRID_SØKER, AKTØRID_BARN, RelasjonsRolleType.BARN));
        builder.leggTil(builder.getRelasjonBuilder(AKTØRID_BARN, AKTØRID_MOR, RelasjonsRolleType.MORA));
        builder.leggTil(builder.getRelasjonBuilder(AKTØRID_MOR, AKTØRID_BARN, RelasjonsRolleType.BARN));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder).medOppgittAnnenPart(new OppgittAnnenPartBuilder().medAktørId(AKTØRID_MOR).build()).build();
    }

    private PersonopplysningGrunnlagEntitet opprettTomtPersonopplysningGrunnlag() {
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).build();
    }
}

