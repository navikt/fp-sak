package no.nav.foreldrepenger.domene.vedtak.xml;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;

public abstract class PersonopplysningXmlTjeneste {

    private PersonopplysningTjeneste personopplysningTjeneste;


    public PersonopplysningXmlTjeneste() {
        // For CDI
    }

    public PersonopplysningXmlTjeneste(PersonopplysningTjeneste personopplysningTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public abstract Object lagPersonopplysning(PersonopplysningerAggregat personopplysningerAggregat, Long behandlingId, AktørId aktørId, Skjæringstidspunkt skjæringstidspunkter);

    public void setPersonopplysninger(Vedtak vedtak, Long behandlingId, AktørId aktørId, Skjæringstidspunkt skjæringstidspunkter) {
        Object personopplysninger = null;
        var stp = skjæringstidspunkter.getSkjæringstidspunktHvisUtledet().orElse(null);
        var personopplysningerAggregat = personopplysningTjeneste
                .hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(behandlingId, aktørId, stp);
        if (personopplysningerAggregat.isPresent()) {
            personopplysninger = lagPersonopplysning(personopplysningerAggregat.get(), behandlingId, aktørId, skjæringstidspunkter);//Implementeres i hver subklasse
        }
        var personopplysninger1 = new no.nav.vedtak.felles.xml.vedtak.v2.Personopplysninger();
        personopplysninger1.getAny().add(personopplysninger);
        vedtak.setPersonOpplysninger(personopplysninger1);
    }

}
