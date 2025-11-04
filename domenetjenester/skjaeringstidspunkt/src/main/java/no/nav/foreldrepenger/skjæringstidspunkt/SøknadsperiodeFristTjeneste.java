package no.nav.foreldrepenger.skjæringstidspunkt;

import no.nav.foreldrepenger.behandling.Søknadsfristdatoer;

public interface SøknadsperiodeFristTjeneste {

    Søknadsfristdatoer finnSøknadsfrist(Long behandlingId);

}
