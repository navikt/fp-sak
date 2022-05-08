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
                                               boolean opprettUføreGrunnlagHvisMangler) {
        var tidligereVurderingAvMorsUføretrygd = tidligereVurderingAvMorsUføretrygd(param);
        var endretVurderingAvMorsUføretrygd = endretVurderingAvMorsUføretrygd(annenforelderMottarUføretrygd, param,
            tidligereVurderingAvMorsUføretrygd, opprettUføreGrunnlagHvisMangler);

        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());
        var rettAvklaring = ytelseFordelingAggregat.getAnnenForelderRettAvklaring();
        var harAnnenForeldreRettSokVersjon = ytelseFordelingAggregat.getOppgittRettighet().getHarAnnenForeldreRett();

        Boolean harAnnenForeldreRettBekreftetVersjon = rettAvklaring.orElse(null);

        var avkreftetBrukersOpplysinger = harAvklartUdefinertEllerEndretBekreftet(harAnnenForeldreRettSokVersjon, annenforelderHarRett);
        var erEndretBekreftetVersjon = harAvklartUdefinertEllerEndretBekreftet(harAnnenForeldreRettBekreftetVersjon, annenforelderHarRett);

        // Totrinns er sett hvis saksbehandler avkreftet først gang eller endret etter han bekreftet
        var totrinn = endretVurderingAvMorsUføretrygd || avkreftetBrukersOpplysinger || (erEndretBekreftetVersjon && rettAvklaring.isPresent());
        return totrinn;
    }

    public boolean totrinnForAleneomsorg(AksjonspunktOppdaterParameter param,
                                         boolean aleneomsorg) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());
        var perioderAleneOmsorg = ytelseFordelingAggregat.getPerioderAleneOmsorg();

        Boolean aleneomsorgForBarnetBekreftetVersjon = ytelseFordelingAggregat.getPerioderAleneOmsorg()
            .map(p -> !p.getPerioder().isEmpty())
            .orElse(null);
        var aleneomsorgForBarnetSokVersjon = ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet();

        var erEndretBekreftetVersjon = harAvklartUdefinertEllerEndretBekreftet(aleneomsorgForBarnetBekreftetVersjon, aleneomsorg);
        var avkreftetBrukersOpplysinger = harAvklartUdefinertEllerEndretBekreftet(aleneomsorgForBarnetSokVersjon, aleneomsorg);

        return avkreftetBrukersOpplysinger || (perioderAleneOmsorg.isPresent() && erEndretBekreftetVersjon);
    }

    public void annenforelderRettHistorikkFelt(AksjonspunktOppdaterParameter param,
                                               boolean annenforelderHarRett,
                                               Boolean annenforelderMottarUføretrygd,
                                               boolean opprettUføreGrunnlagHvisMangler) {

        var tidligereVurderingAvMorsUføretrygd = tidligereVurderingAvMorsUføretrygd(param);
        var endretVurderingAvMorsUføretrygd = endretVurderingAvMorsUføretrygd(annenforelderMottarUføretrygd, param,
            tidligereVurderingAvMorsUføretrygd, opprettUføreGrunnlagHvisMangler);
        var harAnnenForeldreRettBekreftetVersjon = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId())
            .getAnnenForelderRettAvklaring().orElse(null);
        historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.RETT_TIL_FORELDREPENGER,
            konvertBooleanTilVerdiForAnnenforelderHarRett(harAnnenForeldreRettBekreftetVersjon),
            konvertBooleanTilVerdiForAnnenforelderHarRett(annenforelderHarRett));
        if (endretVurderingAvMorsUføretrygd) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.MOR_MOTTAR_UFØRETRYGD,
                tidligereVurderingAvMorsUføretrygd, annenforelderMottarUføretrygd);
        }
    }

    public void aleneomsorgHistorikkFelt(AksjonspunktOppdaterParameter param, boolean aleneomsorg) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());

        Boolean aleneomsorgForBarnetBekreftetVersjon = ytelseFordelingAggregat.getPerioderAleneOmsorg()
            .map(p -> !p.getPerioder().isEmpty())
            .orElse(null);

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
                                          AktørId annenpartAktørId) {
        ytelseFordelingTjeneste.bekreftAnnenforelderHarRett(param.getBehandlingId(), annenforelderHarRett);
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
            if (uføreGrunnlag.isEmpty() && opprettUføreGrunnlagHvisMangler) {
                uføretrygdRepository.lagreUføreGrunnlagAvkreftetAleneomsorgVersjon(param.getBehandlingId(), annenpartAktørId, annenforelderMottarUføretrygd);
            } else {
                uføreGrunnlag.filter(UføretrygdGrunnlagEntitet::uavklartAnnenForelderMottarUføretrygd)
                    .ifPresent(g -> uføretrygdRepository.lagreUføreGrunnlagOverstyrtVersjon(g.getBehandlingId(), annenforelderMottarUføretrygd));
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
}
