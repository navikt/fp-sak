package no.nav.foreldrepenger.datavarehus.xml;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.xml.vedtak.v2.Personopplysninger;
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

    public void setPersonopplysninger(Vedtak vedtak, BehandlingReferanse ref) {
        Object personopplysninger = null;
        var stp = ref.getSkjæringstidspunkt().getSkjæringstidspunktHvisUtledet().orElse(null);
        var personopplysningerAggregat = personopplysningTjeneste
                .hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(ref, stp);
        if (personopplysningerAggregat.isPresent()) {
            personopplysninger = lagPersonopplysning(personopplysningerAggregat.get(), ref.getBehandlingId(), ref.getAktørId(), ref.getSkjæringstidspunkt());//Implementeres i hver subklasse
        }
        var personopplysninger1 = new Personopplysninger();
        personopplysninger1.getAny().add(personopplysninger);
        vedtak.setPersonOpplysninger(personopplysninger1);
    }

}
