package no.nav.foreldrepenger.web.app.tjenester.behandling.dekningsgrad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarDekningsgradDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarDekningsgradOppdaterer implements AksjonspunktOppdaterer<AvklarDekningsgradDto> {

    private AvklarDekningsgradFellesTjeneste fellesTjeneste;

    @Inject
    public AvklarDekningsgradOppdaterer(AvklarDekningsgradFellesTjeneste fellesTjeneste) {
        this.fellesTjeneste = fellesTjeneste;
    }

    AvklarDekningsgradOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(AvklarDekningsgradDto dto, AksjonspunktOppdaterParameter param) {
        return fellesTjeneste.oppdater(dto.getDekningsgrad(), param.getFagsakId(), param.getBehandlingId(), dto.getBegrunnelse());
    }
}
