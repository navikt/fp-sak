package no.nav.foreldrepenger.web.server.abac;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.interceptor.Interceptor;

import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.log.sporingslogg.Sporingsdata;
import no.nav.vedtak.sikkerhet.abac.DefaultAbacSporingslogg;
import no.nav.vedtak.sikkerhet.abac.PdpRequest;

/** Egen sporingslogg implementasjon for å utvide med egne felter. */
@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 1)
public class AppAbacSporingslogg extends DefaultAbacSporingslogg {

    /** Eks. antall akjonspunkter, mottate dokumenter, el. som behandles i denne requesten. */
    @Override
    protected int getAntallResources(PdpRequest pdpRequest) {
        // en request kan i prinsippet inneholde mer enn ett aksjonspunkt (selv om uvanlig).
        return Math.max(1, pdpRequest.getAntall(AbacAttributter.RESOURCE_FORELDREPENGER_SAK_AKSJONSPUNKT_TYPE));
    }

    @Override
    protected void setCustomSporingsdata(PdpRequest pdpRequest, int index, Sporingsdata sporingsdata) {

        var antallIdenter = Math.max(1, antallIdenter(pdpRequest));
        setOptionalListValueinAttributeSet(sporingsdata, pdpRequest,
            AbacAttributter.RESOURCE_FORELDREPENGER_SAK_AKSJONSPUNKT_TYPE,
            (index / antallIdenter),
            AppAbacAttributtType.ABAC_AKSJONSPUNKT_TYPE);

        setOptionalValueinAttributeSet(sporingsdata, pdpRequest,
            AbacAttributter.RESOURCE_FORELDREPENGER_SAK_ANSVARLIG_SAKSBEHANDLER,
            AppAbacAttributtType.ABAC_ANSVALIG_SAKSBEHANDLER);

        setOptionalValueinAttributeSet(sporingsdata, pdpRequest,
            AbacAttributter.RESOURCE_FORELDREPENGER_SAK_BEHANDLINGSSTATUS,
            AppAbacAttributtType.ABAC_BEHANDLING_STATUS);

        setOptionalValueinAttributeSet(sporingsdata, pdpRequest,
            AbacAttributter.RESOURCE_FORELDREPENGER_SAK_SAKSSTATUS,
            AppAbacAttributtType.ABAC_SAK_STATUS);
    }

}
