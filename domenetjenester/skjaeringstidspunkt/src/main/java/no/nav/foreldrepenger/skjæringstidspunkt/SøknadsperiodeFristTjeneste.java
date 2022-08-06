package no.nav.foreldrepenger.skjæringstidspunkt;

import java.util.Optional;

import no.nav.foreldrepenger.behandling.Søknadsfristdatoer;

public interface SøknadsperiodeFristTjeneste {

    Optional<Søknadsfristdatoer> finnSøknadsfrist(Long behandlingId);

}
