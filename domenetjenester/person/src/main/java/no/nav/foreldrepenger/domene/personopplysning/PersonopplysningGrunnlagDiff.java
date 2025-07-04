package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
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
import no.nav.foreldrepenger.behandlingslager.diff.Node;
import no.nav.foreldrepenger.behandlingslager.diff.Pair;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class PersonopplysningGrunnlagDiff {

    private static final Comparator<PersonAdresseEntitet> COMP_ADRESSE = Comparator.comparing(PersonAdresseEntitet::getAktørId).
        thenComparing(PersonAdresseEntitet::getAdresseType).thenComparing(PersonAdresseEntitet::getFom, Comparator.nullsFirst(Comparator.naturalOrder()));

    private static final Set<PersonstatusType> BOSATT_TYPER = Set.of(PersonstatusType.BOSA, PersonstatusType.DØD, PersonstatusType.ADNR);

    private final AktørId søkerAktørId;
    private final Optional<AktørId> annenPartAktørId;
    private final PersonopplysningGrunnlagEntitet grunnlag1;
    private final PersonopplysningGrunnlagEntitet grunnlag2;
    private final Set<AktørId> søkersBarnUnion;
    private final Set<AktørId> søkersBarnSnitt;
    private final Set<AktørId> søkersBarnDiff;

    public PersonopplysningGrunnlagDiff(AktørId søker, PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2) {
        this.søkerAktørId = søker;
        this.grunnlag1 = grunnlag1;
        this.grunnlag2 = grunnlag2;
        søkersBarnUnion = finnAlleBarn();
        søkersBarnSnitt = finnFellesBarn();
        annenPartAktørId = finnAnnenPart();
        søkersBarnDiff = søkersBarnUnion.stream().filter(barn -> !søkersBarnSnitt.contains(barn)).collect(Collectors.toSet());
    }

    public boolean erRelasjonerEndret() {
        var differ = new RegisterdataDiffsjekker(true);
        var relasjoner1 = registerVersjon(grunnlag1).map(PersonInformasjonEntitet::getRelasjoner).orElse(List.of()).stream()
            .sorted(Comparator.comparing(PersonRelasjonEntitet::getAktørId).thenComparing(PersonRelasjonEntitet::getTilAktørId))
            .toList();
        var relasjoner2 = registerVersjon(grunnlag2).map(PersonInformasjonEntitet::getRelasjoner).orElse(List.of()).stream()
            .sorted(Comparator.comparing(PersonRelasjonEntitet::getAktørId).thenComparing(PersonRelasjonEntitet::getTilAktørId))
            .toList();
        return differ.erForskjellPå(relasjoner1, relasjoner2);
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
        return !Objects.equals(hentRelaterteFraMenIkkeTil(grunnlag1, fra, ikkeTil), hentRelaterteFraMenIkkeTil(grunnlag2, fra, ikkeTil));
    }

    private Set<AktørId> hentRelaterteFraMenIkkeTil(PersonopplysningGrunnlagEntitet grunnlag, Set<AktørId> fra, Set<AktørId> ikkeTil) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getRelasjoner).orElse(Collections.emptyList()).stream()
            .filter(rel -> fra.contains(rel.getAktørId()) && !ikkeTil.contains(rel.getTilAktørId()))
            .map(PersonRelasjonEntitet::getTilAktørId)
            .collect(Collectors.toSet());
    }

    public boolean erRelasjonerBostedEndretForSøkerUtenomNyeBarn() {
        var differ = new RegisterdataDiffsjekker(true);
        return differ.erForskjellPå(hentRelasjonerFraMenIkkeTil(grunnlag1, Set.of(søkerAktørId), søkersBarnDiff),
            hentRelasjonerFraMenIkkeTil(grunnlag2, Set.of(søkerAktørId), søkersBarnDiff));
    }

    private List<PersonRelasjonEntitet> hentRelasjonerFraMenIkkeTil(PersonopplysningGrunnlagEntitet grunnlag, Set<AktørId> fra, Set<AktørId> ikkeTil) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getRelasjoner).orElse(Collections.emptyList()).stream()
            .filter(rel -> fra.contains(rel.getAktørId()) && !ikkeTil.contains(rel.getTilAktørId()))
            .sorted(Comparator.comparing(PersonRelasjonEntitet::getAktørId).thenComparing(PersonRelasjonEntitet::getTilAktørId))
            .toList();
    }

    public boolean erPersonstatusIkkeBosattEndretForSøkerPeriode(DatoIntervallEntitet periode) {
        var differ = new RegisterdataDiffsjekker(true);
        return differ.erForskjellPå(
            hentIkkeBosattPersonstatusForPeriode(grunnlag1, søkerAktørId, periode),
            hentIkkeBosattPersonstatusForPeriode(grunnlag2, søkerAktørId, periode));
    }

    public boolean erRegionEndretForSøkerPeriode(DatoIntervallEntitet periode, LocalDate skjæringstidspunkt) {
        return !Objects.equals(hentRangertRegion(grunnlag1, søkerAktørId, periode, skjæringstidspunkt),
            hentRangertRegion(grunnlag2, søkerAktørId, periode, skjæringstidspunkt));
    }

    public boolean erSøkersUtlandsAdresserEndretIPeriode(DatoIntervallEntitet periode) {
        var differ = new RegisterdataDiffsjekker(true);
        return differ.erForskjellPåFiltrert(
            hentUtlandAdresserForPeriode(grunnlag1, Set.of(søkerAktørId), periode),
            hentUtlandAdresserForPeriode(grunnlag2, Set.of(søkerAktørId), periode),
            PersonopplysningGrunnlagDiff::beholdeUtlandsadresseUtenomEndringLandLinje3);
    }

    // Kompensere for endret konvensjon med landkode i adresselinje3 bortfalt høst 2024. TODO forenkle vår 2026 - endre til normal diff
    private static boolean beholdeUtlandsadresseUtenomEndringLandLinje3(Map.Entry<Node, Pair> entry) {
        if (Optional.ofNullable(entry.getKey().getLocalName()).orElse("").contains("adresselinje3")) {
            var al3 = entry.getValue();
            return al3 != null && al3.getElement1() != null && al3.getElement2() != null;
        } else if (Optional.ofNullable(entry.getKey().getLocalName()).orElse("").contains("adresselinje4")) {
            var al4 = entry.getValue();
            return al4 != null && al4.getElement1() != null && al4.getElement2() != null;
        } else {
            return true;
        }
    }

    public boolean erSivilstandEndretForBruker() {
        return !Objects.equals(hentSivilstand(grunnlag1, søkerAktørId), hentSivilstand(grunnlag2, søkerAktørId));
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

    private Set<PersonstatusEntitet> hentIkkeBosattPersonstatusForPeriode(PersonopplysningGrunnlagEntitet grunnlag, AktørId person, DatoIntervallEntitet periode) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getPersonstatus).orElse(Collections.emptyList()).stream()
            .filter(ps -> person.equals(ps.getAktørId()))
            .filter(ps -> ps.getPeriode().overlapper(periode))
            .filter(ps -> !BOSATT_TYPER.contains(ps.getPersonstatus()))
            .collect(Collectors.toSet());
    }

    private List<PersonAdresseEntitet> hentUtlandAdresserForPeriode(PersonopplysningGrunnlagEntitet grunnlag, Set<AktørId> personer, DatoIntervallEntitet periode) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getAdresser).orElse(Collections.emptyList()).stream()
            .filter(adr -> personer.contains(adr.getAktørId()))
            .filter(adr -> adr.getPeriode().overlapper(periode))
            .filter(adr -> !Landkoder.NOR.equals(adr.getLand()))
            .sorted(COMP_ADRESSE)
            .toList();
    }

    private Set<SivilstandType> hentSivilstand(PersonopplysningGrunnlagEntitet grunnlag, AktørId person) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getPersonopplysninger).orElse(Collections.emptyList()).stream()
            .filter(po -> person.equals(po.getAktørId()))
            .map(PersonopplysningEntitet::getSivilstand)
            .collect(Collectors.toSet());
    }

    private Region hentRangertRegion(PersonopplysningGrunnlagEntitet grunnlag, AktørId person, DatoIntervallEntitet periode, LocalDate skjæringstidspunkt) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getStatsborgerskap).orElse(Collections.emptyList()).stream()
            .filter(stb -> person.equals(stb.getAktørId()))
            .filter(stb -> stb.getPeriode().overlapper(periode))
            .map(StatsborgerskapEntitet::getStatsborgerskap)
            .map(l -> MapRegionLandkoder.mapLandkodeForDatoMedSkjæringsdato(l, periode.getFomDato(), skjæringstidspunkt))
            .min(Comparator.comparing(Region::getRank)).orElse(Region.UDEFINERT);
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

    private Optional<AktørId> finnAnnenPart() {
        var første = oppgittAnnenPart(grunnlag1).map(OppgittAnnenPartEntitet::getAktørId);
        var andre = oppgittAnnenPart(grunnlag2).map(OppgittAnnenPartEntitet::getAktørId);
        return første.map(f -> andre.filter(f::equals).isPresent() ? f : null);
    }

    private Set<AktørId> finnBarnaFor(PersonopplysningGrunnlagEntitet grunnlag, AktørId forelder) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getRelasjoner).orElse(Collections.emptyList()).stream()
            .filter(rel -> forelder.equals(rel.getAktørId()))
            .filter(rel -> RelasjonsRolleType.BARN.equals(rel.getRelasjonsrolle()))
            .map(PersonRelasjonEntitet::getTilAktørId)
            .collect(Collectors.toSet());
    }

    private Optional<PersonInformasjonEntitet> registerVersjon(PersonopplysningGrunnlagEntitet grunnlag) {
        return Optional.ofNullable(grunnlag).flatMap(PersonopplysningGrunnlagEntitet::getRegisterVersjon);
    }

    private Optional<OppgittAnnenPartEntitet> oppgittAnnenPart(PersonopplysningGrunnlagEntitet grunnlag) {
        return Optional.ofNullable(grunnlag).flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart);
    }
}
