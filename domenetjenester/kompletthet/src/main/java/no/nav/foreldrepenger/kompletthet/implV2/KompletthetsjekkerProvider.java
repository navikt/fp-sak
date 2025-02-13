package no.nav.foreldrepenger.kompletthet.implV2;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.CDI;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kompletthet.impl.KompletthetsjekkerOld;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class KompletthetsjekkerProvider {

    public KompletthetsjekkerOld finnKompletthetsjekkerFor(FagsakYtelseType ytelseType, BehandlingType behandlingType) {

        var instance = CDI.current()
                .select(KompletthetsjekkerOld.class, new FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral(ytelseType));

        if (instance.isAmbiguous()) {
            instance = instance.select(new BehandlingTypeRef.BehandlingTypeRefLiteral(behandlingType));
        }

        if (instance.isAmbiguous()) {
            var msg = String.format("Mer enn en implementasjon funnet av Kompletthetsjekker for "
                + "fagsakYtelseType=%s og behandlingType=%s", ytelseType.getKode(), behandlingType.getKode());
            throw new TekniskException("FP-912911", msg);
        }
        if (instance.isUnsatisfied()) {
            var msg = String.format("Fant ingen implementasjon av Kompletthetsjekker for fagsakYtelseType=%s "
                    + "og behandlingType=%s", ytelseType.getKode(), behandlingType.getKode());
            throw new TekniskException("FP-912910", msg);
        }
        var minInstans = instance.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException(
                    "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return instance.get();
    }
}
