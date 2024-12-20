package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

@ApplicationScoped
public class FaktaOmsorgRettTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private FagsakEgenskapRepository fagsakEgenskapRepository;

    FaktaOmsorgRettTjeneste() {
        //For CDI proxy
    }

    @Inject
    public FaktaOmsorgRettTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                   FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
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

    public boolean totrinnForAleneomsorg(AksjonspunktOppdaterParameter param, boolean aleneomsorg) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());

        var aleneomsorgForBarnetBekreftetVersjon = ytelseFordelingAggregat.getAleneomsorgAvklaring();
        var aleneomsorgForBarnetSokVersjon = ytelseFordelingAggregat.getOppgittRettighet().getHarAleneomsorgForBarnet();

        var erEndretBekreftetVersjon = harAvklartUdefinertEllerEndretBekreftet(aleneomsorgForBarnetBekreftetVersjon, aleneomsorg);
        var avkreftetBrukersOpplysinger = harAvklartUdefinertEllerEndretBekreftet(aleneomsorgForBarnetSokVersjon, aleneomsorg);

        return avkreftetBrukersOpplysinger || aleneomsorgForBarnetBekreftetVersjon != null && erEndretBekreftetVersjon;
    }

    public List<HistorikkinnslagLinjeBuilder> annenforelderRettHistorikkLinjer(AksjonspunktOppdaterParameter param,
                                                                               boolean annenforelderHarRett,
                                                                               Boolean annenforelderMottarUføretrygd,
                                                                               Boolean annenForelderHarRettEØS) {
        var ytelsefordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());
        var endretVurderingAvMorsUføretrygd = endretVurderingAvMorsUføretrygd(annenforelderMottarUføretrygd,
            ytelsefordelingAggregat.getMorUføretrygdAvklaring());
        var endretVurderingAvRettEØS = endretVurderingAvRettEØS(annenForelderHarRettEØS, ytelsefordelingAggregat.getAnnenForelderRettEØSAvklaring());
        var harAnnenForeldreRettBekreftetVersjon = ytelsefordelingAggregat.getAnnenForelderRettAvklaring();

        List<HistorikkinnslagLinjeBuilder> linjer = new ArrayList<>();

        if (!Objects.equals(konvertBooleanTilVerdiForAnnenforelderHarRett(harAnnenForeldreRettBekreftetVersjon),
            konvertBooleanTilVerdiForAnnenforelderHarRett(annenforelderHarRett))) {
            linjer.add(new HistorikkinnslagLinjeBuilder().fraTil("Rett til foreldrepenger",
                konvertBooleanTilVerdiForAnnenforelderHarRett(harAnnenForeldreRettBekreftetVersjon),
                konvertBooleanTilVerdiForAnnenforelderHarRett(annenforelderHarRett)));
        }
        if (endretVurderingAvMorsUføretrygd) {
            linjer.add(new HistorikkinnslagLinjeBuilder().fraTil("Mor mottar uføretrygd", ytelsefordelingAggregat.getMorUføretrygdAvklaring(),
                annenforelderMottarUføretrygd));
        }
        if (endretVurderingAvRettEØS) {
            linjer.add(new HistorikkinnslagLinjeBuilder().fraTil("Annen forelder har opptjent rett fra land i EØS",
                ytelsefordelingAggregat.getAnnenForelderRettEØSAvklaring(), annenForelderHarRettEØS));
        }
        return linjer;
    }

    public Optional<HistorikkinnslagLinjeBuilder> aleneomsorgHistorikkLinje(AksjonspunktOppdaterParameter param, boolean aleneomsorg) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());

        var aleneomsorgForBarnetBekreftetVersjon = ytelseFordelingAggregat.getAleneomsorgAvklaring();

        var fra = konvertBooleanTilVerdiForAleneomsorgForBarnet(aleneomsorgForBarnetBekreftetVersjon);
        var til = konvertBooleanTilVerdiForAleneomsorgForBarnet(aleneomsorg);

        if (!Objects.equals(fra, til)) {
            return Optional.of(new HistorikkinnslagLinjeBuilder().fraTil("Aleneomsorg", fra, til));
        }
        return Optional.empty();
    }

    public Optional<HistorikkinnslagLinjeBuilder> omsorgRettHistorikkLinje(AksjonspunktOppdaterParameter param, String begrunnelse) {
        if (param.erBegrunnelseEndret()) {
            return Optional.of(new HistorikkinnslagLinjeBuilder().tekst(begrunnelse));
        }
        return Optional.empty();
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

    public void oppdaterAleneomsorg(AksjonspunktOppdaterParameter param, boolean aleneomsorg) {
        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForAleneomsorg(param.getBehandlingId(), aleneomsorg);
        if (aleneomsorg) {
            fagsakEgenskapRepository.fjernFagsakMarkering(param.getRef().fagsakId(), FagsakMarkering.BARE_FAR_RETT);
        }
    }


    private String konvertBooleanTilVerdiForAleneomsorgForBarnet(Boolean aleneomsorgForBarnet) {
        if (aleneomsorgForBarnet == null) {
            return null;
        }
        return aleneomsorgForBarnet ? "Søker har aleneomsorg for barnet" : "Søker har ikke aleneomsorg for barnet";
    }

    private String konvertBooleanTilVerdiForAnnenforelderHarRett(Boolean annenforelderHarRett) {
        if (annenforelderHarRett == null) {
            return null;
        }
        return annenforelderHarRett ? "Annen forelder har rett" : "Annen forelder har ikke rett";
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
