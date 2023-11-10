package no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;


@ApplicationScoped
@DtoTilServiceAdapter(dto = KontrollerStorEtterbetalingSøkerDto.class, adapter = AksjonspunktOppdaterer.class)
class KontrollerStorEtterbetalingOppdaterer implements AksjonspunktOppdaterer<KontrollerStorEtterbetalingSøkerDto> {

    @Override
    public OppdateringResultat oppdater(KontrollerStorEtterbetalingSøkerDto dto, AksjonspunktOppdaterParameter param) {
        return OppdateringResultat.utenOverhopp();
    }

}


