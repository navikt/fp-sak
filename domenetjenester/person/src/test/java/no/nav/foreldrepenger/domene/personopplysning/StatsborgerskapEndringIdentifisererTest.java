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
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class StatsborgerskapEndringIdentifisererTest {

    private AktørId AKTØRID = AktørId.dummy();

    @Test
    public void testStatsborgerskapUendret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(LandOgRegion.get(Landkoder.NOR, Region.NORDEN)));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(LandOgRegion.get(Landkoder.NOR, Region.NORDEN)));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erStatsborgerskapEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at statsborgerskap er uendret").isFalse();
    }

    @Test
    public void testStatsborgerskapUendret_flere_koder() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(LandOgRegion.get(Landkoder.NOR, Region.NORDEN), LandOgRegion.get(Landkoder.SWE, Region.NORDEN)));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(LandOgRegion.get(Landkoder.NOR, Region.NORDEN), LandOgRegion.get(Landkoder.SWE, Region.NORDEN)));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erStatsborgerskapEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at statsborgerskap er uendret").isFalse();
    }

    @Test
    public void testStatsborgerskapUendret_men_rekkefølge_i_liste_endret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(LandOgRegion.get(Landkoder.NOR, Region.NORDEN), LandOgRegion.get(Landkoder.SWE, Region.NORDEN)));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlagMotstattRekkefølge(
                personopplysningGrunnlag1.getRegisterVersjon().map(PersonInformasjonEntitet::getStatsborgerskap).orElse(Collections.emptyList()));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erStatsborgerskapEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at endring i rekkefølge ikke skal detektere endring.").isFalse();
    }

    @Test
    public void testStatsborgerskapEndret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(LandOgRegion.get(Landkoder.SWE, Region.NORDEN)));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(LandOgRegion.get(Landkoder.NOR, Region.NORDEN)));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erStatsborgerskapEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at endring i statsborgerskap blir detektert.").isTrue();
    }

    @Test
    public void testStatsborgerskapEndret_endret_type() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(LandOgRegion.get(Landkoder.SWE, Region.NORDEN), LandOgRegion.get(Landkoder.NOR, Region.NORDEN)));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(LandOgRegion.get(Landkoder.SWE, Region.NORDEN), LandOgRegion.get(Landkoder.USA, Region.UDEFINERT)));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erStatsborgerskapEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at endring i statsborgerskap blir detektert.").isTrue();
    }

    @Test
    public void testStatsborgerskapEndret_ekstra_statsborgerskap_lagt_til() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(LandOgRegion.get(Landkoder.SWE, Region.NORDEN)));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(LandOgRegion.get(Landkoder.SWE, Region.NORDEN), LandOgRegion.get(Landkoder.NOR, Region.NORDEN)));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erStatsborgerskapEndretForSøkerFør(null);
        assertThat(erEndret).as("Forventer at endring i statsborgerskap blir detektert.").isTrue();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlagMotstattRekkefølge(List<StatsborgerskapEntitet> statsborgerLand) {
        final var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        // Bygg opp identiske statsborgerskap, bare legg de inn i motsatt rekkefølge.
        statsborgerLand.stream()
                .collect(Collectors.toCollection(LinkedList::new))
                .descendingIterator()
                .forEachRemaining(
                        s -> builder1.leggTil(builder1.getStatsborgerskapBuilder(AKTØRID, s.getPeriode(), s.getStatsborgerskap(), s.getRegion())
                                .medStatsborgerskap(s.getStatsborgerskap())));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(List<LandOgRegion> statsborgerskap) {
        final var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1
                .leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        IntStream.range(0, statsborgerskap.size())
                .forEach(i -> builder1
                        .leggTil(builder1.getStatsborgerskapBuilder(AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()),
                                statsborgerskap.get(i).land, statsborgerskap.get(i).region)));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }

    private static class LandOgRegion {
        private Landkoder land;
        private Region region;

        private static LandOgRegion get(Landkoder land, Region region) {
            var landOgRegion = new LandOgRegion();
            landOgRegion.land = land;
            landOgRegion.region = region;
            return landOgRegion;
        }
    }

}
