package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.domene.ytelsefordeling.BekreftFaktaForOmsorgVurderingAksjonspunktDto;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.BekreftFaktaForOmsorgVurderingDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftFaktaForOmsorgVurderingDto.BekreftAleneomsorgVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftAleneomsorgOppdaterer implements AksjonspunktOppdaterer<BekreftFaktaForOmsorgVurderingDto.BekreftAleneomsorgVurderingDto> {

    private BehandlingRepositoryProvider behandlingRepository;

    private HistorikkTjenesteAdapter historikkAdapter;

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    BekreftAleneomsorgOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftAleneomsorgOppdaterer(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                        HistorikkTjenesteAdapter historikkAdapter,
                                        YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.behandlingRepository = behandlingRepositoryProvider;
        this.historikkAdapter = historikkAdapter;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftFaktaForOmsorgVurderingDto.BekreftAleneomsorgVurderingDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();

        var ytelseFordelingAggregat = behandlingRepository.getYtelsesFordelingRepository().hentAggregat(behandlingId);
        var perioderAleneOmsorg = ytelseFordelingAggregat.getPerioderAleneOmsorg();

        var aleneomsorgForBarnetSokVersjon = ytelseFordelingAggregat
            .getOppgittRettighet().getHarAleneomsorgForBarnet();

        Boolean aleneomsorgForBarnetBekreftetVersjon = null;
        if (perioderAleneOmsorg.isPresent()) {
            aleneomsorgForBarnetBekreftetVersjon = !perioderAleneOmsorg.get().getPerioder().isEmpty();
        }

        var erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.ALENEOMSORG,
            konvertBooleanTilVerdiForAleneomsorgForBarnet(aleneomsorgForBarnetBekreftetVersjon),
            konvertBooleanTilVerdiForAleneomsorgForBarnet(dto.getAleneomsorg()));

        var avkreftet = avkrefterBrukersOpplysninger(aleneomsorgForBarnetSokVersjon, dto.getAleneomsorg());

        var totrinn = setToTrinns(perioderAleneOmsorg, erEndret, avkreftet);

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OMSORG_OG_RETT);

        final var adapter = new BekreftFaktaForOmsorgVurderingAksjonspunktDto(dto.getAleneomsorg(), null, null);
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForOmsorg(behandlingId, adapter);

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilVerdiForAleneomsorgForBarnet(Boolean aleneomsorgForBarnet) {
        if (aleneomsorgForBarnet == null) {
            return null;
        }
        return aleneomsorgForBarnet ? HistorikkEndretFeltVerdiType.ALENEOMSORG : HistorikkEndretFeltVerdiType.IKKE_ALENEOMSORG;
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, HistorikkEndretFeltVerdiType fraVerdi, HistorikkEndretFeltVerdiType tilVerdi) {
        if (!Objects.equals(tilVerdi, fraVerdi)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, fraVerdi, tilVerdi);
            return true;
        }
        return false;
    }

    private boolean avkrefterBrukersOpplysninger(Object original, Object bekreftet) {
        return !Objects.equals(bekreftet, original);
    }

    private boolean setToTrinns(Optional<PerioderAleneOmsorgEntitet> perioderAleneOmsorg, boolean erEndret, boolean avkreftet) {
        // Totrinns er sett hvis saksbehandler avkreftet først gang eller endret etter han bekreftet
        return avkreftet || (erEndret && perioderAleneOmsorg.isPresent());
    }

}
