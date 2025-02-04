package no.nav.foreldrepenger.domene.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

class PersonAdresseEndringIdentifisererTest {

    private AktørId AKTØRID = AktørId.dummy();

    @Test
    void testPersonAdresseUendret() {
        var utlandkode = Landkoder.SWE;
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(utlandkode));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(utlandkode));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erSøkersUtlandsAdresserEndretIPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at adresse er uendret").isFalse();
    }

    @Test
    void testPersonAdresseUendret_flere_land() {
        var utlandkode = List.of(Landkoder.SWE, Landkoder.DNK);
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(utlandkode);
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(utlandkode);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erSøkersUtlandsAdresserEndretIPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at adresse er uendret").isFalse();
    }

    @Test
    void testPersonAdresseUendret_men_rekkefølge_er_endret() {
        var utlandkode = List.of(Landkoder.SWE, Landkoder.DNK);
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(utlandkode);
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlagMotstattRekkefølge(
                personopplysningGrunnlag1.getRegisterVersjon().map(PersonInformasjonEntitet::getAdresser).orElse(Collections.emptyList()));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erSøkersUtlandsAdresserEndretIPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at adresse er uendret").isFalse();
    }

    @Test
    void testPersonAdresseEndret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(Landkoder.DNK));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(Landkoder.SWE));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erSøkersUtlandsAdresserEndretIPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at endring i adresse blir detektert.").isTrue();
    }

    @Test
    void testPersonAdresseEndretNår() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(Landkoder.DNK));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(Landkoder.SWE));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erSøkersUtlandsAdresserEndretIPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at endring i adresse blir detektert.").isTrue();
    }

    @Test
    void testPersonAdresseEndret_flere_land() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(Landkoder.DNK, Landkoder.SWE));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(Landkoder.DNK, Landkoder.ALA));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erSøkersUtlandsAdresserEndretIPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at endring i adresse blir detektert.").isTrue();
    }

    @Test
    void testPersonAdresseEndret_ekstra_land_lagt_til() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(Landkoder.DNK, Landkoder.SWE));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(Landkoder.DNK, Landkoder.ALA, Landkoder.FIN));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erSøkersUtlandsAdresserEndretIPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at endring i adresse blir detektert.").isTrue();
    }

    @Test
    void testPersonAdresseEndret_landkode_adresselinje3_gir_uendret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlagLinje3(List.of(Landkoder.DNK, Landkoder.SWE), true);
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlagLinje3(List.of(Landkoder.DNK, Landkoder.SWE), false);
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erSøkersUtlandsAdresserEndretIPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at endring i adresse blir detektert.").isFalse();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlagMotstattRekkefølge(List<PersonAdresseEntitet> personadresser) {
        var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        // Bygg opp identiske statsborgerskap, bare legg de inn i motsatt rekkefølge.
        new LinkedList<>(personadresser)
                .descendingIterator()
                .forEachRemaining(a -> builder1
                        .leggTil(builder1.getAdresseBuilder(AKTØRID, a.getPeriode(), a.getAdresseType()).medLand(a.getLand())));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(List<Landkoder> land) {
        var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        // Opprett adresser med forskjellig fra og med dato. Går 1 mnd tilbake for hver adresse. Endrer kun land i denne testen
        IntStream.range(0, land.size()).forEach(i -> builder1.leggTil(
                builder1.getAdresseBuilder(AKTØRID, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(i)), AdresseType.POSTADRESSE_UTLAND)
                        .medLand(land.get(i))
                        .medAdresseType(AdresseType.POSTADRESSE_UTLAND)));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlagLinje3(List<Landkoder> land, boolean landLinje3) {
        var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        // Opprett adresser med forskjellig fra og med dato. Går 1 mnd tilbake for hver adresse. Endrer kun land i denne testen
        IntStream.range(0, land.size()).forEach(i -> builder1.leggTil(
            builder1.getAdresseBuilder(AKTØRID, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(i)), AdresseType.POSTADRESSE_UTLAND)
                .medAdresselinje1("1st street")
                .medAdresselinje2("101")
                .medAdresselinje3(landLinje3 ? land.get(i).getKode() : null)
                .medLand(land.get(i))
                .medAdresseType(AdresseType.POSTADRESSE_UTLAND)));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }
}
