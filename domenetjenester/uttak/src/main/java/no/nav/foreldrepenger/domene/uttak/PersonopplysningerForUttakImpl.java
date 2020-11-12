package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;

@ApplicationScoped
public class PersonopplysningerForUttakImpl implements PersonopplysningerForUttak {

    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    public PersonopplysningerForUttakImpl(BasisPersonopplysningTjeneste personopplysningTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    PersonopplysningerForUttakImpl() {
        //CDI
    }

    @Override
    public Optional<LocalDate> søkersDødsdato(BehandlingReferanse ref) {
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        return Optional.ofNullable(personopplysningerAggregat.getSøker().getDødsdato());
    }

    @Override
    public Optional<LocalDate> søkersDødsdatoGjeldendePåDato(BehandlingReferanse ref, LocalDate dato) {
        var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(
            ref.getBehandlingId(), ref.getAktørId(), dato);
        return Optional.ofNullable(personopplysningerAggregat.getSøker().getDødsdato());
    }

    @Override
    public boolean harOppgittAnnenpart(BehandlingReferanse ref) {
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        return personopplysningerAggregat.getOppgittAnnenPart().isPresent();
    }

    @Override
    public boolean ektefelleHarSammeBosted(BehandlingReferanse ref) {
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        var søker = personopplysningerAggregat.getSøker();
        return harSivilstatusGift(søker) && harEktefelleSammeBosted(personopplysningerAggregat);
    }

    @Override
    public boolean annenpartHarSammeBosted(BehandlingReferanse ref) {
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        var annenPart = personopplysningerAggregat.getAnnenPart();
        // ANNEN PART HAR IKKE RELASJON
        return annenPart.filter(personopplysningEntitet -> personopplysningerAggregat.søkerHarSammeAdresseSom(
            personopplysningEntitet.getAktørId(), RelasjonsRolleType.UDEFINERT)).isPresent();
    }

    @Override
    public boolean barnHarSammeBosted(BehandlingReferanse ref) {
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        var barnRelasjoner = personopplysningerAggregat.getSøkersRelasjoner()
            .stream()
            .filter(familierelasjon -> familierelasjon.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .filter(rel -> Objects.nonNull(rel.getHarSammeBosted()))
            .filter(rel -> erIkkeDød(personopplysningerAggregat, rel))
            .collect(Collectors.toList());

        if (!barnRelasjoner.isEmpty()) {
            // Utleder fra adresse pga avvik i TPS sin FR . harSammeBosted ifm overgang fra DSF til FREG
            if (barnRelasjoner.stream().allMatch(PersonRelasjonEntitet::getHarSammeBosted)) {
                return true;
            }
            return harSammeAdresseSomBarn(personopplysningerAggregat);
        }
        return harSammeAdresseSomBarn(personopplysningerAggregat);
    }

    @Override
    public boolean oppgittAnnenpartUtenNorskID(BehandlingReferanse ref) {
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        var opt = personopplysningerAggregat.getOppgittAnnenPart();
        if (opt.isEmpty()) {
            return false;
        }
        var oppgittAnnenpart = opt.get();
        return oppgittAnnenpart.getAktørId() == null && oppgittAnnenpart.getUtenlandskPersonident() != null;
    }

    private boolean erIkkeDød(PersonopplysningerAggregat personopplysningerAggregat, PersonRelasjonEntitet rel) {
        return personopplysningerAggregat.getPersonopplysning(rel.getTilAktørId()).getDødsdato() == null;
    }

    private boolean harSammeAdresseSomBarn(PersonopplysningerAggregat personopplysningerAggregat) {
        for (PersonAdresseEntitet opplysningAdresseSøker : personopplysningerAggregat.getAdresserFor(
            personopplysningerAggregat.getSøker().getAktørId())) {
            for (PersonopplysningEntitet barn : personopplysningerAggregat.getBarna()) {
                if (harBarnetSammeAdresse(personopplysningerAggregat, opplysningAdresseSøker, barn)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean harBarnetSammeAdresse(PersonopplysningerAggregat personopplysningerAggregat,
                                          PersonAdresseEntitet opplysningAdresseSøker,
                                          PersonopplysningEntitet barn) {
        if (barn.getDødsdato() != null) {
            return true;
        }
        for (PersonAdresseEntitet opplysningAdresseBarn : personopplysningerAggregat.getAdresserFor(
            barn.getAktørId())) {
            var sammeperiode = opplysningAdresseSøker.getPeriode().overlapper(opplysningAdresseBarn.getPeriode());
            if (sammeperiode && likAdresseIgnoringCase(opplysningAdresseSøker.getAdresselinje1(),
                opplysningAdresseBarn.getAdresselinje1()) && Objects.equals(opplysningAdresseSøker.getPostnummer(),
                opplysningAdresseBarn.getPostnummer())) {
                return true;
            }
        }
        return false;
    }

    private boolean likAdresseIgnoringCase(String adresse1, String adresse2) {
        if (adresse1 == null && adresse2 == null) {
            return true;
        }
        if (adresse1 == null || adresse2 == null) {
            return false;
        }
        return adresse1.equalsIgnoreCase(adresse2);
    }

    private boolean harSivilstatusGift(PersonopplysningEntitet søker) {
        return søker.getSivilstand().equals(SivilstandType.GIFT);
    }

    private boolean harEktefelleSammeBosted(PersonopplysningerAggregat personopplysningerAggregat) {
        final Optional<PersonopplysningEntitet> ektefelle = personopplysningerAggregat.getEktefelle();
        return ektefelle.filter(personopplysningEntitet -> personopplysningerAggregat.søkerHarSammeAdresseSom(
            personopplysningEntitet.getAktørId(), RelasjonsRolleType.EKTE)).isPresent();
    }
}
