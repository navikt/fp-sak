package no.nav.foreldrepenger.behandling.aksjonspunkt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.CDI;

import no.nav.vedtak.exception.TekniskException;

public class AksjonspunktUtlederHolder {

    private final List<AksjonspunktUtleder> utledere;

    public AksjonspunktUtlederHolder() {
        this.utledere = new ArrayList<>();
    }

    public AksjonspunktUtlederHolder leggTil(Class<? extends AksjonspunktUtleder> utleder) {
        utledere.add(hentUtleder(utleder));
        return this;
    }

    private AksjonspunktUtleder hentUtleder(Class<? extends AksjonspunktUtleder> aksjonspunktUtlederClass) {
        var instance = CDI.current().select(aksjonspunktUtlederClass);

        if (instance.isAmbiguous()) {
            throw new TekniskException("FP-191205", "Mer enn en implementasjon funnet for aksjonspunktutleder "
                + aksjonspunktUtlederClass.getSimpleName());
        }
        if (instance.isUnsatisfied()) {
            throw new TekniskException("FP-985832", "Ukjent aksjonspunktutleder " + aksjonspunktUtlederClass.getSimpleName());
        }
        var minInstans = instance.get();

        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException(
                    "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }

    public List<AksjonspunktUtleder> getUtledere() {
        return Collections.unmodifiableList(utledere);
    }
}
