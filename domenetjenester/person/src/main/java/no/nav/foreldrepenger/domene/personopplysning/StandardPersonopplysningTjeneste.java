package no.nav.foreldrepenger.domene.personopplysning;

import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.domene.typer.AktørId;

public interface StandardPersonopplysningTjeneste {

    /**
     * Gir personopplysningene på utledet skjæringstidspunktet
     * @return personopplysninger
     */
    PersonopplysningerAggregat hentPersonopplysninger(BehandlingReferanse ref);

    /**
     * Gir personopplysningene på utledet skjæringstidspunktet
     * @return personopplysninger hvis finnes
     */
    Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(BehandlingReferanse ref);

    /**
     * Gir personopplysningene på utledet skjæringstidspunktet
     * @return personopplysninger hvis finnes
     */
    Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(Long behandlingId, AktørId aktørId);

}
