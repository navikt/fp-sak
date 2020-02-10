package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;


@ApplicationScoped
@DtoTilServiceAdapter(dto = KontrollerRevurderingsBehandlingDto.class, adapter = AksjonspunktOppdaterer.class)
class KontrollerRevurderingsBehandlingOppdaterer implements AksjonspunktOppdaterer<KontrollerRevurderingsBehandlingDto> {

    @Override
    public OppdateringResultat oppdater(KontrollerRevurderingsBehandlingDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.utenOveropp();
    }

}
