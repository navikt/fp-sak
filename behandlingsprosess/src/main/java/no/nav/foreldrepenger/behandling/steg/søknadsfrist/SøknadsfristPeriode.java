package no.nav.foreldrepenger.behandling.steg.søknadsfrist;

import java.time.Period;

/**
 * Maks antall måneder mellom søknadens mottattdato og første uttaksdag i søknaden.
 */
public interface SøknadsfristPeriode {

     Period getValue();
}
