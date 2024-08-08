package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class PersonopplysningerAggregat {

    private final AktørId søkerAktørId;
    private final LocalDate skjæringstidspunkt;
    private final List<PersonRelasjonEntitet> alleRelasjoner;
    private final List<PersonstatusEntitet> aktuellePersonstatus;
    private final List<PersonstatusEntitet> overstyrtPersonstatus;
    private final List<PersonstatusEntitet> orginalPersonstatus;
    private final List<OppholdstillatelseEntitet> oppholdstillatelser;

    private final Map<AktørId, PersonopplysningEntitet> personopplysninger;
    private final Map<AktørId, List<PersonAdresseEntitet>> adresser;
    private final Map<AktørId, List<StatsborgerskapEntitet>> statsborgerskap;

    private final OppgittAnnenPartEntitet oppgittAnnenPart;

    public PersonopplysningerAggregat(PersonopplysningGrunnlagEntitet grunnlag, AktørId aktørId, LocalDate forDato, LocalDate skjæringstidspunkt) {
        this(grunnlag, aktørId, DatoIntervallEntitet.fraOgMedTilOgMed(forDato, forDato.plusDays(1)), skjæringstidspunkt);
    }

    public PersonopplysningerAggregat(PersonopplysningGrunnlagEntitet grunnlag, AktørId aktørId, DatoIntervallEntitet forPeriode, LocalDate skjæringstidspunkt) {
        this.søkerAktørId = aktørId;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.oppgittAnnenPart = grunnlag.getOppgittAnnenPart().orElse(null);
        var registerversjon = grunnlag.getRegisterVersjon().orElse(null);
        if (registerversjon != null) {
            this.alleRelasjoner = registerversjon.getRelasjoner();
            this.personopplysninger = registerversjon.getPersonopplysninger().stream().collect(Collectors.toMap(PersonopplysningEntitet::getAktørId, Function.identity()));
            this.adresser = registerversjon.getAdresser().stream()
                    .filter(adr -> erIkkeSøker(aktørId, adr.getAktørId()) ||
                        erGyldigIPeriode(forPeriode, adr.getPeriode()))
                    .collect(Collectors.groupingBy(PersonAdresseEntitet::getAktørId));
            overstyrtPersonstatus = grunnlag.getOverstyrtVersjon().map(PersonInformasjonEntitet::getPersonstatus)
                    .orElse(Collections.emptyList());
            var registerPersonstatus = registerversjon.getPersonstatus()
                .stream()
                .filter(it -> finnesIkkeIOverstyrt(it, overstyrtPersonstatus))
                .toList();
            this.orginalPersonstatus = registerversjon.getPersonstatus()
                    .stream()
                    .filter(it -> !finnesIkkeIOverstyrt(it, overstyrtPersonstatus))
                    .toList();
            this.aktuellePersonstatus = Stream.concat(
                registerPersonstatus.stream(),
                overstyrtPersonstatus.stream())
                    .filter(st -> erIkkeSøker(aktørId, st.getAktørId()) ||
                        erGyldigIPeriode(forPeriode, st.getPeriode()))
                    .toList();
            this.statsborgerskap = registerversjon.getStatsborgerskap().stream()
                    .filter(adr -> erIkkeSøker(aktørId, adr.getAktørId()) ||
                        erGyldigIPeriode(forPeriode, adr.getPeriode()))
                    .collect(Collectors.groupingBy(StatsborgerskapEntitet::getAktørId));
            this.oppholdstillatelser = registerversjon.getOppholdstillatelser().stream()
                .filter(adr -> erGyldigIPeriode(forPeriode, adr.getPeriode()))
                .toList();
        } else {
            this.alleRelasjoner = Collections.emptyList();
            this.personopplysninger = Map.of();
            this.oppholdstillatelser = List.of();
            this.aktuellePersonstatus = Collections.emptyList();
            this.adresser = Map.of();
            this.statsborgerskap = Map.of();
            this.orginalPersonstatus = Collections.emptyList();
            this.overstyrtPersonstatus = Collections.emptyList();
        }
    }

    private boolean finnesIkkeIOverstyrt(PersonstatusEntitet status, List<PersonstatusEntitet> overstyrt) {
        return overstyrt.stream().noneMatch(it -> it.getAktørId().equals(status.getAktørId()) && it.getPeriode().equals(status.getPeriode()));
    }

    private boolean erGyldigIPeriode(DatoIntervallEntitet forPeriode, DatoIntervallEntitet periode) {
        return periode.overlapper(forPeriode);
    }

    private boolean erIkkeSøker(AktørId aktørId, AktørId aktuellAktør) {
        return !aktuellAktør.equals(aktørId);
    }

    public PersonopplysningEntitet getPersonopplysning(AktørId aktørId) {
        return personopplysninger.get(aktørId);
    }

    public List<PersonRelasjonEntitet> getRelasjoner() {
        return Collections.unmodifiableList(alleRelasjoner);
    }

    public List<PersonstatusEntitet> getPersonstatuserFor(AktørId aktørId) {
        return aktuellePersonstatus.stream()
                .filter(ss -> ss.getAktørId().equals(aktørId))
                .toList();
    }

    public PersonstatusEntitet getPersonstatusFor(AktørId aktørId) {
        return aktuellePersonstatus.stream()
            .filter(ss -> ss.getAktørId().equals(aktørId)).max(Comparator.comparing(PersonstatusEntitet::getPeriode)).orElse(null);
    }

    public PersonopplysningEntitet getSøker() {
        return personopplysninger.get(søkerAktørId);
    }

    public List<StatsborgerskapEntitet> getStatsborgerskapFor(AktørId aktørId) {
        return Collections.unmodifiableList(statsborgerskap.getOrDefault(aktørId, List.of()));
    }

    public Optional<StatsborgerskapEntitet> getRangertStatsborgerskapVedSkjæringstidspunktFor(AktørId aktørId) {
        return statsborgerskap.getOrDefault(aktørId, List.of()).stream()
            .min(Comparator.comparing(s -> MapRegionLandkoder.mapLandkodeForDatoMedSkjæringsdato(s.getStatsborgerskap(), skjæringstidspunkt, skjæringstidspunkt).getRank()));
    }

    public Region getStatsborgerskapRegionVedTidspunkt(AktørId aktørId, LocalDate vurderingsdato) {
        return statsborgerskap.getOrDefault(aktørId, List.of()).stream()
            .filter(s -> s.getPeriode().inkluderer(vurderingsdato))
            .map(StatsborgerskapEntitet::getStatsborgerskap)
            .map(s -> MapRegionLandkoder.mapLandkodeForDatoMedSkjæringsdato(s, vurderingsdato, skjæringstidspunkt))
            .min(Comparator.comparing(Region::getRank)).orElse(Region.TREDJELANDS_BORGER);
    }

    public Region getStatsborgerskapRegionVedSkjæringstidspunkt(AktørId aktørId) {
        return statsborgerskap.getOrDefault(aktørId, List.of()).stream()
            .filter(s -> s.getPeriode().inkluderer(skjæringstidspunkt))
            .map(StatsborgerskapEntitet::getStatsborgerskap)
            .map(s -> MapRegionLandkoder.mapLandkodeForDatoMedSkjæringsdato(s, skjæringstidspunkt, skjæringstidspunkt))
            .min(Comparator.comparing(Region::getRank)).orElse(Region.TREDJELANDS_BORGER);
    }

    public List<StatsborgerskapEntitet> getStatsborgerskap(AktørId aktørId) {
        return statsborgerskap.getOrDefault(aktørId, List.of());
    }

    public boolean harStatsborgerskap(AktørId aktørId, Landkoder land) {
        return statsborgerskap.getOrDefault(aktørId, List.of()).stream()
            .anyMatch(sb -> land.equals(sb.getStatsborgerskap()));
    }

    public boolean harStatsborgerskapRegionVedSkjæringstidspunkt(AktørId aktørId, Region region) {
        return statsborgerskap.getOrDefault(aktørId, List.of()).stream()
            .anyMatch(sb -> region.equals(MapRegionLandkoder.mapLandkodeForDatoMedSkjæringsdato(sb.getStatsborgerskap(), skjæringstidspunkt, skjæringstidspunkt)));
    }

    public boolean harStatsborgerskapRegionVedTidspunkt(AktørId aktørId, Region region, LocalDate vurderingsdato) {
        return statsborgerskap.getOrDefault(aktørId, List.of()).stream()
            .anyMatch(sb -> region.equals(MapRegionLandkoder.mapLandkodeForDatoMedSkjæringsdato(sb.getStatsborgerskap(), vurderingsdato, skjæringstidspunkt)));
    }

    public Optional<OppholdstillatelseEntitet> getOppholdstillatelseFor(AktørId aktørId) {
        List<OppholdstillatelseEntitet> aktuelle = søkerAktørId.equals(aktørId) ? oppholdstillatelser : List.of();
        return aktuelle.stream().max(Comparator.comparing(OppholdstillatelseEntitet::getPeriode));
    }

    /**
     * Returnerer opprinnelig personstatus der hvor personstatus har blitt overstyrt
     *
     * @return personstatus
     * @param søkerAktørId
     */
    public Optional<PersonstatusEntitet> getOrginalPersonstatusFor(AktørId søkerAktørId) {
        return orginalPersonstatus.stream()
            .filter(ss -> ss.getAktørId().equals(søkerAktørId))
            .max(Comparator.comparing(PersonstatusEntitet::getPeriode));
    }

    public List<PersonAdresseEntitet> getAdresserFor(AktørId aktørId) {
        return adresser.getOrDefault(aktørId, List.of()).stream()
            .sorted(Comparator.comparing(PersonAdresseEntitet::getPeriode).reversed())
            .toList();
    }

    public List<PersonopplysningEntitet> getBarna() {
        return getBarnaTil(søkerAktørId);
    }

    public List<PersonopplysningEntitet> getBarnaTil(AktørId aktørId) {
        return getTilPersonerFor(aktørId, RelasjonsRolleType.BARN);
    }

    public List<PersonopplysningEntitet> getFellesBarn() {
        var annenPart = getAnnenPart();
        List<PersonopplysningEntitet> fellesBarn = new ArrayList<>();
        if (annenPart.isPresent()) {
            fellesBarn.addAll(getBarna());
            fellesBarn.retainAll(getBarnaTil(annenPart.get().getAktørId()));
        }
        return fellesBarn;
    }

    public Optional<PersonopplysningEntitet> getAnnenPart() {
        return getOppgittAnnenPart().map(OppgittAnnenPartEntitet::getAktørId).map(personopplysninger::get);
    }

    public Optional<PersonopplysningEntitet> getEktefelle() {
        return getTilPersonerFor(søkerAktørId, RelasjonsRolleType.EKTE).stream().findFirst();
    }

    public Optional<PersonopplysningEntitet> getAnnenPartEllerEktefelle() {
        return getAnnenPart().or(this::getEktefelle);
    }

    public List<PersonRelasjonEntitet> getSøkersRelasjoner() {
        return finnRelasjon(søkerAktørId);
    }

    public Map<AktørId, PersonopplysningEntitet> getAktørPersonopplysningMap() {
        return Collections.unmodifiableMap(personopplysninger);
    }

    public List<PersonopplysningEntitet> getTilPersonerFor(AktørId fraAktørId, RelasjonsRolleType relasjonsRolleType) {
        return alleRelasjoner.stream()
            .filter(e -> e.getRelasjonsrolle().equals(relasjonsRolleType) && e.getAktørId().equals(fraAktørId))
            .map(PersonRelasjonEntitet::getTilAktørId)
            .map(personopplysninger::get)
            .filter(Objects::nonNull)
            .toList();
    }

    public Optional<PersonRelasjonEntitet> finnRelasjon(AktørId fraAktørId, AktørId tilAktørId) {
        return getRelasjoner().stream()
                .filter(e -> e.getAktørId().equals(fraAktørId) && e.getTilAktørId().equals(tilAktørId))
                .findFirst();
    }

    public List<PersonRelasjonEntitet> finnRelasjon(AktørId fraAktørId) {
        return getRelasjoner().stream()
                .filter(e -> e.getAktørId().equals(fraAktørId))
                .toList();
    }

    public Optional<OppgittAnnenPartEntitet> getOppgittAnnenPart() {
        return Optional.ofNullable(oppgittAnnenPart);
    }

    public List<PersonopplysningEntitet> getAlleBarnFødtI(SimpleLocalDateInterval fødselIntervall) {
        return getBarnaTil(søkerAktørId).stream()
                .filter(barn -> fødselIntervall.overlapper(SimpleLocalDateInterval.fraOgMedTomNotNull(barn.getFødselsdato(), barn.getFødselsdato())))
                .toList();
    }

    public boolean søkerHarSammeAdresseSom(AktørId aktørId, RelasjonsRolleType relasjonsRolle) {
        var ektefelleRelasjon = getRelasjoner().stream()
                .filter(familierelasjon -> familierelasjon.getAktørId().equals(søkerAktørId) &&
                    familierelasjon.getTilAktørId().equals(aktørId) &&
                    familierelasjon.getRelasjonsrolle().equals(relasjonsRolle))
                .findFirst();
        if (ektefelleRelasjon.filter(PersonRelasjonEntitet::getHarSammeBosted).isPresent()) {
            return true;
        }
        return harSammeAdresseSom(aktørId);
    }

    private boolean harSammeAdresseSom(AktørId aktørId) {
        if (personopplysninger.get(aktørId) == null) {
            return false;
        }

        for (var opplysningAdresseSøker : getAdresserFor(søkerAktørId)) {
            for (var opplysningAdresseAnnenpart : getAdresserFor(aktørId)) {
                var sammeperiode = opplysningAdresseSøker.getPeriode().overlapper(opplysningAdresseAnnenpart.getPeriode());
                if (sammeperiode && PersonAdresseEntitet.likeAdresser(opplysningAdresseSøker, opplysningAdresseAnnenpart)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (PersonopplysningerAggregat) o;
        return Objects.equals(søkerAktørId, that.søkerAktørId) &&
                Objects.equals(personopplysninger, that.personopplysninger) &&
                Objects.equals(alleRelasjoner, that.alleRelasjoner) &&
                Objects.equals(adresser, that.adresser) &&
                Objects.equals(aktuellePersonstatus, that.aktuellePersonstatus) &&
                Objects.equals(statsborgerskap, that.statsborgerskap) &&
                Objects.equals(oppgittAnnenPart, that.oppgittAnnenPart);
    }


    @Override
    public int hashCode() {
        return Objects.hash(søkerAktørId, personopplysninger, alleRelasjoner, adresser, aktuellePersonstatus, statsborgerskap,
            oppgittAnnenPart);
    }


    @Override
    public String toString() {
        return "PersonopplysningerAggregat{" +
                "søkerAktørId=" + søkerAktørId +
                ", personopplysninger=" + personopplysninger +
                ", oppgittAnnenPart=" + oppgittAnnenPart +
                '}';
    }
}
