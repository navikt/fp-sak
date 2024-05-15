package no.nav.foreldrepenger.web.app.tjenester.behandling.dekningsgrad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
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
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;

    @Inject
    public AvklarDekningsgradOppdaterer(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                        AvklarDekningsgradHistorikkinnslagTjeneste avklarDekningsgradHistorikkinnslagTjeneste,
                                        FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.avklarDekningsgradHistorikkinnslagTjeneste = avklarDekningsgradHistorikkinnslagTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
    }

    AvklarDekningsgradOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(AvklarDekingsgradDto dto, AksjonspunktOppdaterParameter param) {
        var avklartDekningsgrad = Dekningsgrad.grad(dto.avklartDekningsgrad());
        lagreDekningsgrad(param, avklartDekningsgrad);
        avklarDekningsgradHistorikkinnslagTjeneste.opprettHistorikkinnslag(dto);
        return OppdateringResultat.utenTransisjon().build();
    }

    private void lagreDekningsgrad(AksjonspunktOppdaterParameter param, Dekningsgrad avklartDekningsgrad) {
        ytelseFordelingTjeneste.lagreSakskompleksDekningsgrad(param.getBehandlingId(), avklartDekningsgrad);
        fagsakRelasjonTjeneste.oppdaterDekningsgrad(param.getRef().fagsakId(), avklartDekningsgrad);
    }
}
