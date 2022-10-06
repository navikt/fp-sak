package no.nav.foreldrepenger.domene.arbeidsgiver;

import javax.enterprise.context.Dependent;

import no.nav.vedtak.felles.integrasjon.organisasjon.AbstractOrganisasjonKlient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

/*
 * Dokumentasjon https://confluence.adeo.no/display/FEL/EREG+-+Tjeneste+REST+ereg.api
 * Swagger https://modapp-q1.adeo.no/ereg/api/swagger-ui.html#/
 */

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.STS_CC, endpointProperty = "organisasjon.rs.url", endpointDefault = "https://modapp.adeo.no/ereg/api/v1/organisasjon")
public class OrganisasjonRestKlient extends AbstractOrganisasjonKlient {

    public OrganisasjonRestKlient() {
        super();
    }
}
