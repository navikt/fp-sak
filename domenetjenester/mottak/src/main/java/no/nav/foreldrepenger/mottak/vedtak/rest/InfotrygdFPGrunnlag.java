package no.nav.foreldrepenger.mottak.vedtak.rest;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.AbstractInfotrygdGrunnlag;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

// Brukes ikke av mottak, men av egen innsynsløsning / ui.
@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "fpsak.it.fp.grunnlag.url", endpointDefault = "http://fp-infotrygd-foreldrepenger/grunnlag", scopesProperty = "fpsak.it.fp.scopes", scopesDefault = "api://prod-fss.teamforeldrepenger.fp-infotrygd-foreldrepenger/.default")
public class InfotrygdFPGrunnlag extends AbstractInfotrygdGrunnlag {

    public InfotrygdFPGrunnlag() {
        super();
    }
}
