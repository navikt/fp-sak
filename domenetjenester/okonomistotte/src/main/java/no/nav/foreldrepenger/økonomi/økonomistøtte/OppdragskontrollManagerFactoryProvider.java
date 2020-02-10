package no.nav.foreldrepenger.økonomi.økonomistøtte;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class OppdragskontrollManagerFactoryProvider {

    private Instance<OppdragskontrollManagerFactory> oppdragskontrollManagerFactoryInstance;

    OppdragskontrollManagerFactoryProvider() {
        // CDI
    }

    @Inject
    public OppdragskontrollManagerFactoryProvider(@Any Instance<OppdragskontrollManagerFactory> oppdragskontrollManagerFactories) {
        this.oppdragskontrollManagerFactoryInstance = oppdragskontrollManagerFactories;
    }

    public OppdragskontrollManagerFactory getTjeneste(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(oppdragskontrollManagerFactoryInstance, fagsakYtelseType).orElseThrow();
    }
}
