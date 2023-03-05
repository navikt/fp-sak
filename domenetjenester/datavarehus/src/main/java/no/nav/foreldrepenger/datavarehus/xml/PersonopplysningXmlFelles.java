package no.nav.foreldrepenger.datavarehus.xml;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppholdstillatelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
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
        var medlemskapsPeriode = personopplysningObjectFactory.createMedlemskapsperiode();

        var beslutningDato = VedtakXmlUtil.lagDateOpplysning(medlemskapPeriodeIn.getBeslutningsdato());
        beslutningDato.ifPresent(medlemskapsPeriode::setBeslutningsdato);

        Optional.ofNullable(medlemskapPeriodeIn.getDekningType()).map(VedtakXmlUtil::lagKodeverksOpplysning).ifPresent(medlemskapsPeriode::setDekningtype);
        medlemskapsPeriode.setErMedlem(VedtakXmlUtil.lagBooleanOpplysning(medlemskapPeriodeIn.getErMedlem()));
        medlemskapsPeriode.setLovvalgsland(VedtakXmlUtil.lagKodeverksOpplysning(medlemskapPeriodeIn.getLovvalgLand()));
        medlemskapsPeriode.setMedlemskaptype(VedtakXmlUtil.lagKodeverksOpplysning(medlemskapPeriodeIn.getMedlemskapType()));
        medlemskapsPeriode.setPeriode(VedtakXmlUtil.lagPeriodeOpplysning(medlemskapPeriodeIn.getFom(), medlemskapPeriodeIn.getTom()));
        return medlemskapsPeriode;
    }

    public String hentVergeNavn(AktørId aktørId) {
        return personinfoAdapter.hentBrukerArbeidsgiverForAktør(aktørId).map(PersoninfoArbeidsgiver::getNavn).orElse("Ukjent navn");
    }

    public PersonIdentifiserbar lagBruker(PersonopplysningerAggregat aggregat, PersonopplysningEntitet personopplysning) {
        var person = personopplysningObjectFactory.createPersonIdentifiserbar();

        populerPerson(aggregat, personopplysning, person);

        var navn = VedtakXmlUtil.lagStringOpplysning(personopplysning.getNavn());
        person.setNavn(navn);

        if (personopplysning.getAktørId() != null) {
            var norskIdent = personinfoAdapter.hentFnr(personopplysning.getAktørId());
            person.setNorskIdent(VedtakXmlUtil.lagStringOpplysning(norskIdent.map(PersonIdent::getIdent).orElse(null)));
        }

        var sivilstand = VedtakXmlUtil.lagKodeverksOpplysning(personopplysning.getSivilstand());
        person.setSivilstand(sivilstand);

        return person;
    }

    public PersonUidentifiserbar lagUidentifiserbarBruker(PersonopplysningerAggregat aggregat, PersonopplysningEntitet personopplysning) {
        var person = personopplysningObjectFactory.createPersonUidentifiserbar();

        populerPerson(aggregat, personopplysning, person);

        if (personopplysning.getAktørId() != null) {
            person.setAktoerId(VedtakXmlUtil.lagStringOpplysning(personopplysning.getAktørId().getId()));
        }

        var sivilstand = VedtakXmlUtil.lagKodeverksOpplysning(personopplysning.getSivilstand());
        person.setSivilstand(sivilstand);

        return person;
    }

    private void populerPerson(PersonopplysningerAggregat aggregat, PersonopplysningEntitet personopplysning, PersonUidentifiserbar person) {
        var dødsdato = VedtakXmlUtil.lagDateOpplysning(personopplysning.getDødsdato());
        dødsdato.ifPresent(person::setDoedsdato);

        var fødseldato = VedtakXmlUtil.lagDateOpplysning(personopplysning.getFødselsdato());
        fødseldato.ifPresent(person::setFoedselsdato);

        var kjønn = personopplysning.getKjønn();
        person.setKjoenn(VedtakXmlUtil.lagStringOpplysning(kjønn.getNavn()));

        var personstatus = Optional.ofNullable(aggregat.getPersonstatusFor(personopplysning.getAktørId()))
            .map(PersonstatusEntitet::getPersonstatus).orElse(PersonstatusType.UDEFINERT);
        person.setPersonstatus(VedtakXmlUtil.lagKodeverksOpplysning(personstatus));

        var statsborgerskap = aggregat.getRangertStatsborgerskapVedSkjæringstidspunktFor(personopplysning.getAktørId())
            .map(StatsborgerskapEntitet::getStatsborgerskap).orElse(Landkoder.UDEFINERT);

        person.setStatsborgerskap(VedtakXmlUtil.lagKodeverksOpplysning(statsborgerskap));

        person.setRegion(VedtakXmlUtil.lagStringOpplysning(aggregat.getStatsborgerskapRegionVedSkjæringstidspunkt(personopplysning.getAktørId()).getNavn()));

        aggregat.getOppholdstillatelseFor(personopplysning.getAktørId()).map(OppholdstillatelseEntitet::getTillatelse)
            .map(OppholdstillatelseType::getKode).map(VedtakXmlUtil::lagStringOpplysning).ifPresent(person::setOppholdstillatelse);
    }

}
