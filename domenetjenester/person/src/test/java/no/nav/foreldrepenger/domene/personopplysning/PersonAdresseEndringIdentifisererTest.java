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
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class PersonAdresseEndringIdentifisererTest {

    private AktørId AKTØRID = AktørId.dummy();

    @Test
    public void testPersonAdresseUendret() {
        final String postnummer = "2040";
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(postnummer));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(postnummer));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erAdresserEndretFør(null);
        assertThat(erEndret).as("Forventer at adresse er uendret").isFalse();
    }

    @Test
    public void testPersonAdresseUendret_flere_postnummer() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of("2040", "2050"));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of("2040", "2050"));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erAdresserEndretFør(null);
        assertThat(erEndret).as("Forventer at adresse er uendret").isFalse();
    }

    @Test
    public void testPersonAdresseUendret_men_rekkefølge_er_endret() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of("2050", "2040"));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlagMotstattRekkefølge(
                personopplysningGrunnlag1.getRegisterVersjon().map(PersonInformasjonEntitet::getAdresser).orElse(Collections.emptyList()));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erAdresserEndretFør(null);
        assertThat(erEndret).as("Forventer at adresse er uendret").isFalse();
    }

    @Test
    public void testPersonAdresseEndret() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of("2040"));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of("2050"));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erAdresserEndretFør(null);
        assertThat(erEndret).as("Forventer at endring i adresse blir detektert.").isTrue();
    }

    @Test
    public void testPersonAdresseEndretNår() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of("2040"));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of("2050"));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erSøkersAdresseEndretFør(LocalDate.now().plusDays(1));
        assertThat(erEndret).as("Forventer at endring i adresse blir detektert.").isTrue();
    }

    @Test
    public void testPersonAdresseEndret_flere_postnummer() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of("2040", "2050"));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of("2040", "2060"));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erAdresserEndretFør(null);
        assertThat(erEndret).as("Forventer at endring i adresse blir detektert.").isTrue();
    }

    @Test
    public void testPersonAdresseEndret_ekstra_postnummer_lagt_til() {
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of("2040", "2050"));
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of("2040", "2050", "9046"));
        PersonopplysningGrunnlagDiff differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        boolean erEndret = differ.erAdresserEndretFør(null);
        assertThat(erEndret).as("Forventer at endring i adresse blir detektert.").isTrue();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlagMotstattRekkefølge(List<PersonAdresseEntitet> personadresser) {
        final PersonInformasjonBuilder builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        // Bygg opp identiske statsborgerskap, bare legg de inn i motsatt rekkefølge.
        new LinkedList<>(personadresser)
                .descendingIterator()
                .forEachRemaining(a -> builder1
                        .leggTil(builder1.getAdresseBuilder(AKTØRID, a.getPeriode(), a.getAdresseType()).medPostnummer(a.getPostnummer())));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(List<String> postnummer) {
        final PersonInformasjonBuilder builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1
                .leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        // Opprett adresser med forskjellig fra og med dato. Går 1 mnd tilbake for hver
        // adresse. Endrer kun postnummer i denne testen
        IntStream.range(0, postnummer.size()).forEach(i -> builder1.leggTil(
                builder1.getAdresseBuilder(AKTØRID, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(i)), AdresseType.POSTADRESSE)
                        .medPostnummer(postnummer.get(i))
                        .medAdresseType(AdresseType.POSTADRESSE)));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }
}
