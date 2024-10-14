package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

public class PersonopplysningerAggregat {

    private final AktørId søkerAktørId;
    private final List<PersonRelasjonEntitet> alleRelasjoner;
    private final List<PersonstatusEntitet> aktuellePersonstatus;
    private final List<OppholdstillatelseEntitet> oppholdstillatelser;

    private final Map<AktørId, PersonopplysningEntitet> personopplysninger;
    private final Map<AktørId, List<PersonAdresseEntitet>> adresser;
    private final Map<AktørId, List<StatsborgerskapEntitet>> statsborgerskap;

    private final OppgittAnnenPartEntitet oppgittAnnenPart;
    private final boolean innhentetPersonopplysningerFraRegister;

    public PersonopplysningerAggregat(PersonopplysningGrunnlagEntitet grunnlag, AktørId aktørId) {
        this.søkerAktørId = aktørId;
        this.oppgittAnnenPart = grunnlag.getOppgittAnnenPart().orElse(null);
        this.personopplysninger = grunnlag.getRegisterVersjon().map(PersonInformasjonEntitet::getPersonopplysninger).orElse(List.of()).stream()
            .collect(Collectors.toMap(PersonopplysningEntitet::getAktørId, Function.identity()));
        this.alleRelasjoner = grunnlag.getRegisterVersjon().map(PersonInformasjonEntitet::getRelasjoner).orElse(List.of());
        this.oppholdstillatelser = grunnlag.getRegisterVersjon().map(PersonInformasjonEntitet::getOppholdstillatelser).orElse(List.of());
        this.adresser =  grunnlag.getRegisterVersjon().map(PersonInformasjonEntitet::getAdresser).orElse(List.of()).stream()
            .collect(Collectors.groupingBy(PersonAdresseEntitet::getAktørId));
        this.statsborgerskap = grunnlag.getRegisterVersjon().map(PersonInformasjonEntitet::getStatsborgerskap).orElse(List.of()).stream()
            .collect(Collectors.groupingBy(StatsborgerskapEntitet::getAktørId));
        var overstyrtPersonstatus = grunnlag.getOverstyrtVersjon().map(PersonInformasjonEntitet::getPersonstatus).orElse(List.of());
        var registerPersonstatus = grunnlag.getRegisterVersjon().map(PersonInformasjonEntitet::getPersonstatus).orElse(List.of()).stream()
            .filter(it -> finnesIkkeIOverstyrt(it, overstyrtPersonstatus))
            .toList();
        this.aktuellePersonstatus = Stream.concat(registerPersonstatus.stream(), overstyrtPersonstatus.stream())
            .toList();
        this.innhentetPersonopplysningerFraRegister = grunnlag.getRegisterVersjon().isPresent();
    }

    private boolean finnesIkkeIOverstyrt(PersonstatusEntitet status, List<PersonstatusEntitet> overstyrt) {
        return overstyrt.stream().noneMatch(it -> it.getAktørId().equals(status.getAktørId()) && it.getPeriode().equals(status.getPeriode()));
    }

