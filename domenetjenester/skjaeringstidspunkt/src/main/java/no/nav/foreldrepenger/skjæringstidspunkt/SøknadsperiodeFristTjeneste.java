package no.nav.foreldrepenger.skjæringstidspunkt;

import no.nav.foreldrepenger.behandling.Søknadsfristdatoer;

import java.util.Optional;

public interface SøknadsperiodeFristTjeneste {

    Optional<Søknadsfristdatoer> finnSøknadsfrist(Long behandlingId);

}
