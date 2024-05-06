package no.nav.foreldrepenger.web.app.tjenester.behandling.dekningsgrad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarDekingsgradDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarDekningsgradOppdaterer implements AksjonspunktOppdaterer<AvklarDekingsgradDto> {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private AvklarDekningsgradHistorikkinnslagTjeneste avklarDekningsgradHistorikkinnslagTjeneste;

    @Inject
    public AvklarDekningsgradOppdaterer(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                        AvklarDekningsgradHistorikkinnslagTjeneste avklarDekningsgradHistorikkinnslagTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.avklarDekningsgradHistorikkinnslagTjeneste = avklarDekningsgradHistorikkinnslagTjeneste;
    }

    AvklarDekningsgradOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(AvklarDekingsgradDto dto, AksjonspunktOppdaterParameter param) {
        ytelseFordelingTjeneste.lagreSakskompleksDekningsgrad(param.getBehandlingId(), Dekningsgrad.grad(dto.avklartDekningsgrad()));
        avklarDekningsgradHistorikkinnslagTjeneste.opprettHistorikkinnslag(dto);
        return OppdateringResultat.utenTransisjon().build();
    }
}
