package no.nav.foreldrepenger.behandling.steg.søknadsfrist.svp;

import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.søknadsfrist.SøknadsfristPeriode;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.vedtak.konfig.KonfigVerdi;


@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class SøknadsfristPeriodeSVP implements SøknadsfristPeriode {

    private final Period value;

    @Inject
    public SøknadsfristPeriodeSVP(@KonfigVerdi(value = "svp.søknadfrist.etter.første.uttaksdag", defaultVerdi = "P3M") Period value) {
        this.value = value;
    }

    public Period getValue() {
        return value;
    }
}
