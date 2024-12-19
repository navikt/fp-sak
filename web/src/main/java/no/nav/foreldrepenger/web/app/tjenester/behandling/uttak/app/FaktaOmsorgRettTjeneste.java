package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class FaktaOmsorgRettTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FagsakEgenskapRepository fagsakEgenskapRepository;
    private HistorikkTjenesteAdapter historikkAdapter;

    FaktaOmsorgRettTjeneste() {
        //For CDI proxy
    }

    @Inject
    public FaktaOmsorgRettTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                   HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                   FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.historikkAdapter = historikkTjenesteAdapter;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    public boolean totrinnForAnnenforelderRett(AksjonspunktOppdaterParameter param,
                                               boolean annenforelderHarRett,
                                               Boolean annenforelderMottarUføretrygd,
                                               Boolean annenForelderHarRettEØS) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());
        var tidligereVurderingAvMorsUføretrygd = ytelseFordelingAggregat.getMorUføretrygdAvklaring();
        var endretVurderingAvMorsUføretrygd = endretVurderingAvMorsUføretrygd(annenforelderMottarUføretrygd, tidligereVurderingAvMorsUføretrygd);

        var endretVurderingAvRettEØS = endretVurderingAvRettEØS(annenForelderHarRettEØS, ytelseFordelingAggregat.getAnnenForelderRettEØSAvklaring());

        var harAnnenForeldreRettSokVersjon = ytelseFordelingAggregat.getOppgittRettighet().getHarAnnenForeldreRett();

        var harAnnenForeldreRettBekreftetVersjon = ytelseFordelingAggregat.getAnnenForelderRettAvklaring();

        var avkreftetBrukersOpplysinger = harAvklartUdefinertEllerEndretBekreftet(harAnnenForeldreRettSokVersjon, annenforelderHarRett);
        var erEndretBekreftetVersjon = harAvklartUdefinertEllerEndretBekreftet(harAnnenForeldreRettBekreftetVersjon, annenforelderHarRett);

        // Totrinns er sett hvis saksbehandler avkreftet først gang eller endret etter han bekreftet
        return endretVurderingAvMorsUføretrygd || endretVurderingAvRettEØS || avkreftetBrukersOpplysinger
            || harAnnenForeldreRettBekreftetVersjon != null && erEndretBekreftetVersjon;
    }

    public boolean totrinnForAleneomsorg(AksjonspunktOppdaterParameter param,
                                         boolean aleneomsorg) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());

        var aleneomsorgForBarnetBekreftetVersjon = ytelseFordelingAggregat.getAleneomsorgAvklaring();
        var aleneomsorgForBarnetSokVersjon = ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet();

        var erEndretBekreftetVersjon = harAvklartUdefinertEllerEndretBekreftet(aleneomsorgForBarnetBekreftetVersjon, aleneomsorg);
        var avkreftetBrukersOpplysinger = harAvklartUdefinertEllerEndretBekreftet(aleneomsorgForBarnetSokVersjon, aleneomsorg);

        return avkreftetBrukersOpplysinger || aleneomsorgForBarnetBekreftetVersjon != null && erEndretBekreftetVersjon;
    }

    public void annenforelderRettHistorikkFelt(AksjonspunktOppdaterParameter param,
                                               boolean annenforelderHarRett,
                                               Boolean annenforelderMottarUføretrygd, Boolean annenForelderHarRettEØS) {
        var ytelsefordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());
        var endretVurderingAvMorsUføretrygd = endretVurderingAvMorsUføretrygd(annenforelderMottarUføretrygd, ytelsefordelingAggregat.getMorUføretrygdAvklaring());
        var endretVurderingAvRettEØS = endretVurderingAvRettEØS(annenForelderHarRettEØS, ytelsefordelingAggregat.getAnnenForelderRettEØSAvklaring());
        var harAnnenForeldreRettBekreftetVersjon = ytelsefordelingAggregat.getAnnenForelderRettAvklaring();

        historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER,
            konvertBooleanTilVerdiForAnnenforelderHarRett(harAnnenForeldreRettBekreftetVersjon),
            konvertBooleanTilVerdiForAnnenforelderHarRett(annenforelderHarRett));
        if (endretVurderingAvMorsUføretrygd) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.MOR_MOTTAR_UFØRETRYGD,
                ytelsefordelingAggregat.getMorUføretrygdAvklaring(), annenforelderMottarUføretrygd);
        }
        if (endretVurderingAvRettEØS) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.ANNEN_FORELDER_RETT_EØS,
                ytelsefordelingAggregat.getAnnenForelderRettEØSAvklaring(), annenForelderHarRettEØS);
        }
    }

    public void aleneomsorgHistorikkFelt(AksjonspunktOppdaterParameter param, boolean aleneomsorg) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());

        var aleneomsorgForBarnetBekreftetVersjon = ytelseFordelingAggregat.getAleneomsorgAvklaring();

        var fra = konvertBooleanTilVerdiForAleneomsorgForBarnet(aleneomsorgForBarnetBekreftetVersjon);
        var til = konvertBooleanTilVerdiForAleneomsorgForBarnet(aleneomsorg);

        if (!Objects.equals(fra, til)) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.ALENEOMSORG, fra, til);
        }

    }

    public void omsorgRettHistorikkInnslag(AksjonspunktOppdaterParameter param, String begrunnelse) {
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OMSORG_OG_RETT);
    }

    public void oppdaterAnnenforelderRett(AksjonspunktOppdaterParameter param,
                                          boolean annenforelderHarRett,
                                          Boolean annenforelderMottarUføretrygd,
                                          Boolean annenForelderHarRettEØS) {
        ytelseFordelingTjeneste.bekreftAnnenforelderHarRett(param.getBehandlingId(), annenforelderHarRett, annenForelderHarRettEØS, annenforelderMottarUføretrygd);
        if (annenforelderHarRett || Boolean.TRUE.equals(annenForelderHarRettEØS)) {
            fagsakEgenskapRepository.fjernFagsakMarkering(param.getRef().fagsakId(), FagsakMarkering.BARE_FAR_RETT);
        } else if (!RelasjonsRolleType.MORA.equals(param.getRef().relasjonRolle())) {
            fagsakEgenskapRepository.leggTilFagsakMarkering(param.getRef().fagsakId(), FagsakMarkering.BARE_FAR_RETT);
        }
    }

    public void oppdaterAleneomsorg(AksjonspunktOppdaterParameter param,
                                    boolean aleneomsorg) {
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForAleneomsorg(param.getBehandlingId(), aleneomsorg);
        if (aleneomsorg) {
            fagsakEgenskapRepository.fjernFagsakMarkering(param.getRef().fagsakId(), FagsakMarkering.BARE_FAR_RETT);
        }
    }


    private HistorikkEndretFeltVerdiType konvertBooleanTilVerdiForAleneomsorgForBarnet(Boolean aleneomsorgForBarnet) {
        if (aleneomsorgForBarnet == null) {
            return null;
        }
        return aleneomsorgForBarnet ? HistorikkEndretFeltVerdiType.ALENEOMSORG : HistorikkEndretFeltVerdiType.IKKE_ALENEOMSORG;
    }

    private HistorikkEndretFeltVerdiType konvertBooleanTilVerdiForAnnenforelderHarRett(Boolean annenforelderHarRett) {
        if (annenforelderHarRett == null) {
            return null;
        }
        return annenforelderHarRett ? HistorikkEndretFeltVerdiType.ANNEN_FORELDER_HAR_RETT : HistorikkEndretFeltVerdiType.ANNEN_FORELDER_HAR_IKKE_RETT;
    }

    private boolean harAvklartUdefinertEllerEndretBekreftet(Boolean original, boolean bekreftet) {
        return original == null || !original.equals(bekreftet);
    }

    private boolean endretVurderingAvMorsUføretrygd(Boolean nyVerdi, Boolean tidligereVurdering) {
        return nyVerdi != null && !Objects.equals(tidligereVurdering, nyVerdi);
    }

    private boolean endretVurderingAvRettEØS(Boolean nyVerdi, Boolean tidligereVurdering) {
        return nyVerdi != null && !Objects.equals(tidligereVurdering, nyVerdi);
    }
}
