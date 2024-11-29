package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import static no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType.FAKTA_FOR_OMSORG;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.BekreftFaktaForOmsorgVurderingDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftFaktaForOmsorgVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftOmsorgOppdaterer implements AksjonspunktOppdaterer<BekreftFaktaForOmsorgVurderingDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private Historikkinnslag2Repository historikkinnslag2Repository;


    BekreftOmsorgOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftOmsorgOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                   YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                   Historikkinnslag2Repository historikkinnslag2Repository) {
        this.historikkAdapter = historikkAdapter;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
    }

    @Override
    public OppdateringResultat oppdater(BekreftFaktaForOmsorgVurderingDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandlingId);

        var harOmsorgForBarnetBekreftetVersjon = ytelseFordelingAggregat.getOverstyrtOmsorg();

        var erEndret = opprettHistorikkInnslagForOmsorg(dto, harOmsorgForBarnetBekreftetVersjon);

        var omsorg = dto.getOmsorg();

        erEndret = !Objects.equals(harOmsorgForBarnetBekreftetVersjon, dto.getOmsorg());
        if (param.erBegrunnelseEndret() || !Objects.equals(harOmsorgForBarnetBekreftetVersjon, dto.getOmsorg())) {
            var historikkinnslag = new Historikkinnslag2.Builder()
                .medBehandlingId(behandlingId)
                .medFagsakId(param.getRef().fagsakId())
                .medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medTittel(SkjermlenkeType.FAKTA_FOR_OMSORG)
                .addTekstlinje(HistorikkinnslagTekstlinjeBuilder.fraTilEquals("Omsorg",
                    konverterBooleanTilVerdiForOmsorgForBarnet(harOmsorgForBarnetBekreftetVersjon),
                    konverterBooleanTilVerdiForOmsorgForBarnet(dto.getOmsorg())))
                .addTekstlinje(dto.getBegrunnelse())
                .build();
            historikkinnslag2Repository.lagre(historikkinnslag);
        }
        var totrinn = setToTrinns(erEndret, Boolean.FALSE.equals(omsorg));

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(FAKTA_FOR_OMSORG);

        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForOmsorg(behandlingId, omsorg);

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private boolean opprettHistorikkInnslagForOmsorg(BekreftFaktaForOmsorgVurderingDto dto, Boolean harOmsorgForBarnetBekreftetVersjon) {
        return oppdaterVedEndretVerdi(HistorikkEndretFeltType.OMSORG,
                konverterBooleanTilVerdiForOmsorgForBarnet(harOmsorgForBarnetBekreftetVersjon),
                konverterBooleanTilVerdiForOmsorgForBarnet(dto.getOmsorg()));
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

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, String original, String bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            return true;
        }
        return false;
    }

}
