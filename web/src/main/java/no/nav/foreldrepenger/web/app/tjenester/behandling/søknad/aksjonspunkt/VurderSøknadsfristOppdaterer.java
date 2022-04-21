package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderSøknadsfristDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderSøknadsfristOppdaterer implements AksjonspunktOppdaterer<VurderSøknadsfristDto> {

    @Override
    public OppdateringResultat oppdater(VurderSøknadsfristDto dto, AksjonspunktOppdaterParameter param) {
        var fagsakYtelseType = param.getRef().fagsakYtelseType();
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(VurderSøknadsfristOppdatererTjeneste.class, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Finner ikke tjeneste for ytelsetype " + fagsakYtelseType));
        return tjeneste.oppdater(dto, param);
    }
}
