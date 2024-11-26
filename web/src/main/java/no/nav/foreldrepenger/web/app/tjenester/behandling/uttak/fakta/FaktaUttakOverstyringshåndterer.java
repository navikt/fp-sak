package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

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
@DtoTilServiceAdapter(dto = OverstyringFaktaUttakDto.class, adapter = Overstyringshåndterer.class)
public class FaktaUttakOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringFaktaUttakDto> {

    private FaktaUttakFellesTjeneste fellesTjeneste;

    @Inject
    public FaktaUttakOverstyringshåndterer(FaktaUttakFellesTjeneste fellesTjeneste) {
        super(AksjonspunktDefinisjon.OVERSTYRING_FAKTA_UTTAK);
        this.fellesTjeneste = fellesTjeneste;
    }

    FaktaUttakOverstyringshåndterer() {
        //CDI
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringFaktaUttakDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        return fellesTjeneste.oppdater(dto.getBegrunnelse(), dto.getPerioder(), behandling.getId(), true);
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringFaktaUttakDto dto) {

    }
}
