package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.time.LocalDate;
import java.time.ZoneId;
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

import org.threeten.extra.Interval;

import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class PersonopplysningerAggregat {

    private final AktørId søkerAktørId;
    private final List<PersonRelasjonEntitet> alleRelasjoner;
    private final List<PersonstatusEntitet> aktuellePersonstatus;
    private final List<PersonstatusEntitet> overstyrtPersonstatus;
    private final List<PersonstatusEntitet> orginalPersonstatus;
    private final List<OppholdstillatelseEntitet> oppholdstillatelser;

    private final Map<AktørId, PersonopplysningEntitet> personopplysninger;
    private final Map<AktørId, List<PersonAdresseEntitet>> adresser;
    private final Map<AktørId, List<StatsborgerskapEntitet>> statsborgerskap;

    private final OppgittAnnenPartEntitet oppgittAnnenPart;

    public PersonopplysningerAggregat(PersonopplysningGrunnlagEntitet grunnlag, AktørId aktørId, DatoIntervallEntitet forPeriode) {
        this.søkerAktørId = aktørId;
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
            final List<PersonstatusEntitet> registerPersonstatus = registerversjon.getPersonstatus()
                    .stream()
                    .filter(it -> finnesIkkeIOverstyrt(it, overstyrtPersonstatus))
                    .collect(Collectors.toList());
            this.orginalPersonstatus = registerversjon.getPersonstatus()
                    .stream()
                    .filter(it -> !finnesIkkeIOverstyrt(it, overstyrtPersonstatus))
                    .collect(Collectors.toList());
            this.aktuellePersonstatus = Stream.concat(
                registerPersonstatus.stream(),
                overstyrtPersonstatus.stream())
                    .filter(st -> erIkkeSøker(aktørId, st.getAktørId()) ||
                        erGyldigIPeriode(forPeriode, st.getPeriode()))
                    .collect(Collectors.toList());
            this.statsborgerskap = registerversjon.getStatsborgerskap().stream()
                    .filter(adr -> erIkkeSøker(aktørId, adr.getAktørId()) ||
                        erGyldigIPeriode(forPeriode, adr.getPeriode()))
                    .peek(sb -> sb.setRegion(MapRegionLandkoder.mapLandkode(sb.getStatsborgerskap().getKode())))
                    .collect(Collectors.groupingBy(StatsborgerskapEntitet::getAktørId));
            this.oppholdstillatelser = registerversjon.getOppholdstillatelser().stream()
                .filter(adr -> erGyldigIPeriode(forPeriode, adr.getPeriode()))
                .collect(Collectors.toList());
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
        return periode.tilIntervall().overlaps(forPeriode.tilIntervall());
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
                .collect(Collectors.toList());
    }

    public PersonstatusEntitet getPersonstatusFor(AktørId aktørId) {
        return aktuellePersonstatus.stream()
            .filter(ss -> ss.getAktørId().equals(aktørId))
            .sorted(Comparator.comparing(PersonstatusEntitet::getPeriode).reversed())
            .findFirst().orElse(null);
    }

    public PersonopplysningEntitet getSøker() {
        return personopplysninger.get(søkerAktørId);
    }

    public List<StatsborgerskapEntitet> getStatsborgerskapFor(AktørId aktørId) {
        return statsborgerskap.getOrDefault(aktørId, List.of()).stream()
            .sorted(Comparator.comparing(s -> s.getRegion().getRank()))
            .collect(Collectors.toList());
    }

    public Region getStatsborgerskapRegionFor(AktørId aktørId) {
        return statsborgerskap.getOrDefault(aktørId, List.of()).stream()
            .min(Comparator.comparing(s -> s.getRegion().getRank()))
            .map(StatsborgerskapEntitet::getRegion).orElse(Region.TREDJELANDS_BORGER);
    }

    public boolean harStatsborgerskap(AktørId aktørId, Landkoder land) {
        return statsborgerskap.getOrDefault(aktørId, List.of()).stream()
            .anyMatch(sb -> land.equals(sb.getStatsborgerskap()));
    }

    public boolean harStatsborgerskapRegion(AktørId aktørId, Region region) {
        return statsborgerskap.getOrDefault(aktørId, List.of()).stream()
            .anyMatch(sb -> region.equals(sb.getRegion()));
    }

    public List<OppholdstillatelseEntitet> getOppholdstillatelser(AktørId aktørId) {
        return søkerAktørId.equals(aktørId) ? oppholdstillatelser : List.of();
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
                .sorted(Comparator.comparing(PersonstatusEntitet::getPeriode).reversed())
                .findFirst();
    }

    public String getNavn() {
        return getSøker().getNavn();
    }

    public List<PersonAdresseEntitet> getAdresserFor(AktørId aktørId) {
        return adresser.getOrDefault(aktørId, List.of()).stream()
            .sorted(Comparator.comparing(PersonAdresseEntitet::getPeriode).reversed())
            .collect(Collectors.toList());
    }

    public List<PersonopplysningEntitet> getBarna() {
        return getBarnaTil(søkerAktørId);
    }

    public List<PersonopplysningEntitet> getBarnaTil(AktørId aktørId) {
        return getTilPersonerFor(aktørId, RelasjonsRolleType.BARN);
    }

    public List<PersonopplysningEntitet> getFellesBarn() {
        Optional<PersonopplysningEntitet> annenPart = getAnnenPart();
        List<PersonopplysningEntitet> fellesBarn = new ArrayList<>();
        if (annenPart.isPresent()) {
            fellesBarn.addAll(getBarna());
            fellesBarn.retainAll(getBarnaTil(annenPart.get().getAktørId()));
        }
        return fellesBarn;
    }

    public Optional<PersonopplysningEntitet> getAnnenPart() {
        var annenpart = getOppgittAnnenPart().map(ap -> personopplysninger.get(ap.getAktørId())).orElse(null);
        return Optional.ofNullable(annenpart);
    }

    public Optional<PersonopplysningEntitet> getEktefelle() {
        return getTilPersonerFor(søkerAktørId, RelasjonsRolleType.EKTE).stream().findFirst();
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
            .collect(Collectors.toList());
    }

    public Optional<PersonRelasjonEntitet> finnRelasjon(AktørId fraAktørId, AktørId tilAktørId) {
        return getRelasjoner().stream()
                .filter(e -> e.getAktørId().equals(fraAktørId) && e.getTilAktørId().equals(tilAktørId))
                .findFirst();
    }

    public List<PersonRelasjonEntitet> finnRelasjon(AktørId fraAktørId) {
        return getRelasjoner().stream()
                .filter(e -> e.getAktørId().equals(fraAktørId))
                .collect(Collectors.toList());
    }

    public Optional<OppgittAnnenPartEntitet> getOppgittAnnenPart() {
        return Optional.ofNullable(oppgittAnnenPart);
    }

    public List<PersonopplysningEntitet> getAlleBarnFødtI(Interval fødselIntervall) {
        return getBarnaTil(søkerAktørId).stream()
                .filter(barn -> fødselIntervall.overlaps(byggInterval(barn.getFødselsdato())))
                .collect(Collectors.toList());
    }

    private Interval byggInterval(LocalDate dato) {
        return Interval.of(dato.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant(),
            dato.atStartOfDay().plusDays(1).atZone(ZoneId.systemDefault()).toInstant());
    }

    public boolean søkerHarSammeAdresseSom(AktørId aktørId, RelasjonsRolleType relasjonsRolle) {
        Optional<PersonRelasjonEntitet> ektefelleRelasjon = getRelasjoner().stream()
                .filter(familierelasjon -> familierelasjon.getAktørId().equals(søkerAktørId) &&
                    familierelasjon.getTilAktørId().equals(aktørId) &&
                    familierelasjon.getRelasjonsrolle().equals(relasjonsRolle))
                .findFirst();
        // Overgang DSF->FREG gir !harSammeBosted fra TPS
        if (ektefelleRelasjon.filter(PersonRelasjonEntitet::getHarSammeBosted).isPresent()) {
            return true;
        }
        return harSammeAdresseSom(aktørId);
    }

    private boolean harSammeAdresseSom(AktørId aktørId) {
        if (personopplysninger.get(aktørId) == null) {
            return false;
        }

        for (PersonAdresseEntitet opplysningAdresseSøker : getAdresserFor(søkerAktørId)) {
            for (PersonAdresseEntitet opplysningAdresseAnnenpart : getAdresserFor(aktørId)) {
                var sammeperiode = opplysningAdresseSøker.getPeriode().overlapper(opplysningAdresseAnnenpart.getPeriode());
                var sammeMatrikkel = likAdresseIgnoringCase(opplysningAdresseSøker.getMatrikkelId(), opplysningAdresseAnnenpart.getMatrikkelId());
                if (sammeperiode && (sammeMatrikkel || (likAdresseIgnoringCase(opplysningAdresseSøker.getAdresselinje1(), opplysningAdresseAnnenpart.getAdresselinje1())
                    && Objects.equals(opplysningAdresseSøker.getPostnummer(), opplysningAdresseAnnenpart.getPostnummer())))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean likAdresseIgnoringCase(String adresse1, String adresse2) {
        if (adresse1 == null && adresse2 == null)
            return true;
        if (adresse1 == null || adresse2 == null)
            return false;
        return adresse1.equalsIgnoreCase(adresse2);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonopplysningerAggregat that = (PersonopplysningerAggregat) o;
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
