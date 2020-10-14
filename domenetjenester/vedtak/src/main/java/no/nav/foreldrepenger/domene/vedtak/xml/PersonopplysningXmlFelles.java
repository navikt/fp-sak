package no.nav.foreldrepenger.domene.vedtak.xml;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.felles.xml.felles.v2.DateOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.KodeverksOpplysning;
import no.nav.vedtak.felles.xml.felles.v2.StringOpplysning;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.Medlemskapsperiode;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.PersonIdentifiserbar;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.v2.PersonUidentifiserbar;

@ApplicationScoped
public class PersonopplysningXmlFelles {

    private final ObjectFactory personopplysningObjectFactory = new ObjectFactory();
    private PersoninfoAdapter personinfoAdapter;


    PersonopplysningXmlFelles() {
        // For CDI
    }

    @Inject
    public PersonopplysningXmlFelles(PersoninfoAdapter personinfoAdapter) {
        this.personinfoAdapter = personinfoAdapter;
    }

    public Medlemskapsperiode lagMedlemskapPeriode(MedlemskapPerioderEntitet medlemskapPeriodeIn) {
        Medlemskapsperiode medlemskapsPeriode = personopplysningObjectFactory.createMedlemskapsperiode();

        Optional<DateOpplysning> beslutningDato = VedtakXmlUtil.lagDateOpplysning(medlemskapPeriodeIn.getBeslutningsdato());
        beslutningDato.ifPresent(medlemskapsPeriode::setBeslutningsdato);

        medlemskapsPeriode.setDekningtype(VedtakXmlUtil.lagKodeverksOpplysning(medlemskapPeriodeIn.getDekningType()));
        medlemskapsPeriode.setErMedlem(VedtakXmlUtil.lagBooleanOpplysning(medlemskapPeriodeIn.getErMedlem()));
        medlemskapsPeriode.setLovvalgsland(VedtakXmlUtil.lagKodeverksOpplysning(medlemskapPeriodeIn.getLovvalgLand()));
        medlemskapsPeriode.setMedlemskaptype(VedtakXmlUtil.lagKodeverksOpplysning(medlemskapPeriodeIn.getMedlemskapType()));
        medlemskapsPeriode.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(medlemskapPeriodeIn.getFom(), medlemskapPeriodeIn.getTom()));
        return medlemskapsPeriode;
    }

    public String hentVergeNavn(AktørId aktørId) {
        return personinfoAdapter.hentBrukerArbeidsgiverForAktør(aktørId).map(PersoninfoArbeidsgiver::getNavn).orElse("Ukjent navn"); //$NON-NLS-1$
    }

    public PersonIdentifiserbar lagBruker(PersonopplysningerAggregat aggregat, PersonopplysningEntitet personopplysning) {
        PersonIdentifiserbar person = personopplysningObjectFactory.createPersonIdentifiserbar();

        populerPerson(aggregat, personopplysning, person);

        StringOpplysning navn = VedtakXmlUtil.lagStringOpplysning(personopplysning.getNavn());
        person.setNavn(navn);

        if (personopplysning.getAktørId() != null) {
            Optional<PersonIdent> norskIdent = personinfoAdapter.hentFnr(personopplysning.getAktørId());
            person.setNorskIdent(VedtakXmlUtil.lagStringOpplysning(norskIdent.map(PersonIdent::getIdent).orElse(null)));
        }

        if (personopplysning.getRegion() != null) {
            person.setRegion(VedtakXmlUtil.lagStringOpplysning(personopplysning.getRegion().getNavn()));
        }

        KodeverksOpplysning sivilstand = VedtakXmlUtil.lagKodeverksOpplysning(personopplysning.getSivilstand());
        person.setSivilstand(sivilstand);

        return person;
    }

    public PersonUidentifiserbar lagUidentifiserbarBruker(PersonopplysningerAggregat aggregat, PersonopplysningEntitet personopplysning) {
        PersonUidentifiserbar person = personopplysningObjectFactory.createPersonUidentifiserbar();

        populerPerson(aggregat, personopplysning, person);

        if (personopplysning.getRegion() != null) {
            person.setRegion(VedtakXmlUtil.lagStringOpplysning(personopplysning.getRegion().getNavn()));
        }

        if (personopplysning.getAktørId() != null) {
            person.setAktoerId(VedtakXmlUtil.lagStringOpplysning(personopplysning.getAktørId().getId()));
        }

        KodeverksOpplysning sivilstand = VedtakXmlUtil.lagKodeverksOpplysning(personopplysning.getSivilstand());
        person.setSivilstand(sivilstand);

        return person;
    }

    private void populerPerson(PersonopplysningerAggregat aggregat, PersonopplysningEntitet personopplysning, PersonUidentifiserbar person) {
        Optional<DateOpplysning> dødsdato = VedtakXmlUtil.lagDateOpplysning(personopplysning.getDødsdato());
        dødsdato.ifPresent(person::setDoedsdato);

        Optional<DateOpplysning> fødseldato = VedtakXmlUtil.lagDateOpplysning(personopplysning.getFødselsdato());
        fødseldato.ifPresent(person::setFoedselsdato);

        NavBrukerKjønn kjønn = personopplysning.getKjønn();
        person.setKjoenn(VedtakXmlUtil.lagStringOpplysning(kjønn.getNavn()));

        PersonstatusType personstatus = Optional.ofNullable(aggregat.getPersonstatusFor(personopplysning.getAktørId()))
            .map(PersonstatusEntitet::getPersonstatus).orElse(PersonstatusType.UDEFINERT);
        person.setPersonstatus(VedtakXmlUtil.lagKodeverksOpplysning(personstatus));

        Landkoder statsborgerskap = aggregat.getStatsborgerskapFor(personopplysning.getAktørId()).stream().findFirst()
            .map(StatsborgerskapEntitet::getStatsborgerskap).orElse(Landkoder.UDEFINERT);
        person.setStatsborgerskap(VedtakXmlUtil.lagKodeverksOpplysning(statsborgerskap));
    }

}