    private boolean erGyldigIPeriode(AbstractLocalDateInterval forPeriode, AbstractLocalDateInterval periode) {
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

    public List<PersonstatusEntitet> getPersonstatuserFor(AktørId aktørId, AbstractLocalDateInterval forPeriode) {
        return aktuellePersonstatus.stream()
            .filter(st -> erIkkeSøker(aktørId, st.getAktørId()) || erGyldigIPeriode(forPeriode, st.getPeriode()))
            .filter(ss -> ss.getAktørId().equals(aktørId))
            .toList();
    }

    public PersonstatusEntitet getPersonstatusFor(AktørId aktørId, AbstractLocalDateInterval forPeriode) {
        return getPersonstatuserFor(aktørId, forPeriode).stream()
            .max(Comparator.comparing(PersonstatusEntitet::getPeriode)).orElse(null);
    }

    public PersonopplysningEntitet getSøker() {
        return personopplysninger.get(søkerAktørId);
    }

    public List<StatsborgerskapEntitet> getStatsborgerskapFor(AktørId aktørId, AbstractLocalDateInterval forPeriode) {
        return statsborgerskap.getOrDefault(aktørId, List.of()).stream()
                .filter(adr -> erIkkeSøker(aktørId, adr.getAktørId()) || erGyldigIPeriode(forPeriode, adr.getPeriode()))
                .toList();
    }

    public Optional<StatsborgerskapEntitet> getRangertStatsborgerskapVedSkjæringstidspunktFor(AktørId aktørId, LocalDate skjæringstidspunkt) {
        return getStatsborgerskapFor(aktørId, SimpleLocalDateInterval.enDag(skjæringstidspunkt)).stream()
            .min(Comparator.comparing(s -> MapRegionLandkoder.mapLandkodeForDatoMedSkjæringsdato(s.getStatsborgerskap(), skjæringstidspunkt, skjæringstidspunkt).getRank()));
    }

    public LocalDateTimeline<Region> getStatsborgerskapRegionIInterval(AktørId aktørId, AbstractLocalDateInterval interval, LocalDate skjæringstidspunkt) {
        var segments = statsborgerskap.getOrDefault(aktørId, List.of())
            .stream()
            .filter(s -> s.getPeriode().overlapper(interval))
            .map(s -> {
                var region = MapRegionLandkoder.mapLandkodeForDatoMedSkjæringsdato(s.getStatsborgerskap(), interval.getFomDato(), skjæringstidspunkt);
                return new LocalDateSegment<>(new LocalDateInterval(s.getPeriode().getFomDato(), s.getPeriode().getTomDato()), region);
            })
            .collect(Collectors.toSet());

        return new LocalDateTimeline<>(segments, (datoInterval, datoSegment, datoSegment2) -> {
            var prioritertRegion =
                Region.COMPARATOR.compare(datoSegment.getValue(), datoSegment2.getValue()) < 0 ? datoSegment.getValue() : datoSegment2.getValue();
            return new LocalDateSegment<>(datoInterval, prioritertRegion);
        });
    }

    public Region getStatsborgerskapRegionVedSkjæringstidspunkt(AktørId aktørId, LocalDate skjæringstidspunkt) {
        return statsborgerskap.getOrDefault(aktørId, List.of()).stream()
            .filter(s -> s.getPeriode().inkluderer(skjæringstidspunkt))
            .map(StatsborgerskapEntitet::getStatsborgerskap)
            .map(s -> MapRegionLandkoder.mapLandkodeForDatoMedSkjæringsdato(s, skjæringstidspunkt, skjæringstidspunkt))
            .min(Region.COMPARATOR).orElse(Region.TREDJELANDS_BORGER);
    }

    public Optional<OppholdstillatelseEntitet> getSisteOppholdstillatelseFor(AktørId aktørId, AbstractLocalDateInterval forPeriode) {
        return getOppholdstillatelseFor(aktørId, forPeriode).stream().max(Comparator.comparing(OppholdstillatelseEntitet::getPeriode));
    }

    public List<OppholdstillatelseEntitet> getOppholdstillatelseFor(AktørId aktørId, AbstractLocalDateInterval forPeriode) {
        List<OppholdstillatelseEntitet> liste = søkerAktørId.equals(aktørId) ? oppholdstillatelser : List.of();
        return liste.stream()
            .filter(ot -> erGyldigIPeriode(forPeriode, ot.getPeriode()))
            .toList();
    }

    public List<PersonAdresseEntitet> getAdresserFor(AktørId aktørId, AbstractLocalDateInterval forPeriode) {
        return adresser.getOrDefault(aktørId, List.of()).stream()
            .filter(adr -> erIkkeSøker(aktørId, adr.getAktørId()) || erGyldigIPeriode(forPeriode, adr.getPeriode()))
            .sorted(Comparator.comparing(PersonAdresseEntitet::getPeriode).reversed())
            .toList();
    }

    public List<PersonopplysningEntitet> getBarna() {
        return getBarnaTil(søkerAktørId);
    }

    public List<PersonopplysningEntitet> getBarnaTil(AktørId aktørId) {
        return getTilPersonerFor(aktørId, RelasjonsRolleType.BARN);
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

    private List<PersonopplysningEntitet> getTilPersonerFor(AktørId fraAktørId, RelasjonsRolleType relasjonsRolleType) {
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

    private List<PersonRelasjonEntitet> finnRelasjon(AktørId fraAktørId) {
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

    public boolean søkerHarSammeAdresseSom(AktørId aktørId, RelasjonsRolleType relasjonsRolle, AbstractLocalDateInterval forPeriode) {
        var ektefelleRelasjon = getRelasjoner().stream()
                .filter(familierelasjon -> familierelasjon.getAktørId().equals(søkerAktørId) &&
                    familierelasjon.getTilAktørId().equals(aktørId) &&
                    familierelasjon.getRelasjonsrolle().equals(relasjonsRolle))
                .findFirst();
        if (ektefelleRelasjon.filter(PersonRelasjonEntitet::getHarSammeBosted).isPresent()) {
            return true;
        }
        return harSammeAdresseSom(aktørId, forPeriode);
    }

    private boolean harSammeAdresseSom(AktørId aktørId, AbstractLocalDateInterval forPeriode) {
        if (personopplysninger.get(aktørId) == null) {
            return false;
        }

        for (var opplysningAdresseSøker : getAdresserFor(søkerAktørId, forPeriode)) {
            for (var opplysningAdresseAnnenpart : getAdresserFor(aktørId, forPeriode)) {
                var sammeperiode = opplysningAdresseSøker.getPeriode().overlapper(opplysningAdresseAnnenpart.getPeriode());
                if (sammeperiode && PersonAdresseEntitet.likeAdresser(opplysningAdresseSøker, opplysningAdresseAnnenpart)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean harInnhentetPersonopplysningerFraRegister() {
        return innhentetPersonopplysningerFraRegister;
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
