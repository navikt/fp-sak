package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAleneomsorgVurderingDto;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarAleneomsorgVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftAleneomsorgOppdaterer implements AksjonspunktOppdaterer<AvklarAleneomsorgVurderingDto> {


    private FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste;

    BekreftAleneomsorgOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftAleneomsorgOppdaterer(FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste) {
        this.faktaOmsorgRettTjeneste = faktaOmsorgRettTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarAleneomsorgVurderingDto dto, AksjonspunktOppdaterParameter param) {

        if (!dto.getAleneomsorg() && (dto.getAnnenforelderHarRett() == null
            || !dto.getAnnenforelderHarRett() && dto.getAnnenforelderMottarUføretrygd() == null)) {
            throw new FunksjonellException("FP-093924", "Avkreftet aleneomsorg mangler verdi for annen forelder rett eller uføretrygd.",
                "Angi om annen forelder har rett eller om annen forelder mottar uføretrygd.");
        }
        var totrinn = faktaOmsorgRettTjeneste.totrinnForAleneomsorg(param, dto.getAleneomsorg());
        faktaOmsorgRettTjeneste.aleneomsorgHistorikkFelt(param, dto.getAleneomsorg());
        faktaOmsorgRettTjeneste.oppdaterAleneomsorg(param, dto.getAleneomsorg());
        if (!dto.getAleneomsorg() && dto.getAnnenforelderHarRett() != null) {
            // Inntil videre ...
            totrinn = totrinn || faktaOmsorgRettTjeneste.totrinnForAnnenforelderRett(param, dto.getAnnenforelderHarRett(),
                dto.getAnnenforelderMottarUføretrygd(), dto.getAnnenForelderHarRettEØS());
            faktaOmsorgRettTjeneste.annenforelderRettHistorikkFelt(param, dto.getAnnenforelderHarRett(),
                dto.getAnnenforelderMottarUføretrygd(), dto.getAnnenForelderHarRettEØS());
            faktaOmsorgRettTjeneste.oppdaterAnnenforelderRett(param, dto.getAnnenforelderHarRett(),
                dto.getAnnenforelderMottarUføretrygd(), dto.getAnnenForelderHarRettEØS());
        }
        faktaOmsorgRettTjeneste.omsorgRettHistorikkInnslag(param, dto.getBegrunnelse());
        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

}
