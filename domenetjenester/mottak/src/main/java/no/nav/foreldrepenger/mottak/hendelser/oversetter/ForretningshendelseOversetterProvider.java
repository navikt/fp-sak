package no.nav.foreldrepenger.mottak.hendelser.oversetter;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseOversetter;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;

@Dependent
public class ForretningshendelseOversetterProvider {

    private Instance<ForretningshendelseOversetter<? extends Forretningshendelse>> oversettere;

    @Inject
    public ForretningshendelseOversetterProvider(@Any Instance<ForretningshendelseOversetter<? extends Forretningshendelse>> oversettere) {
        this.oversettere = oversettere;
    }

    @SuppressWarnings("unchecked")
    public <T extends Forretningshendelse> ForretningshendelseOversetter<T> finnOversetter(ForretningshendelseType forretningshendelseType) {
        Instance<ForretningshendelseOversetter<? extends Forretningshendelse>> selected = oversettere.select(new ForretningshendelsestypeRef.ForretningshendelsestypeRefLiteral(forretningshendelseType));
        if (selected.isAmbiguous()) {
            throw new IllegalArgumentException("Mer enn en implementasjon funnet for forretningshendelsetype:" + forretningshendelseType);
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for forretningshendelsetype:" + forretningshendelseType);
        }
        ForretningshendelseOversetter<? extends Forretningshendelse> minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return (ForretningshendelseOversetter<T>) minInstans;
    }
}
