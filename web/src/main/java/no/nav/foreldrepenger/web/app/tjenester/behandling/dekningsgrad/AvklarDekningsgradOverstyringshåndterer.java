package no.nav.foreldrepenger.web.app.tjenester.behandling.dekningsgrad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarDekningsgradOverstyringDto.class, adapter = Overstyringshåndterer.class)
public class AvklarDekningsgradOverstyringshåndterer extends AbstractOverstyringshåndterer<AvklarDekningsgradOverstyringDto> {

    private AvklarDekningsgradFellesTjeneste fellesTjeneste;

    @Inject
    public AvklarDekningsgradOverstyringshåndterer(AvklarDekningsgradFellesTjeneste fellesTjeneste) {
        super(AksjonspunktDefinisjon.OVERSTYRING_AV_DEKNINGSGRAD);
        this.fellesTjeneste = fellesTjeneste;
    }

    AvklarDekningsgradOverstyringshåndterer() {
        //CDI
    }

    @Override
    public OppdateringResultat håndterOverstyring(AvklarDekningsgradOverstyringDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        return fellesTjeneste.oppdater(dto.getDekningsgrad(), kontekst.getFagsakId(), kontekst.getBehandlingId(), dto.getBegrunnelse());
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, AvklarDekningsgradOverstyringDto dto) {

    }
}
