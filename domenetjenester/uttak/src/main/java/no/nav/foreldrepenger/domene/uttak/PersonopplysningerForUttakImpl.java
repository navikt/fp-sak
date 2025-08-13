package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@ApplicationScoped
public class PersonopplysningerForUttakImpl implements PersonopplysningerForUttak {

    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    public PersonopplysningerForUttakImpl(PersonopplysningTjeneste personopplysningTjeneste) {
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
    public boolean harOppgittAnnenpartMedNorskID(BehandlingReferanse ref) {
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        return personopplysningerAggregat.getOppgittAnnenPart().map(OppgittAnnenPartEntitet::getAktørId).isPresent();
    }

    @Override
    public boolean ektefelleHarSammeBosted(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt) {
        var forPeriode = skjæringstidspunkt.getUttaksintervall()
            .map(intervall -> DatoIntervallEntitet.fraOgMedTilOgMed(intervall.getFomDato(), intervall.getTomDato()))
            .orElseGet(() -> DatoIntervallEntitet.enDag(skjæringstidspunkt.getUtledetSkjæringstidspunkt()));
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);


        var søker = personopplysningerAggregat.getSøker();
        return harSivilstatusGift(søker) && harEktefelleSammeBosted(personopplysningerAggregat, forPeriode);
    }

    @Override
    public boolean annenpartHarSammeBosted(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt) {
        var forPeriode = skjæringstidspunkt.getUttaksintervall()
            .map(intervall -> DatoIntervallEntitet.fraOgMedTilOgMed(intervall.getFomDato(), intervall.getTomDato()))
            .orElseGet(() -> DatoIntervallEntitet.enDag(skjæringstidspunkt.getUtledetSkjæringstidspunkt()));
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref).orElseThrow();

        var annenPart = personopplysningerAggregat.getAnnenPart().map(PersonopplysningEntitet::getAktørId);
        // ANNEN PART HAR IKKE RELASJON
        return annenPart.filter(apAktør -> personopplysningerAggregat.søkerHarSammeAdresseSom(apAktør, RelasjonsRolleType.UDEFINERT, forPeriode)).isPresent();
    }

    @Override
    public boolean barnHarSammeBosted(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt) {
        var forPeriode = Optional.ofNullable(skjæringstidspunkt)
            .flatMap(Skjæringstidspunkt::getUttaksintervall)
            .map(intervall -> DatoIntervallEntitet.fraOgMedTilOgMed(intervall.getFomDato(), intervall.getTomDato()))
            .orElseGet(() -> DatoIntervallEntitet.enDag(skjæringstidspunkt.getUtledetSkjæringstidspunkt()));
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref).orElseThrow();
        var barnRelasjoner = personopplysningerAggregat.getSøkersRelasjoner()
            .stream()
            .filter(familierelasjon -> familierelasjon.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .filter(rel -> Objects.nonNull(rel.getHarSammeBosted()))
            .filter(rel -> erIkkeDød(personopplysningerAggregat, rel))
            .toList();

        if (!barnRelasjoner.isEmpty()) {
            // TODO TFP-5036 allMatch er litt skummel - bør sjekke kun for perioder som er aktuelle i behandlingen
            // Utleder fra adresse pga avvik i PDL sin FR . harSammeBosted ifm overgang fra DSF til FREG
            if (barnRelasjoner.stream().allMatch(PersonRelasjonEntitet::getHarSammeBosted)) {
                return true;
            }
            return harSammeAdresseSomBarn(personopplysningerAggregat, forPeriode);
        }
        return harSammeAdresseSomBarn(personopplysningerAggregat, forPeriode);
    }

    private boolean erIkkeDød(PersonopplysningerAggregat personopplysningerAggregat, PersonRelasjonEntitet rel) {
        return personopplysningerAggregat.getPersonopplysning(rel.getTilAktørId()).getDødsdato() == null;
    }

    private boolean harSammeAdresseSomBarn(PersonopplysningerAggregat personopplysningerAggregat, AbstractLocalDateInterval forPeriode) {
        return personopplysningerAggregat.getAdresserFor(personopplysningerAggregat.getSøker().getAktørId(), forPeriode)
            .stream()
            .anyMatch(opplysningAdresseSøker -> personopplysningerAggregat.getBarna()
                .stream()
                .anyMatch(barn -> harBarnetSammeAdresse(personopplysningerAggregat, opplysningAdresseSøker, barn, forPeriode)));
    }

    private boolean harBarnetSammeAdresse(PersonopplysningerAggregat personopplysningerAggregat,
                                          PersonAdresseEntitet opplysningAdresseSøker,
                                          PersonopplysningEntitet barn, AbstractLocalDateInterval forPeriode) {
        if (barn.getDødsdato() != null) {
            return true;
        }
        for (var opplysningAdresseBarn : personopplysningerAggregat.getAdresserFor(barn.getAktørId(), forPeriode)) {
            var sammeperiode = opplysningAdresseSøker.getPeriode().overlapper(opplysningAdresseBarn.getPeriode());
            if (sammeperiode && PersonAdresseEntitet.likeAdresser(opplysningAdresseSøker, opplysningAdresseBarn)) {
                return true;
            }
        }
        return false;
    }

    private boolean harSivilstatusGift(PersonopplysningEntitet søker) {
        return søker.getSivilstand().equals(SivilstandType.GIFT);
    }

    private boolean harEktefelleSammeBosted(PersonopplysningerAggregat personopplysningerAggregat, AbstractLocalDateInterval forPeriode) {
        var ektefelle = personopplysningerAggregat.getEktefelle().map(PersonopplysningEntitet::getAktørId);
        return ektefelle.filter(ektefelleAktør -> personopplysningerAggregat.søkerHarSammeAdresseSom(ektefelleAktør, RelasjonsRolleType.EKTE, forPeriode)).isPresent();
    }
}
