package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

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
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakHistorikkTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringFaktaUttakDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringFaktaUttakDto.OverstyrerFaktaUttakDto.class, adapter = Overstyringshåndterer.class)
public class FaktaUttakOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringFaktaUttakDto.OverstyrerFaktaUttakDto> {

    private FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste;
    private FaktaUttakOverstyringFelles faktaUttakOverstyringFelles;

    FaktaUttakOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public FaktaUttakOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                           FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste,
                                           FaktaUttakOverstyringFelles faktaUttakOverstyringFelles) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_FAKTA_UTTAK);
        this.faktaUttakHistorikkTjeneste = faktaUttakHistorikkTjeneste;
        this.faktaUttakOverstyringFelles = faktaUttakOverstyringFelles;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringFaktaUttakDto.OverstyrerFaktaUttakDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        return faktaUttakOverstyringFelles.håndterOverstyring(dto, behandling);
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringFaktaUttakDto.OverstyrerFaktaUttakDto dto) {
        faktaUttakHistorikkTjeneste.byggHistorikkinnslag(dto.getBekreftedePerioder(), dto.getSlettedePerioder(), behandling, true);
    }
}
