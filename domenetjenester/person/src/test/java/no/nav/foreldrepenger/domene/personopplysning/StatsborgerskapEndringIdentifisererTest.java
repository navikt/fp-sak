package no.nav.foreldrepenger.domene.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

class StatsborgerskapEndringIdentifisererTest {

    private static final LocalDate IDAG = LocalDate.now();

    private AktørId AKTØRID = AktørId.dummy();

    @Test
    void testStatsborgerskapUendret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(Landkoder.NOR));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(Landkoder.NOR));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erRegionEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(IDAG), IDAG);
        assertThat(erEndret).as("Forventer at statsborgerskap er uendret").isFalse();
    }

    @Test
    void testStatsborgerskapUendret_flere_koder() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(Landkoder.POL, Landkoder.SWE));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(Landkoder.NOR, Landkoder.SWE));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erRegionEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(IDAG), IDAG);
        assertThat(erEndret).as("Forventer at statsborgerskap er uendret").isFalse();
    }

    @Test
    void testStatsborgerskapUendret_men_rekkefølge_i_liste_endret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(Landkoder.NOR, Landkoder.SWE));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlagMotstattRekkefølge(
                personopplysningGrunnlag1.getRegisterVersjon().map(PersonInformasjonEntitet::getStatsborgerskap).orElse(Collections.emptyList()));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erRegionEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(IDAG), IDAG);
        assertThat(erEndret).as("Forventer at endring i rekkefølge ikke skal detektere endring.").isFalse();
    }

    @Test
    void testStatsborgerskapEndret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(Landkoder.POL));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(Landkoder.NOR));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erRegionEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(IDAG), IDAG);
        assertThat(erEndret).as("Forventer at endring i statsborgerskap blir detektert.").isTrue();
    }

    @Test
    void testStatsborgerskapEndret_endret_type() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(Landkoder.FRA, Landkoder.USA));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(Landkoder.SWE, Landkoder.USA));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erRegionEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(IDAG), IDAG);
        assertThat(erEndret).as("Forventer at endring i statsborgerskap blir detektert.").isTrue();
    }

    @Test
    void testStatsborgerskapEndret_ekstra_statsborgerskap_lagt_til() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(Landkoder.CHE));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(Landkoder.CHE, Landkoder.NOR));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erRegionEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(IDAG), IDAG);
        assertThat(erEndret).as("Forventer at endring i statsborgerskap blir detektert.").isTrue();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlagMotstattRekkefølge(List<StatsborgerskapEntitet> statsborgerLand) {
        final var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        // Bygg opp identiske statsborgerskap, bare legg de inn i motsatt rekkefølge.
        new LinkedList<>(statsborgerLand)
                .descendingIterator()
                .forEachRemaining(
                        s -> builder1.leggTil(builder1.getStatsborgerskapBuilder(AKTØRID, s.getPeriode(), s.getStatsborgerskap())
                                .medStatsborgerskap(s.getStatsborgerskap())));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(List<Landkoder> statsborgerskap) {
        final var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1
                .leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        IntStream.range(0, statsborgerskap.size())
                .forEach(i -> builder1
                        .leggTil(builder1.getStatsborgerskapBuilder(AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(IDAG, IDAG),
                                statsborgerskap.get(i))));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }

}
