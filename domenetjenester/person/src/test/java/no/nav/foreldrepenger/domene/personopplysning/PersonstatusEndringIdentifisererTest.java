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

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class PersonstatusEndringIdentifisererTest {

    private AktørId AKTØRID = AktørId.dummy();

    @Test
    public void testPersonstatusUendret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.FOSV));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(List.of(PersonstatusType.FOSV));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erPersonstatusEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at personstatus er uendret").isFalse();
    }

    @Test
    public void testPersonstatusUendret_flere_statuser() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.FOSV, PersonstatusType.BOSA));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.FOSV, PersonstatusType.BOSA));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erPersonstatusEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at personstatus er uendret").isFalse();
    }

    @Test
    public void testPersonstatusEndret_ekstra_status_lagt_til() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.FOSV, PersonstatusType.BOSA));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.FOSV, PersonstatusType.BOSA, PersonstatusType.UREG));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erPersonstatusEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at endring i personstatus blir detektert.").isTrue();
    }

    @Test
    public void testPersonstatusEndret_status_endret_type() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.FOSV, PersonstatusType.BOSA));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.UREG, PersonstatusType.FOSV));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erPersonstatusEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at endring i personstatus blir detektert.").isTrue();
    }

    @Test
    public void testPersonstatusUendret_men_rekkefølge_i_liste_endret() {
        var personopplysningGrunnlag1 = opprettPersonopplysningGrunnlag(
                List.of(PersonstatusType.FOSV, PersonstatusType.BOSA));
        var personopplysningGrunnlag2 = opprettPersonopplysningGrunnlagMotstattRekkefølge(
                personopplysningGrunnlag1.getRegisterVersjon().map(PersonInformasjonEntitet::getPersonstatus).orElse(Collections.emptyList()));
        var differ = new PersonopplysningGrunnlagDiff(AKTØRID, personopplysningGrunnlag1, personopplysningGrunnlag2);

        var erEndret = differ.erPersonstatusEndretForSøkerPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now()));
        assertThat(erEndret).as("Forventer at endring i rekkefølge ikke skal detektere endring.").isFalse();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlagMotstattRekkefølge(List<PersonstatusEntitet> personstatuser) {
        final var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        personstatuser.stream()
                .collect(Collectors.toCollection(LinkedList::new))
                .descendingIterator()
                .forEachRemaining(
                        ps -> builder1.leggTil(builder1.getPersonstatusBuilder(AKTØRID, ps.getPeriode()).medPersonstatus(ps.getPersonstatus())));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }

    private PersonopplysningGrunnlagEntitet opprettPersonopplysningGrunnlag(List<PersonstatusType> personstatuser) {
        final var builder1 = PersonInformasjonBuilder.oppdater(Optional.empty(), PersonopplysningVersjonType.REGISTRERT);
        builder1.leggTil(builder1.getPersonopplysningBuilder(AKTØRID));
        // Opprett personstatuser med forskjellig fra og med dato. Går 1 mnd tilbake for
        // hver status.
        IntStream.range(0, personstatuser.size())
                .forEach(i -> builder1.leggTil(builder1.getPersonstatusBuilder(AKTØRID, DatoIntervallEntitet.fraOgMed(LocalDate.now().minusMonths(i)))
                        .medPersonstatus(personstatuser.get(i))));
        return PersonopplysningGrunnlagBuilder.oppdatere(Optional.empty()).medRegistrertVersjon(builder1).build();
    }
}
