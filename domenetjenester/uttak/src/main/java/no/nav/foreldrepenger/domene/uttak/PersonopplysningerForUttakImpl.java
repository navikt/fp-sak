package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
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
    public Optional<LocalDate> søkersDødsdatoGjeldendePåDato(BehandlingReferanse ref, LocalDate dato) {
        var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunkt(ref, dato);
        return Optional.ofNullable(personopplysningerAggregat.getSøker().getDødsdato());
    }

    @Override
    public boolean harOppgittAnnenpartMedNorskID(BehandlingReferanse ref) {
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysninger(ref);
        return personopplysningerAggregat.getOppgittAnnenPart().map(OppgittAnnenPartEntitet::getAktørId).isPresent();
    }

    @Override
    public boolean ektefelleHarSammeBosted(BehandlingReferanse ref) {
        var personopplysningerAggregat = Optional.ofNullable(ref.getSkjæringstidspunkt().getUtledetMedlemsintervall())
            .map(intervall -> DatoIntervallEntitet.fraOgMedTilOgMed(intervall.getFomDato(), intervall.getTomDato()))
            .flatMap(intervall -> personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(ref, intervall))
            .orElseGet(() -> personopplysningTjeneste.hentPersonopplysninger(ref));

        var søker = personopplysningerAggregat.getSøker();
        return harSivilstatusGift(søker) && harEktefelleSammeBosted(personopplysningerAggregat);
    }

    @Override
    public boolean annenpartHarSammeBosted(BehandlingReferanse ref) {
        var personopplysningerAggregat = Optional.ofNullable(ref.getSkjæringstidspunkt().getUtledetMedlemsintervall())
            .map(intervall -> DatoIntervallEntitet.fraOgMedTilOgMed(intervall.getFomDato(), intervall.getTomDato()))
            .flatMap(intervall -> personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(ref, intervall))
            .orElseGet(() -> personopplysningTjeneste.hentPersonopplysninger(ref));

        var annenPart = personopplysningerAggregat.getAnnenPart().map(PersonopplysningEntitet::getAktørId);
        // ANNEN PART HAR IKKE RELASJON
        return annenPart.filter(apAktør -> personopplysningerAggregat.søkerHarSammeAdresseSom(apAktør, RelasjonsRolleType.UDEFINERT)).isPresent();
    }

    @Override
    public boolean barnHarSammeBosted(BehandlingReferanse ref) {
        var personopplysningerAggregat = Optional.ofNullable(ref.getSkjæringstidspunkt().getUtledetMedlemsintervall())
            .map(intervall -> DatoIntervallEntitet.fraOgMedTilOgMed(intervall.getFomDato(), intervall.getTomDato()))
            .flatMap(intervall -> personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(ref, intervall))
            .orElseGet(() -> personopplysningTjeneste.hentPersonopplysninger(ref));
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
        return personopplysningerAggregat.getAdresserFor(personopplysningerAggregat.getSøker().getAktørId())
            .stream()
            .anyMatch(opplysningAdresseSøker -> personopplysningerAggregat.getBarna()
                .stream()
                .anyMatch(barn -> harBarnetSammeAdresse(personopplysningerAggregat, opplysningAdresseSøker, barn)));
    }

    private boolean harBarnetSammeAdresse(PersonopplysningerAggregat personopplysningerAggregat,
                                          PersonAdresseEntitet opplysningAdresseSøker,
                                          PersonopplysningEntitet barn) {
        if (barn.getDødsdato() != null) {
            return true;
        }
        for (var opplysningAdresseBarn : personopplysningerAggregat.getAdresserFor(barn.getAktørId())) {
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

    private boolean harEktefelleSammeBosted(PersonopplysningerAggregat personopplysningerAggregat) {
        var ektefelle = personopplysningerAggregat.getEktefelle().map(PersonopplysningEntitet::getAktørId);
        return ektefelle.filter(ektefelleAktør -> personopplysningerAggregat.søkerHarSammeAdresseSom(ektefelleAktør, RelasjonsRolleType.EKTE)).isPresent();
    }
}
