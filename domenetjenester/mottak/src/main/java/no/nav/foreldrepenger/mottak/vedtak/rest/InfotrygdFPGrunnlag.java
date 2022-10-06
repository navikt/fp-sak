package no.nav.foreldrepenger.mottak.vedtak.rest;

import javax.enterprise.context.ApplicationScoped;

import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.AbstractInfotrygdGrunnlag;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.STS_CC, endpointProperty = "fpsak.it.fp.grunnlag.url", endpointDefault = "http://infotrygd-foreldrepenger.default/grunnlag")
public class InfotrygdFPGrunnlag extends AbstractInfotrygdGrunnlag {

    public InfotrygdFPGrunnlag() {
        super();
    }
}
