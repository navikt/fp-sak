package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.BekreftFaktaForOmsorgVurderingDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftFaktaForOmsorgVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftOmsorgOppdaterer implements AksjonspunktOppdaterer<BekreftFaktaForOmsorgVurderingDto> {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private Historikkinnslag2Repository historikkinnslag2Repository;


    BekreftOmsorgOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftOmsorgOppdaterer(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                   Historikkinnslag2Repository historikkinnslag2Repository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
    }

    @Override
    public OppdateringResultat oppdater(BekreftFaktaForOmsorgVurderingDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);

        var harOmsorgForBarnetBekreftetVersjon = ytelseFordelingAggregat.getOverstyrtOmsorg();

        var omsorg = dto.getOmsorg();

        var erEndret = !Objects.equals(harOmsorgForBarnetBekreftetVersjon, dto.getOmsorg());
        if (param.erBegrunnelseEndret() || !Objects.equals(harOmsorgForBarnetBekreftetVersjon, dto.getOmsorg())) {
            lagreHistorikk(dto, param, behandlingId, harOmsorgForBarnetBekreftetVersjon);
        }
        var totrinn = setToTrinns(erEndret, Boolean.FALSE.equals(omsorg));

        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForOmsorg(behandlingId, omsorg);

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private void lagreHistorikk(BekreftFaktaForOmsorgVurderingDto dto,
                           AksjonspunktOppdaterParameter param,
                           Long behandlingId,
                           Boolean harOmsorgForBarnetBekreftetVersjon) {
        var historikkinnslag = new Historikkinnslag2.Builder()
            .medBehandlingId(behandlingId)
            .medFagsakId(param.getFagsakId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_FOR_OMSORG)
            .addLinje(HistorikkinnslagLinjeBuilder.fraTilEquals("Omsorg",
                konverterBooleanTilVerdiForOmsorgForBarnet(harOmsorgForBarnetBekreftetVersjon),
                konverterBooleanTilVerdiForOmsorgForBarnet(dto.getOmsorg())))
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }

    private boolean setToTrinns(boolean erEndret, boolean ikkeOmsorg) {
        // Totrinns er sett hvis saksbehandler avkreftet først gang eller endret etter han bekreftet
        return ikkeOmsorg || erEndret;
    }

    private String konverterBooleanTilVerdiForOmsorgForBarnet(Boolean omsorgForBarnet) {
        if (omsorgForBarnet == null) {
            return null;
        }
        return omsorgForBarnet ? "Søker har omsorg for barnet" : "Søker har ikke omsorg for barnet";
    }

}
