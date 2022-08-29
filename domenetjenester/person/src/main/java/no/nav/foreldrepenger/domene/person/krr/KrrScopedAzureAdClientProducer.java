package no.nav.foreldrepenger.domene.person.krr;


import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Qualifier;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.felles.integrasjon.rest.AzureADRestClient;


public class KrrScopedAzureAdClientProducer {

    private static final String DEFAULT_KRR_AZURE_SCOPE = "api://prod-gcp.team-rocket.digdir-krr-proxy/.default";

    @Produces
    @KrrScoped
    @Dependent
    public AzureADRestClient provider(@KonfigVerdi(value = "krr.rs.scopes", defaultVerdi = DEFAULT_KRR_AZURE_SCOPE) String scope) {
        return AzureADRestClient.builder().scope(scope).build();
    }

    @Qualifier
    @Retention(RUNTIME)
    @Target({METHOD, FIELD, PARAMETER, TYPE})
    public @interface KrrScoped {}

}
