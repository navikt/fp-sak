package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ufore.UføretrygdRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
public class FaktaOmsorgRettTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;
    private UføretrygdRepository uføretrygdRepository;

    FaktaOmsorgRettTjeneste() {
        //For CDI proxy
    }

    @Inject
    public FaktaOmsorgRettTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                   HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                   UføretrygdRepository uføretrygdRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.historikkAdapter = historikkTjenesteAdapter;
        this.uføretrygdRepository = uføretrygdRepository;
    }

    public boolean totrinnForAnnenforelderRett(AksjonspunktOppdaterParameter param,
                                               boolean annenforelderHarRett,
                                               Boolean annenforelderMottarUføretrygd,
                                               boolean opprettUføreGrunnlagHvisMangler,
                                               Boolean annenForelderHarRettEØS) {
        var tidligereVurderingAvMorsUføretrygd = tidligereVurderingAvMorsUføretrygd(param);
        var endretVurderingAvMorsUføretrygd = endretVurderingAvMorsUføretrygd(annenforelderMottarUføretrygd, param,
            tidligereVurderingAvMorsUføretrygd, opprettUføreGrunnlagHvisMangler);

        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());
        var endretVurderingAvRettEØS = endretVurderingAvRettEØS(annenForelderHarRettEØS, ytelseFordelingAggregat.getAnnenForelderRettEØSAvklaring());

        var harAnnenForeldreRettSokVersjon = ytelseFordelingAggregat.getOppgittRettighet().getHarAnnenForeldreRett();

        var harAnnenForeldreRettBekreftetVersjon = ytelseFordelingAggregat.getAnnenForelderRettAvklaring();

        var avkreftetBrukersOpplysinger = harAvklartUdefinertEllerEndretBekreftet(harAnnenForeldreRettSokVersjon, annenforelderHarRett);
        var erEndretBekreftetVersjon = harAvklartUdefinertEllerEndretBekreftet(harAnnenForeldreRettBekreftetVersjon, annenforelderHarRett);

        // Totrinns er sett hvis saksbehandler avkreftet først gang eller endret etter han bekreftet
        var totrinn = endretVurderingAvMorsUføretrygd || endretVurderingAvRettEØS || avkreftetBrukersOpplysinger ||
            (harAnnenForeldreRettBekreftetVersjon != null && erEndretBekreftetVersjon);
        return totrinn;
    }

    public boolean totrinnForAleneomsorg(AksjonspunktOppdaterParameter param,
                                         boolean aleneomsorg) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());

        Boolean aleneomsorgForBarnetBekreftetVersjon = ytelseFordelingAggregat.getAleneomsorgAvklaring();
        var aleneomsorgForBarnetSokVersjon = ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet();

        var erEndretBekreftetVersjon = harAvklartUdefinertEllerEndretBekreftet(aleneomsorgForBarnetBekreftetVersjon, aleneomsorg);
        var avkreftetBrukersOpplysinger = harAvklartUdefinertEllerEndretBekreftet(aleneomsorgForBarnetSokVersjon, aleneomsorg);

        return avkreftetBrukersOpplysinger || (aleneomsorgForBarnetBekreftetVersjon != null && erEndretBekreftetVersjon);
    }

    public void annenforelderRettHistorikkFelt(AksjonspunktOppdaterParameter param,
                                               boolean annenforelderHarRett,
                                               Boolean annenforelderMottarUføretrygd,
                                               boolean opprettUføreGrunnlagHvisMangler,
                                               Boolean annenForelderHarRettEØS) {
        var ytelsefordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());
        var tidligereVurderingAvMorsUføretrygd = tidligereVurderingAvMorsUføretrygd(param);
        var endretVurderingAvMorsUføretrygd = endretVurderingAvMorsUføretrygd(annenforelderMottarUføretrygd, param,
            tidligereVurderingAvMorsUføretrygd, opprettUføreGrunnlagHvisMangler);
        var endretVurderingAvRettEØS = endretVurderingAvRettEØS(annenForelderHarRettEØS, ytelsefordelingAggregat.getAnnenForelderRettEØSAvklaring());
        var harAnnenForeldreRettBekreftetVersjon = ytelsefordelingAggregat.getAnnenForelderRettAvklaring();

        historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER,
            konvertBooleanTilVerdiForAnnenforelderHarRett(harAnnenForeldreRettBekreftetVersjon),
            konvertBooleanTilVerdiForAnnenforelderHarRett(annenforelderHarRett));
        if (endretVurderingAvMorsUføretrygd) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.MOR_MOTTAR_UFØRETRYGD,
                tidligereVurderingAvMorsUføretrygd, annenforelderMottarUføretrygd);
        }
        if (endretVurderingAvRettEØS) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.ANNEN_FORELDER_RETT_EØS,
                ytelsefordelingAggregat.getAnnenForelderRettEØSAvklaring(), annenForelderHarRettEØS);
        }
    }

    public void aleneomsorgHistorikkFelt(AksjonspunktOppdaterParameter param, boolean aleneomsorg) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());

        Boolean aleneomsorgForBarnetBekreftetVersjon = ytelseFordelingAggregat.getAleneomsorgAvklaring();

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
                                          boolean opprettUføreGrunnlagHvisMangler,
                                          AktørId annenpartAktørId,
                                          Boolean annenForelderHarRettEØS) {
        ytelseFordelingTjeneste.bekreftAnnenforelderHarRett(param.getBehandlingId(), annenforelderHarRett, annenForelderHarRettEØS);
        oppdaterUføretrygdVedBehov(annenforelderMottarUføretrygd, param, opprettUføreGrunnlagHvisMangler, annenpartAktørId);
    }

    public void oppdaterAleneomsorg(AksjonspunktOppdaterParameter param,
                                    boolean aleneomsorg) {
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForAleneomsorg(param.getBehandlingId(), aleneomsorg);
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

    private void oppdaterUføretrygdVedBehov(Boolean annenforelderMottarUføretrygd, AksjonspunktOppdaterParameter param,
                                            boolean opprettUføreGrunnlagHvisMangler,
                                            AktørId annenpartAktørId) {
        if (annenforelderMottarUføretrygd != null) {
            var uføreGrunnlag = uføretrygdRepository.hentGrunnlag(param.getBehandlingId());
            if ((uføreGrunnlag.isEmpty() || uføreGrunnlag.get().getUføretrygdRegister() == null) && opprettUføreGrunnlagHvisMangler) {
                uføretrygdRepository.lagreUføreGrunnlagAvkreftetAleneomsorgVersjon(param.getBehandlingId(), annenpartAktørId, annenforelderMottarUføretrygd);
            } else {
                uføreGrunnlag.ifPresent(g -> uføretrygdRepository.lagreUføreGrunnlagOverstyrtVersjon(g.getBehandlingId(), annenforelderMottarUføretrygd));
            }
        }
    }

    private boolean endretVurderingAvMorsUføretrygd(Boolean nyVerdi, AksjonspunktOppdaterParameter param, Boolean tidligereVurdering, boolean opprettUføreGrunnlagHvisMangler) {
        return nyVerdi != null && (uføretrygdRepository.hentGrunnlag(param.getBehandlingId()).isPresent() || opprettUføreGrunnlagHvisMangler)
            && !Objects.equals(tidligereVurdering, nyVerdi);
    }

    private Boolean tidligereVurderingAvMorsUføretrygd(AksjonspunktOppdaterParameter param) {
        return uføretrygdRepository.hentGrunnlag(param.getBehandlingId())
            .map(UføretrygdGrunnlagEntitet::getUføretrygdOverstyrt).orElse(null);
    }

    private boolean endretVurderingAvRettEØS(Boolean nyVerdi, Boolean tidligereVurdering) {
        return nyVerdi != null && !Objects.equals(tidligereVurdering, nyVerdi);
    }
}
