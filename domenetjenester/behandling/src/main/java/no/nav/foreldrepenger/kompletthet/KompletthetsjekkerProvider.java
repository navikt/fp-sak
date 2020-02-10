package no.nav.foreldrepenger.kompletthet;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class KompletthetsjekkerProvider {

    public Kompletthetsjekker finnKompletthetsjekkerFor(FagsakYtelseType ytelseType, BehandlingType behandlingType) {

        Instance<Kompletthetsjekker> instance = CDI.current()
            .select(Kompletthetsjekker.class, new FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral(ytelseType.getKode()));

        if (instance.isAmbiguous()) {
            instance = instance.select(new BehandlingTypeRef.BehandlingTypeRefLiteral(behandlingType.getKode()));
        }

        if (instance.isAmbiguous()) {
            throw KompletthetFeil.FACTORY.flereImplementasjonerAvKompletthetsjekker(ytelseType.getKode(), behandlingType.getKode()).toException();
        } else if (instance.isUnsatisfied()) {
            throw KompletthetFeil.FACTORY.ingenImplementasjonerAvKompletthetssjekker(ytelseType.getKode(), behandlingType.getKode()).toException();
        }
        Kompletthetsjekker minInstans = instance.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return instance.get();
    }
}
