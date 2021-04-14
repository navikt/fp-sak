package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakHistorikkTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakToTrinnsTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.KontrollerOppgittFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarAnnenforelderHarRettDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAnnenforelderHarRettOppdaterer implements AksjonspunktOppdaterer<AvklarAnnenforelderHarRettDto>  {

    private KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste;
    private FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste;
    private FaktaUttakToTrinnsTjeneste faktaUttakToTrinnsTjeneste;

    AvklarAnnenforelderHarRettOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarAnnenforelderHarRettOppdaterer(KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste,
                                                FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste,
                                                FaktaUttakToTrinnsTjeneste faktaUttakToTrinnsTjeneste) {
        this.kontrollerOppgittFordelingTjeneste = kontrollerOppgittFordelingTjeneste;
        this.faktaUttakHistorikkTjeneste = faktaUttakHistorikkTjeneste;
        this.faktaUttakToTrinnsTjeneste = faktaUttakToTrinnsTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(AvklarAnnenforelderHarRettDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = param.getBehandling();
        var totrinn = faktaUttakToTrinnsTjeneste.oppdaterToTrinnskontrollVedEndringerAnnenforelderHarRett(dto, behandling);
        faktaUttakHistorikkTjeneste.byggHistorikkinnslagForAvklarAnnenforelderHarIkkeRett(dto, param);
        kontrollerOppgittFordelingTjeneste.avklarAnnenforelderHarIkkeRett(dto, behandling);

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();

    }

}
