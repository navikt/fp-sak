package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningGrunnlagDiff;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class PersonopplysningGrunnlagDiffTest {

    private static final AktørId EKTEFELLE_AKTØR_ID = AktørId.dummy();
    private static final AktørId BARN_AKTØR_ID = AktørId.dummy();
    private static final AktørId SØKER_AKTØR_ID = AktørId.dummy();
    private static final AktørId ANNEN_AKTØR_ID = AktørId.dummy();

    @Test
    public void skal_ikke_identifisere_som_kun_endring_fødsel_dersom_ingen_diff() {
        // Arrange
        final PersonopplysningGrunnlagBuilder orginalt = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        final PersonInformasjonBuilder builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(SØKER_AKTØR_ID));
        orginalt.medRegistrertVersjon(builder1);

        final PersonopplysningGrunnlagBuilder oppdatert = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        final PersonInformasjonBuilder builder2 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder2.leggTil(builder2.getPersonopplysningBuilder(SØKER_AKTØR_ID));
        oppdatert.medRegistrertVersjon(builder2);

        // Act/Assert
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(SØKER_AKTØR_ID, orginalt.build(), oppdatert.build());
        Assertions.assertThat(differ.erRelasjonerEndret()).isFalse();
    }

    @Test
    public void skal_identifisere_som_kun_ny_fødsel_dersom_diff_kun_omfatter_melding_om_fødsel() {
        // Arrange
        final PersonopplysningGrunnlagBuilder orginalt = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        final PersonInformasjonBuilder builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(SØKER_AKTØR_ID));
        orginalt.medRegistrertVersjon(builder1);

        final PersonopplysningGrunnlagBuilder oppdatert = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        final PersonInformasjonBuilder builder2 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder2.leggTil(builder2.getPersonopplysningBuilder(SØKER_AKTØR_ID).medSivilstand(SivilstandType.GIFT));
        builder2.leggTil(builder2.getPersonopplysningBuilder(ANNEN_AKTØR_ID).medFødselsdato(LocalDate.now().minusDays(1)));
        builder2.leggTil(builder2.getRelasjonBuilder(SØKER_AKTØR_ID, ANNEN_AKTØR_ID, RelasjonsRolleType.BARN));
        builder2.leggTil(builder2.getRelasjonBuilder(ANNEN_AKTØR_ID, SØKER_AKTØR_ID, RelasjonsRolleType.MORA));
        oppdatert.medRegistrertVersjon(builder2);

        // Act/Assert
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(SØKER_AKTØR_ID, orginalt.build(), oppdatert.build());
        Assertions.assertThat(differ.erRelasjonerEndret()).isTrue();
        Assertions.assertThat(differ.erRelasjonerEndretSøkerAntallBarn()).isTrue();
        Assertions.assertThat(differ.erRelasjonerEndretForSøkerUtenomNyeBarn()).isFalse();
        Assertions.assertThat(differ.erRelasjonerEndretForEksisterendeBarn()).isFalse();
    }

    @Test
    public void skal_ikke_identifisere_som_kun_endring_fødsel_dersom_diff_omfatter_mer_enn_melding_om_fødsel() {
        // Arrange
        final PersonopplysningGrunnlagBuilder orginalt = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        final PersonInformasjonBuilder builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(SØKER_AKTØR_ID).medSivilstand(SivilstandType.GIFT));
        orginalt.medRegistrertVersjon(builder1);

        final PersonopplysningGrunnlagBuilder oppdatert = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        final PersonInformasjonBuilder builder2 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder2.leggTil(builder2.getPersonopplysningBuilder(SØKER_AKTØR_ID).medSivilstand(SivilstandType.UGIFT));
        builder2.leggTil(builder2.getPersonopplysningBuilder(ANNEN_AKTØR_ID).medFødselsdato(LocalDate.now().minusDays(1)));
        builder2.leggTil(builder2.getRelasjonBuilder(SØKER_AKTØR_ID, ANNEN_AKTØR_ID, RelasjonsRolleType.BARN));
        builder2.leggTil(builder2.getRelasjonBuilder(ANNEN_AKTØR_ID, SØKER_AKTØR_ID, RelasjonsRolleType.MORA));
        oppdatert.medRegistrertVersjon(builder2);

        // Act/Assert
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(SØKER_AKTØR_ID, orginalt.build(), oppdatert.build());
        Assertions.assertThat(differ.erRelasjonerEndret()).isTrue();
        Assertions.assertThat(differ.erRelasjonerEndretSøkerAntallBarn()).isTrue();
        Assertions.assertThat(differ.erRelasjonerEndretForSøkerUtenomNyeBarn()).isFalse();
        Assertions.assertThat(differ.erRelasjonerEndretForEksisterendeBarn()).isFalse();
        Assertions.assertThat(differ.erSivilstandEndretForBruker()).isTrue();
    }

    @Test
    public void skal_identifisere_som_kun_endring_fødsel_dersom_diff_omfatter_ektefelle_i_tillegg_til_nytt_barn() {
        // Arrange
        final PersonopplysningGrunnlagBuilder orginalt = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        final PersonInformasjonBuilder builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(SØKER_AKTØR_ID));
        builder1.leggTil(builder1.getPersonopplysningBuilder(EKTEFELLE_AKTØR_ID));
        builder1.leggTil(builder1.getRelasjonBuilder(EKTEFELLE_AKTØR_ID, SØKER_AKTØR_ID, RelasjonsRolleType.EKTE));
        builder1.leggTil(builder1.getRelasjonBuilder(SØKER_AKTØR_ID, EKTEFELLE_AKTØR_ID, RelasjonsRolleType.EKTE));

        orginalt.medRegistrertVersjon(builder1);

        final PersonopplysningGrunnlagBuilder oppdatert = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        final PersonInformasjonBuilder builder2 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder2.leggTil(builder2.getPersonopplysningBuilder(SØKER_AKTØR_ID));
        builder2.leggTil(builder2.getPersonopplysningBuilder(EKTEFELLE_AKTØR_ID));
        builder2.leggTil(builder2.getRelasjonBuilder(EKTEFELLE_AKTØR_ID, SØKER_AKTØR_ID, RelasjonsRolleType.EKTE));
        builder2.leggTil(builder2.getRelasjonBuilder(SØKER_AKTØR_ID, EKTEFELLE_AKTØR_ID, RelasjonsRolleType.EKTE));
        builder2.leggTil(builder2.getPersonopplysningBuilder(ANNEN_AKTØR_ID).medFødselsdato(LocalDate.now().minusDays(1)));
        builder2.leggTil(builder2.getRelasjonBuilder(SØKER_AKTØR_ID, ANNEN_AKTØR_ID, RelasjonsRolleType.BARN));
        builder2.leggTil(builder2.getRelasjonBuilder(ANNEN_AKTØR_ID, SØKER_AKTØR_ID, RelasjonsRolleType.MORA));
        builder2.leggTil(builder2.getRelasjonBuilder(EKTEFELLE_AKTØR_ID, ANNEN_AKTØR_ID, RelasjonsRolleType.BARN));
        builder2.leggTil(builder2.getRelasjonBuilder(ANNEN_AKTØR_ID, EKTEFELLE_AKTØR_ID, RelasjonsRolleType.FARA));
        oppdatert.medRegistrertVersjon(builder2);

        // Act
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(SØKER_AKTØR_ID, orginalt.build(), oppdatert.build());
        Assertions.assertThat(differ.erRelasjonerEndret()).isTrue();
        Assertions.assertThat(differ.erRelasjonerEndretSøkerAntallBarn()).isTrue();
        Assertions.assertThat(differ.erRelasjonerEndretForSøkerUtenomNyeBarn()).isFalse();
        Assertions.assertThat(differ.erRelasjonerEndretForEksisterendeBarn()).isFalse();
    }


    @Test
    public void skal_ikke_identifisere_som_kun_endring_fødsel_dersom_diff_omfatter_ny_ektefelle_i_tillegg_til_nytt_barn() {
        // Arrange
        final PersonopplysningGrunnlagBuilder orginalt = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        final PersonInformasjonBuilder builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(SØKER_AKTØR_ID));
        orginalt.medRegistrertVersjon(builder1);

        final PersonopplysningGrunnlagBuilder oppdatert = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        final PersonInformasjonBuilder builder2 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder2.leggTil(builder2.getPersonopplysningBuilder(SØKER_AKTØR_ID));
        builder2.leggTil(builder2.getPersonopplysningBuilder(EKTEFELLE_AKTØR_ID));
        builder2.leggTil(builder2.getPersonopplysningBuilder(ANNEN_AKTØR_ID).medFødselsdato(LocalDate.now().minusDays(1)));
        builder2.leggTil(builder2.getRelasjonBuilder(SØKER_AKTØR_ID, ANNEN_AKTØR_ID, RelasjonsRolleType.BARN));
        builder2.leggTil(builder2.getRelasjonBuilder(ANNEN_AKTØR_ID, SØKER_AKTØR_ID, RelasjonsRolleType.MORA));
        builder2.leggTil(builder2.getRelasjonBuilder(EKTEFELLE_AKTØR_ID, SØKER_AKTØR_ID, RelasjonsRolleType.EKTE));
        builder2.leggTil(builder2.getRelasjonBuilder(SØKER_AKTØR_ID, EKTEFELLE_AKTØR_ID, RelasjonsRolleType.EKTE));
        oppdatert.medRegistrertVersjon(builder2);

        // Act
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(SØKER_AKTØR_ID, orginalt.build(), oppdatert.build());
        Assertions.assertThat(differ.erRelasjonerEndret()).isTrue();
        Assertions.assertThat(differ.erRelasjonerEndretSøkerAntallBarn()).isTrue();
        Assertions.assertThat(differ.erRelasjonerEndretForSøkerUtenomNyeBarn()).isTrue();
        Assertions.assertThat(differ.erRelasjonerEndretForEksisterendeBarn()).isFalse();
    }

    @Test
    public void skal_identifisere_som_kun_ny_fødsel_dersom_diff_kun_omfatter_melding_om_fødsel_barn_nummer_to() {
        // Arrange
        final PersonopplysningGrunnlagBuilder orginalt = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        final PersonInformasjonBuilder builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(SØKER_AKTØR_ID));
        builder1.leggTil(builder1.getPersonopplysningBuilder(ANNEN_AKTØR_ID).medFødselsdato(LocalDate.now().minusYears(3)));
        builder1.leggTil(builder1.getRelasjonBuilder(SØKER_AKTØR_ID, ANNEN_AKTØR_ID, RelasjonsRolleType.BARN));
        builder1.leggTil(builder1.getRelasjonBuilder(ANNEN_AKTØR_ID, SØKER_AKTØR_ID, RelasjonsRolleType.MORA));
        orginalt.medRegistrertVersjon(builder1);

        final PersonopplysningGrunnlagBuilder oppdatert = PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty());
        final PersonInformasjonBuilder builder2 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder2.leggTil(builder2.getPersonopplysningBuilder(SØKER_AKTØR_ID));
        builder2.leggTil(builder2.getPersonopplysningBuilder(ANNEN_AKTØR_ID).medFødselsdato(LocalDate.now().minusYears(3)));
        builder2.leggTil(builder2.getRelasjonBuilder(SØKER_AKTØR_ID, ANNEN_AKTØR_ID, RelasjonsRolleType.BARN));
        builder2.leggTil(builder2.getRelasjonBuilder(ANNEN_AKTØR_ID, SØKER_AKTØR_ID, RelasjonsRolleType.MORA));
        builder2.leggTil(builder2.getPersonopplysningBuilder(BARN_AKTØR_ID).medFødselsdato(LocalDate.now().minusDays(1)));
        builder2.leggTil(builder2.getRelasjonBuilder(SØKER_AKTØR_ID, BARN_AKTØR_ID, RelasjonsRolleType.BARN));
        builder2.leggTil(builder2.getRelasjonBuilder(BARN_AKTØR_ID, SØKER_AKTØR_ID, RelasjonsRolleType.MORA));
        oppdatert.medRegistrertVersjon(builder2);

        // Act/Assert
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(SØKER_AKTØR_ID, orginalt.build(), oppdatert.build());
        Assertions.assertThat(differ.erRelasjonerEndret()).isTrue();
        Assertions.assertThat(differ.erRelasjonerEndretSøkerAntallBarn()).isTrue();
        Assertions.assertThat(differ.erRelasjonerEndretForSøkerUtenomNyeBarn()).isFalse();
        Assertions.assertThat(differ.erRelasjonerEndretForEksisterendeBarn()).isFalse();

    }

}
