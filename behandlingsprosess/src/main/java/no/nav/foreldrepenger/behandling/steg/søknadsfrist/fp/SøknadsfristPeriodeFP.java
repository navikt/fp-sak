package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.søknadsfrist.SøknadsfristPeriode;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class SøknadsfristPeriodeFP implements SøknadsfristPeriode {

    private final Period value;

    @Inject
    public SøknadsfristPeriodeFP(@KonfigVerdi(value = "fp.søknadfrist.etter.første.uttaksdag", defaultVerdi = "P3M") Period value) {
        this.value = value;
    }

    public Period getValue() {
        return value;
    }
}
