package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import static java.lang.Boolean.TRUE;

import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarAnnenforelderHarRettDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarAnnenforelderHarRettOppdaterer implements AksjonspunktOppdaterer<AvklarAnnenforelderHarRettDto> {

    private FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste;
    private Historikkinnslag2Repository historikkRepository;

    AvklarAnnenforelderHarRettOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarAnnenforelderHarRettOppdaterer(FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste, Historikkinnslag2Repository historikkRepository) {
        this.faktaOmsorgRettTjeneste = faktaOmsorgRettTjeneste;
        this.historikkRepository = historikkRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarAnnenforelderHarRettDto dto, AksjonspunktOppdaterParameter param) {
        var annenforelderHarRett = dto.getAnnenforelderHarRett();
        var annenForelderHarRettEØS =
            TRUE.equals(annenforelderHarRett) && dto.getAnnenForelderHarRettEØS() != null ? Boolean.FALSE : dto.getAnnenForelderHarRettEØS();
        var totrinn = faktaOmsorgRettTjeneste.totrinnForAnnenforelderRett(param, annenforelderHarRett, dto.getAnnenforelderMottarUføretrygd(),
            annenForelderHarRettEØS);
        oppretHistorikkinnslag(dto, param, annenforelderHarRett, annenForelderHarRettEØS);

        faktaOmsorgRettTjeneste.oppdaterAnnenforelderRett(param, annenforelderHarRett, dto.getAnnenforelderMottarUføretrygd(),
            annenForelderHarRettEØS);


        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private void oppretHistorikkinnslag(AvklarAnnenforelderHarRettDto dto,
                                        AksjonspunktOppdaterParameter param,
                                        Boolean annenforelderHarRett,
                                        Boolean annenForelderHarRettEØS) {
        var historikkinnslagLinjer = Stream.concat(
                faktaOmsorgRettTjeneste.annenforelderRettHistorikkLinjer(param, annenforelderHarRett, dto.getAnnenforelderMottarUføretrygd(),
                    annenForelderHarRettEØS).stream(), faktaOmsorgRettTjeneste.omsorgRettHistorikkLinje(param, dto.getBegrunnelse()).stream())
            .toList();

        var historikkinnslag = new Historikkinnslag2.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(param.getFagsakId())
            .medBehandlingId(param.getRef().behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OMSORG_OG_RETT)
            .medLinjer(historikkinnslagLinjer)
            .build();
        historikkRepository.lagre(historikkinnslag);
    }


}
