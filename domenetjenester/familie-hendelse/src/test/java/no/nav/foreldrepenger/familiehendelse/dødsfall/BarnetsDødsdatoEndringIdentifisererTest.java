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

class BarnetsDødsdatoEndringIdentifisererTest {
    private AktørId AKTØRID_SØKER = AktørId.dummy();
    private AktørId AKTØRID_BARN = AktørId.dummy();

    @Test
    void testBarnLever() {
        final LocalDate dødsdato = null;
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(dødsdato, true);
        var personopplysningGrunnlagOrginal = opprettPersonopplysningGrunnlag(dødsdato, true);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, personopplysningGrunnlag1, personopplysningGrunnlagOrginal);

        var erEndret = differ.erBarnDødsdatoEndret();
        assertThat(erEndret).as("Forventer at informsjon om barnets død er uendret").isFalse();
    }

    @Test
    void test_nytt_barn_i_tps_som_ikke_var_registrert_i_TPS_orginalt() {
        final LocalDate dødsdato = null;
        var personopplysningGrunnlagOrginal = opprettPersonopplysningGrunnlag(dødsdato, false);
        var personopplysningGrunnlagNy = opprettPersonopplysningGrunnlag(dødsdato, true);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, personopplysningGrunnlagNy, personopplysningGrunnlagOrginal);

        var erEndret = differ.erBarnDødsdatoEndret();
        assertThat(erEndret).as("Forventer at informsjon om barnets død er uendret").isFalse();
    }

    @Test
    void testDødsdatoUendret() {
        var dødsdato = LocalDate.now().minusDays(10);
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(dødsdato, true);
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato, true);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erBarnDødsdatoEndret();
        assertThat(erEndret).as("Forventer at informsjon om barnets død er uendret").isFalse();
    }

    @Test
    void testBarnDør() {
        var dødsdato = LocalDate.now().minusDays(10);
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(null, true);
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato, true);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erBarnDødsdatoEndret();
        assertThat(erEndret).as("Forventer at endring om barnets død blir detektert.").isTrue();
    }

    @Test
    void testDødsdatoEndret() {
        var dødsdato = LocalDate.now().minusDays(10);
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(dødsdato.minusDays(1), true);
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato, true);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erBarnDødsdatoEndret();
        assertThat(erEndret).as("Forventer at endring om barnets død blir detektert.").isTrue();
    }

    @Test
    void skal_detektere_dødsdato_selv_om_registeropplysninger_ikke_finnes_på_originalt_grunnlag() {
        // Arrange
        var dødsdato = LocalDate.now().minusDays(10);
        var personopplysningGrunnlag1 = opprettTomtPersonopplysningGrunnlag();
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(dødsdato, true);

        // Act
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID_SØKER, personopplysningGrunnlag1, personopplysningGrunnlag2);
        var erEndret = differ.erBarnDødsdatoEndret();

        // Assert
        assertThat(erEndret).as("Forventer at barnets død blir detektert selv om det ikke finnes registeropplysninger på originalt grunnlag.")
            .isTrue();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(LocalDate dødsdatoBarn, boolean registrerMedBarn) {
        var builder = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder.leggTil(builder.getPersonopplysningBuilder(AKTØRID_SØKER).medFødselsdato(LocalDate.now().minusYears(30)));
        if (registrerMedBarn) {
            builder.leggTil(
                builder.getPersonopplysningBuilder(AKTØRID_BARN).medFødselsdato(LocalDate.now().minusMonths(1)).medDødsdato(dødsdatoBarn));
            builder.leggTil(builder.getRelasjonBuilder(AKTØRID_SØKER, AKTØRID_BARN, RelasjonsRolleType.BARN));
            builder.leggTil(builder.getRelasjonBuilder(AKTØRID_BARN, AKTØRID_SØKER, RelasjonsRolleType.MORA));
        }
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty())
            .medRegistrertVersjon(builder)
            .medOppgittAnnenPart(new OppgittAnnenPartBuilder().build())
            .build();
    }

    private PersonopplysningGrunnlagEntitet opprettTomtPersonopplysningGrunnlag() {
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).build();
    }
}
