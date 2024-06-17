package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.BekreftFaktaForOmsorgVurderingDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftFaktaForOmsorgVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftOmsorgOppdaterer implements AksjonspunktOppdaterer<BekreftFaktaForOmsorgVurderingDto> {

    private BehandlingRepositoryProvider behandlingRepository;

    private HistorikkTjenesteAdapter historikkAdapter;

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    BekreftOmsorgOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftOmsorgOppdaterer(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                   HistorikkTjenesteAdapter historikkAdapter,
                                   YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.behandlingRepository = behandlingRepositoryProvider;
        this.historikkAdapter = historikkAdapter;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftFaktaForOmsorgVurderingDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var ytelseFordelingAggregat = behandlingRepository.getYtelsesFordelingRepository().hentAggregat(behandlingId);

        var harOmsorgForBarnetBekreftetVersjon = ytelseFordelingAggregat.getOverstyrtOmsorg();

        var erEndret = opprettHistorikkInnslagForOmsorg(dto, harOmsorgForBarnetBekreftetVersjon);

        var omsorg = dto.getOmsorg();

        var totrinn = setToTrinns(erEndret, Boolean.FALSE.equals(omsorg));

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.FAKTA_FOR_OMSORG);

        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForOmsorg(behandlingId, omsorg);

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private boolean opprettHistorikkInnslagForOmsorg(BekreftFaktaForOmsorgVurderingDto dto, Boolean harOmsorgForBarnetBekreftetVersjon) {
        return oppdaterVedEndretVerdi(HistorikkEndretFeltType.OMSORG, konverterBooleanTilVerdiForOmsorgForBarnet(harOmsorgForBarnetBekreftetVersjon),
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
        // TODO PFP-8740 midlertidig løsning. Inntil en løsning for å støtte dette
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
