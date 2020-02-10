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
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class PersonopplysningerAggregat {

    private final AktørId søkerAktørId;
    private final List<PersonopplysningEntitet> allePersonopplysninger;
    private final List<PersonRelasjonEntitet> alleRelasjoner;
    private final List<PersonAdresseEntitet> aktuelleAdresser;
    private final List<PersonstatusEntitet> aktuellePersonstatus;
    private final List<PersonstatusEntitet> overstyrtPersonstatus;
    private final List<PersonstatusEntitet> orginalPersonstatus;
    private final List<StatsborgerskapEntitet> aktuelleStatsborgerskap;
    private final OppgittAnnenPartEntitet oppgittAnnenPart;

    public PersonopplysningerAggregat(PersonopplysningGrunnlagEntitet grunnlag, AktørId aktørId, DatoIntervallEntitet forPeriode, Map<Landkoder, Region> landkoderRegionMap) {
        this.søkerAktørId = aktørId;
        this.oppgittAnnenPart = grunnlag.getOppgittAnnenPart().orElse(null);
        if (grunnlag.getRegisterVersjon().isPresent()) {
            this.alleRelasjoner = grunnlag.getRegisterVersjon().get().getRelasjoner();
            this.allePersonopplysninger = grunnlag.getRegisterVersjon().get().getPersonopplysninger();
            this.aktuelleAdresser = grunnlag.getRegisterVersjon().get().getAdresser()
                    .stream()
                    .filter(adr -> erIkkeSøker(aktørId, adr.getAktørId()) ||
                        erGyldigIPeriode(forPeriode, adr.getPeriode()))
                    .collect(Collectors.toList());
            overstyrtPersonstatus = grunnlag.getOverstyrtVersjon().map(PersonInformasjonEntitet::getPersonstatus)
                    .orElse(Collections.emptyList());
            final List<PersonstatusEntitet> registerPersonstatus = grunnlag.getRegisterVersjon().get().getPersonstatus()
                    .stream()
                    .filter(it -> finnesIkkeIOverstyrt(it, overstyrtPersonstatus))
                    .collect(Collectors.toList());
            this.orginalPersonstatus = grunnlag.getRegisterVersjon().get().getPersonstatus()
                    .stream()
                    .filter(it -> !finnesIkkeIOverstyrt(it, overstyrtPersonstatus))
                    .collect(Collectors.toList());
            this.aktuellePersonstatus = Stream.concat(
                registerPersonstatus.stream(),
                overstyrtPersonstatus.stream())
                    .filter(st -> erIkkeSøker(aktørId, st.getAktørId()) ||
                        erGyldigIPeriode(forPeriode, st.getPeriode()))
                    .collect(Collectors.toList());
            this.aktuelleStatsborgerskap = grunnlag.getRegisterVersjon().get().getStatsborgerskap()
                    .stream()
                    .filter(adr -> erIkkeSøker(aktørId, adr.getAktørId()) ||
                        erGyldigIPeriode(forPeriode, adr.getPeriode()))
                    .peek(sb -> sb.setRegion(landkoderRegionMap.get(sb.getStatsborgerskap())))
                    .collect(Collectors.toList());
        } else {
            this.alleRelasjoner = Collections.emptyList();
            this.allePersonopplysninger = Collections.emptyList();
            this.aktuelleAdresser = Collections.emptyList();
            this.aktuellePersonstatus = Collections.emptyList();
            this.aktuelleStatsborgerskap = Collections.emptyList();
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

    public List<PersonopplysningEntitet> getPersonopplysninger() {
        return Collections.unmodifiableList(allePersonopplysninger);
    }

    public PersonopplysningEntitet getPersonopplysning(AktørId aktørId) {
        return allePersonopplysninger.stream().filter(it -> it.getAktørId().equals(aktørId)).findFirst().orElse(null);
    }

    public List<PersonRelasjonEntitet> getRelasjoner() {
        return Collections.unmodifiableList(alleRelasjoner);
    }

    public List<PersonstatusEntitet> getPersonstatuserFor(AktørId aktørId) {
        return aktuellePersonstatus.stream()
                .filter(ss -> ss.getAktørId().equals(aktørId))
                .collect(Collectors.toList());
    }

    public PersonopplysningEntitet getSøker() {
        return allePersonopplysninger.stream()
                .filter(po -> po.getAktørId().equals(søkerAktørId))
                .findFirst()
                .orElse(null);
    }

    public List<StatsborgerskapEntitet> getStatsborgerskapFor(AktørId aktørId) {
        return aktuelleStatsborgerskap.stream()
                .filter(ss -> ss.getAktørId().equals(aktørId))
                .sorted(Comparator.comparing(this::rangerRegion))
                .collect(Collectors.toList());
    }

    // Det finnes ingen definert rangering for regioner. Men venter med å generalisere til det finnes use-caser som
    // krever en annen rangering enn nedenfor.
    private Integer rangerRegion(StatsborgerskapEntitet region) {
        if (region.getRegion().equals(Region.NORDEN)) {
            return 1;
        }
        if (region.getRegion().equals(Region.EOS)) {
            return 2;
        }
        return 3;
    }

    public PersonstatusEntitet getPersonstatusFor(AktørId aktørId) {
        return aktuellePersonstatus.stream()
                .filter(ss -> ss.getAktørId().equals(aktørId))
                .sorted(Comparator.comparing(PersonstatusEntitet::getPeriode).reversed())
                .findFirst().orElse(null);
    }

    /**
     * SKal kun benyttes av GUI til å vise verdien i aksjonspunktet {@value AksjonspunktDefinisjon#AVKLAR_FAKTA_FOR_PERSONSTATUS}
     *
     * @param søkerAktørId
     * @param aktørId
     * @return
     */
    public PersonstatusEntitet getOverstyrtPersonstatusFor(AktørId søkerAktørId) {
        return aktuellePersonstatus.stream()
                .filter(ss -> ss.getAktørId().equals(søkerAktørId))
                .sorted(Comparator.comparing(PersonstatusEntitet::getPeriode).reversed())
                .findFirst().orElse(null);
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
        return aktuelleAdresser.stream()
                .filter(ss -> ss.getAktørId().equals(aktørId))
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
        if (getOppgittAnnenPart().isPresent()) {
            return getPersonopplysninger().stream().filter(it -> it.getAktørId().equals(getOppgittAnnenPart().get().getAktørId())).findFirst();
        }
        return Optional.empty();
    }

    public Optional<PersonopplysningEntitet> getEktefelle() {
        List<PersonopplysningEntitet> personer = getTilPersonerFor(søkerAktørId, RelasjonsRolleType.EKTE);
        return personer.isEmpty() ? Optional.empty() : Optional.of(personer.get(0));
    }

    public List<PersonRelasjonEntitet> getSøkersRelasjoner() {
        return finnRelasjon(søkerAktørId);
    }

    public Map<AktørId, PersonopplysningEntitet> getAktørPersonopplysningMap() {
        return getPersonopplysninger().stream().collect(Collectors.toMap(PersonopplysningEntitet::getAktørId, Function.identity()));
    }

    public List<PersonopplysningEntitet> getTilPersonerFor(AktørId fraAktørId, RelasjonsRolleType relasjonsRolleType) {
        List<AktørId> tilAktører = alleRelasjoner.stream()
                .filter(e -> e.getRelasjonsrolle().equals(relasjonsRolleType) && e.getAktørId().equals(fraAktørId))
                .map(PersonRelasjonEntitet::getTilAktørId)
                .collect(Collectors.toList());

        List<PersonopplysningEntitet> tilPersoner = new ArrayList<>();
        tilAktører.forEach(e -> {
            allePersonopplysninger.stream()
            .filter(po -> po.getAktørId().equals(e))
            .forEach(p -> tilPersoner.add(p));
        });
        return Collections.unmodifiableList(tilPersoner);
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
        if (ektefelleRelasjon.isPresent() && ektefelleRelasjon.get().getHarSammeBosted() != null) {
            return ektefelleRelasjon.get().getHarSammeBosted();
        } else {
            return harSammeAdresseSom(aktørId);
        }
    }

    private boolean harSammeAdresseSom(AktørId aktørId) {
        if (getPersonopplysninger().stream().noneMatch(it -> it.getAktørId().equals(aktørId))) {
            return false;
        }

        for (PersonAdresseEntitet opplysningAdresseSøker : getAdresserFor(søkerAktørId)) {
            for (PersonAdresseEntitet opplysningAdresseAnnenpart : getAdresserFor(aktørId)) {
                if (Objects.equals(opplysningAdresseSøker.getAdresselinje1(), opplysningAdresseAnnenpart.getAdresselinje1())
                        && Objects.equals(opplysningAdresseSøker.getPostnummer(), opplysningAdresseAnnenpart.getPostnummer())) {
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
        PersonopplysningerAggregat that = (PersonopplysningerAggregat) o;
        return Objects.equals(søkerAktørId, that.søkerAktørId) &&
                Objects.equals(allePersonopplysninger, that.allePersonopplysninger) &&
                Objects.equals(alleRelasjoner, that.alleRelasjoner) &&
                Objects.equals(aktuelleAdresser, that.aktuelleAdresser) &&
                Objects.equals(aktuellePersonstatus, that.aktuellePersonstatus) &&
                Objects.equals(aktuelleStatsborgerskap, that.aktuelleStatsborgerskap) &&
                Objects.equals(oppgittAnnenPart, that.oppgittAnnenPart);
    }


    @Override
    public int hashCode() {
        return Objects.hash(søkerAktørId, allePersonopplysninger, alleRelasjoner, aktuelleAdresser, aktuellePersonstatus, aktuelleStatsborgerskap,
            oppgittAnnenPart);
    }


    @Override
    public String toString() {
        return "PersonopplysningerAggregat{" +
                "søkerAktørId=" + søkerAktørId +
                ", allePersonopplysninger=" + allePersonopplysninger +
                ", oppgittAnnenPart=" + oppgittAnnenPart +
                '}';
    }
}
