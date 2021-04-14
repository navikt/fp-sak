package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class PersonopplysningGrunnlagDiff {
    private AktørId søkerAktørId;
    private Optional<AktørId> annenPartAktørId;
    private PersonopplysningGrunnlagEntitet grunnlag1;
    private PersonopplysningGrunnlagEntitet grunnlag2;
    private Set<AktørId> søkersBarnUnion;
    private Set<AktørId> søkersBarnSnitt;
    private Set<AktørId> søkersBarnDiff;
    private Set<AktørId> personerSnitt;

    public PersonopplysningGrunnlagDiff(AktørId søker, PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2) {
        this.søkerAktørId = søker;
        this.grunnlag1 = grunnlag1;
        this.grunnlag2 = grunnlag2;
        søkersBarnUnion = finnAlleBarn();
        søkersBarnSnitt = finnFellesBarn();
        personerSnitt = fellesAktører();
        annenPartAktørId = finnAnnenPart();
        søkersBarnDiff = søkersBarnUnion.stream().filter(barn -> !søkersBarnSnitt.contains(barn)).collect(Collectors.toSet());
    }

    public boolean erRelasjonerEndret() {
        var differ = new RegisterdataDiffsjekker(true);
        return differ.erForskjellPå(registerVersjon(grunnlag1).map(PersonInformasjonEntitet::getRelasjoner).orElse(Collections.emptyList()),
            registerVersjon(grunnlag2).map(PersonInformasjonEntitet::getRelasjoner).orElse(Collections.emptyList()));
    }

    public boolean erRelasjonerEndretSøkerAntallBarn() {
        return !søkersBarnDiff.isEmpty();
    }

    public boolean erRelasjonerEndretForSøkerUtenomNyeBarn() {
        Set<AktørId> ikkeSjekkMot = new HashSet<>(søkersBarnDiff);
        annenPartAktørId.ifPresent(ikkeSjekkMot::add);
        return erRelasjonerEndretForAktører(Set.of(søkerAktørId), ikkeSjekkMot);
    }

    public boolean erRelasjonerEndretForEksisterendeBarn() {
        Set<AktørId> ikkeSjekkMot = new HashSet<>();
        annenPartAktørId.ifPresent(ikkeSjekkMot::add);
        return erRelasjonerEndretForAktører(søkersBarnSnitt, ikkeSjekkMot);
    }

    private boolean erRelasjonerEndretForAktører(Set<AktørId> fra, Set<AktørId> ikkeTil) {
        var differ = new RegisterdataDiffsjekker(true);
        return differ.erForskjellPå(hentRelasjonerFraMenIkkeTil(grunnlag1, fra, ikkeTil),
            hentRelasjonerFraMenIkkeTil(grunnlag2, fra, ikkeTil));
    }

    private List<PersonRelasjonEntitet> hentRelasjonerFraMenIkkeTil(PersonopplysningGrunnlagEntitet grunnlag, Set<AktørId> fra, Set<AktørId> ikkeTil) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getRelasjoner).orElse(Collections.emptyList()).stream()
            .filter(rel -> fra.contains(rel.getAktørId()) && !ikkeTil.contains(rel.getTilAktørId()))
            .collect(Collectors.toList());
    }

    public boolean erPersonstatusEndretForSøkerFør(LocalDate stp) {
        var differ = new RegisterdataDiffsjekker(true);
        return differ.erForskjellPå(hentPersonstatusFør(grunnlag1, søkerAktørId, stp),
            hentPersonstatusFør(grunnlag2, søkerAktørId, stp));
    }

    public boolean erStatsborgerskapEndretForSøkerFør(LocalDate stp) {
        var differ = new RegisterdataDiffsjekker(true);
        return differ.erForskjellPå(hentStatsborgerskapFør(grunnlag1, søkerAktørId, stp),
            hentStatsborgerskapFør(grunnlag2, søkerAktørId, stp));
    }

    public boolean erSøkersAdresseEndretFør(LocalDate stp) {
        var differ = new RegisterdataDiffsjekker(true);
        return differ.erForskjellPå(hentAdresserFør(grunnlag1, Set.of(søkerAktørId), stp),
            hentAdresserFør(grunnlag2, Set.of(søkerAktørId), stp));
    }

    public boolean erSøkersAdresseLandEndretFør(LocalDate stp) {
        var differ = new RegisterdataDiffsjekker(true);
        return differ.erForskjellPå(
            hentAdresserFør(grunnlag1, Set.of(søkerAktørId), stp).stream().map(PersonAdresseEntitet::getLand).collect(Collectors.toSet()),
            hentAdresserFør(grunnlag2, Set.of(søkerAktørId), stp).stream().map(PersonAdresseEntitet::getLand).collect(Collectors.toSet()));
    }

    public boolean erAdresserEndretFør(LocalDate stp) {
        var differ = new RegisterdataDiffsjekker(true);
        return differ.erForskjellPå(hentAdresserFør(grunnlag1, personerSnitt, stp),
            hentAdresserFør(grunnlag2, personerSnitt, stp));
    }

    public boolean erSivilstandEndretForBruker() {
        return !Objects.equals(hentSivilstand(grunnlag1, søkerAktørId), hentSivilstand(grunnlag2, søkerAktørId));
    }

    public boolean erRegionEndretForBruker() {
        return !Objects.equals(hentRegion(grunnlag1, søkerAktørId), hentRegion(grunnlag2, søkerAktørId));
    }

    public boolean erForeldreDødsdatoEndret() {
        Set<AktørId> foreldre = new HashSet<>();
        foreldre.add(søkerAktørId);
        oppgittAnnenPart(grunnlag1).map(OppgittAnnenPartEntitet::getAktørId).ifPresent(foreldre::add);
        oppgittAnnenPart(grunnlag2).map(OppgittAnnenPartEntitet::getAktørId).ifPresent(foreldre::add);
        return !Objects.equals(hentDødsdatoer(grunnlag1, foreldre), hentDødsdatoer(grunnlag2, foreldre));
    }

    public boolean erBarnDødsdatoEndret() {
        return !Objects.equals(hentDødsdatoer(grunnlag1, søkersBarnUnion), hentDødsdatoer(grunnlag2, søkersBarnUnion));
    }

    private List<PersonstatusEntitet> hentPersonstatusFør(PersonopplysningGrunnlagEntitet grunnlag, AktørId person, LocalDate stp) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getPersonstatus).orElse(Collections.emptyList()).stream()
            .filter(ps -> person.equals(ps.getAktørId()))
            .filter(ps -> stp == null || ps.getPeriode().getFomDato().isBefore(stp))
            .collect(Collectors.toList());
    }

    private List<StatsborgerskapEntitet> hentStatsborgerskapFør(PersonopplysningGrunnlagEntitet grunnlag, AktørId person, LocalDate stp) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getStatsborgerskap).orElse(Collections.emptyList()).stream()
            .filter(stb -> person.equals(stb.getAktørId()))
            .filter(stb -> stp == null || stb.getPeriode().getFomDato().isBefore(stp))
            .collect(Collectors.toList());
    }

    private List<PersonAdresseEntitet> hentAdresserFør(PersonopplysningGrunnlagEntitet grunnlag, Set<AktørId> personer, LocalDate stp) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getAdresser).orElse(Collections.emptyList()).stream()
            .filter(adr -> personer.contains(adr.getAktørId()))
            .filter(adr -> stp == null || adr.getPeriode().getFomDato().isBefore(stp))
            .collect(Collectors.toList());
    }

    private Set<SivilstandType> hentSivilstand(PersonopplysningGrunnlagEntitet grunnlag, AktørId person) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getPersonopplysninger).orElse(Collections.emptyList()).stream()
            .filter(po -> person.equals(po.getAktørId()))
            .map(PersonopplysningEntitet::getSivilstand)
            .collect(Collectors.toSet());
    }

    private Set<Region> hentRegion(PersonopplysningGrunnlagEntitet grunnlag, AktørId person) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getPersonopplysninger).orElse(Collections.emptyList()).stream()
            .filter(po -> person.equals(po.getAktørId()))
            .map(PersonopplysningEntitet::getRegion)
            .collect(Collectors.toSet());
    }

    private Set<LocalDate> hentDødsdatoer(PersonopplysningGrunnlagEntitet grunnlag, Set<AktørId> aktuelle) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getPersonopplysninger).orElse(Collections.emptyList()).stream()
            .filter(po -> aktuelle.contains(po.getAktørId()))
            .map(PersonopplysningEntitet::getDødsdato)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private Set<AktørId> finnAlleBarn() {
        Set<AktørId> barn = new HashSet<>();
        barn.addAll(finnBarnaFor(grunnlag1, søkerAktørId));
        barn.addAll(finnBarnaFor(grunnlag2, søkerAktørId));
        return barn;
    }

    private Set<AktørId> finnFellesBarn() {
        var barn1 = finnBarnaFor(grunnlag1, søkerAktørId);
        var barn2 = finnBarnaFor(grunnlag2, søkerAktørId);
        return barn2.stream().filter(barn1::contains).collect(Collectors.toSet());
    }

    private Set<AktørId> fellesAktører() {
        var første = registerVersjon(grunnlag1).map(PersonInformasjonEntitet::getPersonopplysninger).orElse(Collections.emptyList()).stream().map(PersonopplysningEntitet::getAktørId).collect(Collectors.toSet());
        return registerVersjon(grunnlag2).map(PersonInformasjonEntitet::getPersonopplysninger).orElse(Collections.emptyList()).stream()
            .map(PersonopplysningEntitet::getAktørId)
            .filter(første::contains)
            .collect(Collectors.toSet());
    }

    private Optional<AktørId> finnAnnenPart() {
        var første = oppgittAnnenPart(grunnlag1).map(OppgittAnnenPartEntitet::getAktørId);
        var andre = oppgittAnnenPart(grunnlag2).map(OppgittAnnenPartEntitet::getAktørId);
        return første.map(f -> andre.map(f::equals).orElse(Boolean.FALSE) ? f : null);
    }

    private Set<AktørId> finnBarnaFor(PersonopplysningGrunnlagEntitet grunnlag, AktørId forelder) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getRelasjoner).orElse(Collections.emptyList()).stream()
            .filter(rel -> forelder.equals(rel.getAktørId()))
            .filter(rel -> RelasjonsRolleType.BARN.equals(rel.getRelasjonsrolle()))
            .map(PersonRelasjonEntitet::getTilAktørId)
            .collect(Collectors.toSet());
    }

    private Optional<PersonInformasjonEntitet> registerVersjon(PersonopplysningGrunnlagEntitet grunnlag) {
        return grunnlag != null ? grunnlag.getRegisterVersjon() : Optional.empty();
    }

    private Optional<OppgittAnnenPartEntitet> oppgittAnnenPart(PersonopplysningGrunnlagEntitet grunnlag) {
        return grunnlag != null ? grunnlag.getOppgittAnnenPart() : Optional.empty();
    }
}
