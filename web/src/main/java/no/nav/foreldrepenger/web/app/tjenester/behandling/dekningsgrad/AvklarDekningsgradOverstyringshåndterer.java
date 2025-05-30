package no.nav.foreldrepenger.web.app.tjenester.behandling.dekningsgrad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarDekningsgradOverstyringDto.class, adapter = Overstyringshåndterer.class)
public class AvklarDekningsgradOverstyringshåndterer implements Overstyringshåndterer<AvklarDekningsgradOverstyringDto> {

    private AvklarDekningsgradFellesTjeneste fellesTjeneste;

    @Inject
    public AvklarDekningsgradOverstyringshåndterer(AvklarDekningsgradFellesTjeneste fellesTjeneste) {
        this.fellesTjeneste = fellesTjeneste;
    }

    AvklarDekningsgradOverstyringshåndterer() {
        //CDI
    }

    @Override
    public OppdateringResultat håndterOverstyring(AvklarDekningsgradOverstyringDto dto, BehandlingReferanse ref) {
        return fellesTjeneste.oppdater(dto.getDekningsgrad(), ref.fagsakId(), ref.behandlingId(), dto.getBegrunnelse());
    }
}
