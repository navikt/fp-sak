package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static java.lang.Boolean.TRUE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarAnnenforelderHarRettDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAnnenforelderHarRettOppdaterer implements AksjonspunktOppdaterer<AvklarAnnenforelderHarRettDto>  {

    private FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste;

    AvklarAnnenforelderHarRettOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarAnnenforelderHarRettOppdaterer(FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste) {
        this.faktaOmsorgRettTjeneste = faktaOmsorgRettTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarAnnenforelderHarRettDto dto, AksjonspunktOppdaterParameter param) {
        var annenforelderHarRett = dto.getAnnenforelderHarRett();
        var annenForelderHarRettEØS = TRUE.equals(annenforelderHarRett) && dto.getAnnenForelderHarRettEØS() != null ? Boolean.FALSE : dto.getAnnenForelderHarRettEØS();
        var totrinn = faktaOmsorgRettTjeneste.totrinnForAnnenforelderRett(param, annenforelderHarRett,
            dto.getAnnenforelderMottarUføretrygd(), annenForelderHarRettEØS);
        faktaOmsorgRettTjeneste.annenforelderRettHistorikkFelt(param, annenforelderHarRett,
            dto.getAnnenforelderMottarUføretrygd(), annenForelderHarRettEØS);
        faktaOmsorgRettTjeneste.omsorgRettHistorikkInnslag(param, dto.getBegrunnelse());
        faktaOmsorgRettTjeneste.oppdaterAnnenforelderRett(param, annenforelderHarRett,
            dto.getAnnenforelderMottarUføretrygd(), annenForelderHarRettEØS);
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }


}
