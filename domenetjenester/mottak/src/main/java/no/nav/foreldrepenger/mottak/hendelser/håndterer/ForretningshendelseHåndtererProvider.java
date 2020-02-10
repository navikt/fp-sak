package no.nav.foreldrepenger.mottak.hendelser.håndterer;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelseHåndterer;
import no.nav.foreldrepenger.mottak.hendelser.ForretningshendelsestypeRef;

@Dependent
public class ForretningshendelseHåndtererProvider {

    private Instance<ForretningshendelseHåndterer> håndterere;

    @Inject
    public ForretningshendelseHåndtererProvider(@Any Instance<ForretningshendelseHåndterer> håndterere) {
        this.håndterere = håndterere;
    }

    public ForretningshendelseHåndterer finnHåndterer(ForretningshendelseType forretningshendelseType, FagsakYtelseType ytelseType) {
        Instance<ForretningshendelseHåndterer> selected = håndterere.select(new ForretningshendelsestypeRef.ForretningshendelsestypeRefLiteral(forretningshendelseType));
        if (selected.isAmbiguous()) {
            selected = selected.select(new FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral(ytelseType.getKode()));
        }
        if (selected.isAmbiguous()) {
            throw new IllegalArgumentException("Mer enn en implementasjon funnet for forretningshendelsetype:" + forretningshendelseType);
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for forretningshendelsetype:" + forretningshendelseType);
        }
        ForretningshendelseHåndterer minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }
}
