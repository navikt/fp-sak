package no.nav.foreldrepenger.web.server.abac;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.interceptor.Interceptor;

import no.nav.foreldrepenger.sikkerhet.abac.LegacyTokenProvider;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.sikkerhet.abac.AbacAuditlogger;
import no.nav.vedtak.sikkerhet.abac.PdpKlient;
import no.nav.vedtak.sikkerhet.abac.PdpRequest;
import no.nav.vedtak.sikkerhet.abac.PdpRequestBuilder;

@Default
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 1)
public class AppPepImpl extends no.nav.vedtak.sikkerhet.abac.PepImpl {

    AppPepImpl() {
    }

    @Inject
    public AppPepImpl(PdpKlient pdpKlient,
                   PdpRequestBuilder pdpRequestBuilder,
                   AbacAuditlogger sporingslogg,
                   @KonfigVerdi(value = "pip.users", required = false) String pipUsers) {
        super(pdpKlient, new LegacyTokenProvider() ,pdpRequestBuilder, sporingslogg, pipUsers);
    }

    /** Ta hensyn til at flere aksjonspunker kan vurderes per request. */
    @Override
    protected int getAntallResources(PdpRequest pdpRequest) {
        return pdpRequest.getAntall(AbacAttributter.RESOURCE_FORELDREPENGER_SAK_AKSJONSPUNKT_TYPE);
    }
}
