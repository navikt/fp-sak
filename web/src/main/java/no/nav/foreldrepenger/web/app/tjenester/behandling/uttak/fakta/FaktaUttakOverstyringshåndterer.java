package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringFaktaUttakDto.class, adapter = Overstyringshåndterer.class)
public class FaktaUttakOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringFaktaUttakDto> {

    private FaktaUttakFellesTjeneste fellesTjeneste;

    @Inject
    public FaktaUttakOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                           FaktaUttakFellesTjeneste fellesTjeneste) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_FAKTA_UTTAK);
        this.fellesTjeneste = fellesTjeneste;
    }

    FaktaUttakOverstyringshåndterer() {
        //CDI
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringFaktaUttakDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        return fellesTjeneste.oppdater(dto.getPerioder(), behandling.getId());
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringFaktaUttakDto dto) {

    }
}
