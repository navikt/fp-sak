package no.nav.foreldrepenger.mottak.hendelser.saksvelger;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseSaksvelger;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;

@Dependent
public class ForretningshendelseSaksvelgerProvider {

    private Instance<ForretningshendelseSaksvelger<? extends Forretningshendelse>> saksvelgere;

    @Inject
    public ForretningshendelseSaksvelgerProvider(@Any Instance<ForretningshendelseSaksvelger<? extends Forretningshendelse>> saksvelgere) {
        this.saksvelgere = saksvelgere;
    }

    @SuppressWarnings("unchecked")
    public <T extends Forretningshendelse> ForretningshendelseSaksvelger<T> finnSaksvelger(ForretningshendelseType forretningshendelseType) {
        var selected = saksvelgere.select(new ForretningshendelsestypeRef.ForretningshendelsestypeRefLiteral(forretningshendelseType));
        if (selected.isAmbiguous()) {
            throw new IllegalArgumentException("Mer enn en implementasjon funnet for forretningshendelsetype:" + forretningshendelseType);
        }
        if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for forretningshendelsetype:" + forretningshendelseType);
        }
        var minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return (ForretningshendelseSaksvelger<T>) minInstans;
    }
}
