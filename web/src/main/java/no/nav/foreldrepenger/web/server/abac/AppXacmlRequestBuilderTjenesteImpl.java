package no.nav.foreldrepenger.web.server.abac;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.interceptor.Interceptor;

import no.nav.vedtak.sikkerhet.abac.NavAbacCommonAttributter;
import no.nav.vedtak.sikkerhet.abac.PdpRequest;
import no.nav.vedtak.sikkerhet.pdp.XacmlRequestBuilderTjeneste;
import no.nav.vedtak.sikkerhet.pdp.xacml.XacmlAttributeSet;
import no.nav.vedtak.sikkerhet.pdp.xacml.XacmlRequestBuilder;
import no.nav.vedtak.util.Tuple;

@Dependent
@Alternative
@Priority(Interceptor.Priority.APPLICATION + 2)
public class AppXacmlRequestBuilderTjenesteImpl implements XacmlRequestBuilderTjeneste {

    public AppXacmlRequestBuilderTjenesteImpl() {
    }

    @Override
    public XacmlRequestBuilder lagXacmlRequestBuilder(PdpRequest pdpRequest) {
        var xacmlBuilder = new XacmlRequestBuilder();

        var actionAttributeSet = new XacmlAttributeSet();
        actionAttributeSet.addAttribute(NavAbacCommonAttributter.XACML10_ACTION_ACTION_ID,
                pdpRequest.getString(NavAbacCommonAttributter.XACML10_ACTION_ACTION_ID));
        xacmlBuilder.addActionAttributeSet(actionAttributeSet);

        var identer = hentIdenter(pdpRequest, NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_FNR,
                NavAbacCommonAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE);

        if (identer.isEmpty()) {
            populerResources(xacmlBuilder, pdpRequest, null);
        } else {
            for (var ident : identer) {
                populerResources(xacmlBuilder, pdpRequest, ident);
            }
        }

        return xacmlBuilder;
    }

    private static void populerResources(XacmlRequestBuilder xacmlBuilder, PdpRequest pdpRequest, Tuple<String, String> ident) {
        var aksjonspunktTyper = pdpRequest.getListOfString(AbacAttributter.RESOURCE_FORELDREPENGER_SAK_AKSJONSPUNKT_TYPE);
        if (aksjonspunktTyper.isEmpty()) {
            xacmlBuilder.addResourceAttributeSet(byggRessursAttributter(pdpRequest, ident, null));
        } else {
            for (var aksjonspunktType : aksjonspunktTyper) {
                xacmlBuilder.addResourceAttributeSet(byggRessursAttributter(pdpRequest, ident, aksjonspunktType));
            }
        }
    }

    private static XacmlAttributeSet byggRessursAttributter(PdpRequest pdpRequest, Tuple<String, String> ident, String aksjonsounktType) {
        var resourceAttributeSet = new XacmlAttributeSet();
        resourceAttributeSet.addAttribute(NavAbacCommonAttributter.RESOURCE_FELLES_DOMENE,
                pdpRequest.getString(NavAbacCommonAttributter.RESOURCE_FELLES_DOMENE));
        resourceAttributeSet.addAttribute(NavAbacCommonAttributter.RESOURCE_FELLES_RESOURCE_TYPE,
                pdpRequest.getString(NavAbacCommonAttributter.RESOURCE_FELLES_RESOURCE_TYPE));
        setOptionalValueinAttributeSet(resourceAttributeSet, pdpRequest, AbacAttributter.RESOURCE_FORELDREPENGER_SAK_SAKSSTATUS);
        setOptionalValueinAttributeSet(resourceAttributeSet, pdpRequest, AbacAttributter.RESOURCE_FORELDREPENGER_SAK_BEHANDLINGSSTATUS);
        setOptionalValueinAttributeSet(resourceAttributeSet, pdpRequest, AbacAttributter.RESOURCE_FORELDREPENGER_SAK_ANSVARLIG_SAKSBEHANDLER);
        if (ident != null) {
            resourceAttributeSet.addAttribute(ident.getElement1(), ident.getElement2());
        }
        if (aksjonsounktType != null) {
            resourceAttributeSet.addAttribute(AbacAttributter.RESOURCE_FORELDREPENGER_SAK_AKSJONSPUNKT_TYPE, aksjonsounktType);
        }

        return resourceAttributeSet;
    }

    private static void setOptionalValueinAttributeSet(XacmlAttributeSet resourceAttributeSet, PdpRequest pdpRequest, String key) {
        pdpRequest.getOptional(key).ifPresent(s -> resourceAttributeSet.addAttribute(key, s));
    }

    private static List<Tuple<String, String>> hentIdenter(PdpRequest pdpRequest, String... identNøkler) {
        List<Tuple<String, String>> identer = new ArrayList<>();
        for (var key : identNøkler) {
            identer.addAll(pdpRequest.getListOfString(key).stream().map(it -> new Tuple<>(key, it)).collect(Collectors.toList()));
        }
        return identer;
    }
}
