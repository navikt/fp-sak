package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaOmsorgRettTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAleneomsorgVurderingDto;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarAleneomsorgVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftAleneomsorgOppdaterer implements AksjonspunktOppdaterer<AvklarAleneomsorgVurderingDto> {

    private static final Logger LOG = LoggerFactory.getLogger(BekreftAleneomsorgOppdaterer.class);

    private FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste;
    private HistorikkinnslagRepository historikkRepository;

    BekreftAleneomsorgOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftAleneomsorgOppdaterer(FaktaOmsorgRettTjeneste faktaOmsorgRettTjeneste, HistorikkinnslagRepository historikkRepository) {
        this.faktaOmsorgRettTjeneste = faktaOmsorgRettTjeneste;
        this.historikkRepository = historikkRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarAleneomsorgVurderingDto dto, AksjonspunktOppdaterParameter param) {
        // Må ha valgt aleneomsorg = true, annenForelderRett = true, eller annenForelderRettEØS = true, eller valgt uføretrygd true/false
        if (måVelgeUføre(dto) && dto.getAnnenforelderMottarUføretrygd() == null) {
            LOG.warn("Avklar aleneomsorg: får inn denne Dto'en som gir feil {}", dto);
            throw new FunksjonellException("FP-093924", "Avkreftet aleneomsorg mangler verdi for annen forelder rett eller uføretrygd.",
                "Angi om annen forelder har rett eller om annen forelder mottar uføretrygd.");
        }
        var totrinn = faktaOmsorgRettTjeneste.totrinnForAleneomsorg(param, dto.getAleneomsorg());


        List<HistorikkinnslagLinjeBuilder> historikkinnslagLinjer = new ArrayList<>();
        var aleneomsorglinje = faktaOmsorgRettTjeneste.aleneomsorgHistorikkLinje(param, dto.getAleneomsorg());
        aleneomsorglinje.ifPresent(historikkinnslagLinjer::add);

        faktaOmsorgRettTjeneste.oppdaterAleneomsorg(param, dto.getAleneomsorg());
        if (!dto.getAleneomsorg() && dto.getAnnenforelderHarRett() != null) {
            // Inntil videre ...
            totrinn = totrinn || faktaOmsorgRettTjeneste.totrinnForAnnenforelderRett(param, dto.getAnnenforelderHarRett(),
                dto.getAnnenforelderMottarUføretrygd(), dto.getAnnenForelderHarRettEØS());
            historikkinnslagLinjer.addAll(faktaOmsorgRettTjeneste.annenforelderRettHistorikkLinjer(param, dto.getAnnenforelderHarRett(),
                dto.getAnnenforelderMottarUføretrygd(), dto.getAnnenForelderHarRettEØS()));
            faktaOmsorgRettTjeneste.oppdaterAnnenforelderRett(param, dto.getAnnenforelderHarRett(), dto.getAnnenforelderMottarUføretrygd(),
                dto.getAnnenForelderHarRettEØS());
        }
        var omsorgRettLinje = faktaOmsorgRettTjeneste.omsorgRettHistorikkLinje(param, dto.getBegrunnelse());
        omsorgRettLinje.ifPresent(historikkinnslagLinjer::add);

        historikkRepository.lagre(new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(param.getFagsakId())
            .medBehandlingId(param.getRef().behandlingId())
            .medTittel(SkjermlenkeType.FAKTA_OMSORG_OG_RETT)
            .medLinjer(historikkinnslagLinjer)
            .build());

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private static boolean måVelgeUføre(AvklarAleneomsorgVurderingDto dto) {
        return !(Objects.equals(dto.getAleneomsorg(), Boolean.TRUE) || Objects.equals(dto.getAnnenforelderHarRett(), Boolean.TRUE) || Objects.equals(
            dto.getAnnenForelderHarRettEØS(), Boolean.TRUE));
    }

}
