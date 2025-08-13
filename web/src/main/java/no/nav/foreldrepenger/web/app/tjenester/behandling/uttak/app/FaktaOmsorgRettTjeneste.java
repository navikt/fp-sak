package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
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
    public FaktaOmsorgRettTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste, FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    public boolean totrinnForRettighetsavklaring(AksjonspunktOppdaterParameter param, Rettighetstype rettighetstype) {
        var gjeldendeRettighetstype = finnRettighetstypeFørEndring(param);
        return !rettighetstype.equals(gjeldendeRettighetstype);
    }

    public List<HistorikkinnslagLinjeBuilder> annenForelderRettAvklaringHistorikkLinjer(Rettighetstype nyRettighetstype,
                                                                                        String begrunnelse) {
        List<HistorikkinnslagLinjeBuilder> linjer = new ArrayList<>(rettHistorikkLinjer(nyRettighetstype));
        linjer.add(new HistorikkinnslagLinjeBuilder().tekst(begrunnelse));
        return linjer;
    }

    public List<HistorikkinnslagLinjeBuilder> aleneomsorgAvklaringHistorikkLinjer(Rettighetstype nyRettighetstype,
                                                                                  String begrunnelse) {
        List<HistorikkinnslagLinjeBuilder> linjer = new ArrayList<>();
        var til = konvertBooleanTilVerdiForAleneomsorgForBarnet(nyRettighetstype.equals(Rettighetstype.ALENEOMSORG));
        linjer.add(new HistorikkinnslagLinjeBuilder().bold(til));

        if (!nyRettighetstype.equals(Rettighetstype.ALENEOMSORG)) {
            linjer.addAll(rettHistorikkLinjer(nyRettighetstype));
        }
        linjer.add(new HistorikkinnslagLinjeBuilder().tekst(begrunnelse));
        return linjer;
    }

    public void avklarRettighet(AksjonspunktOppdaterParameter param, Rettighetstype rettighetstype) {
        ytelseFordelingTjeneste.avklarRettighet(param.getBehandlingId(), rettighetstype);
        if (rettighetstype.equals(Rettighetstype.BARE_FAR_RETT)) {
            fagsakEgenskapRepository.leggTilFagsakMarkering(param.getRef().fagsakId(), FagsakMarkering.BARE_FAR_RETT);
        } else {
            fagsakEgenskapRepository.fjernFagsakMarkering(param.getRef().fagsakId(), FagsakMarkering.BARE_FAR_RETT);
        }
    }

    public void overstyrRettighet(BehandlingReferanse ref, Rettighetstype nyOverstyrtRettighet) {
        ytelseFordelingTjeneste.overstyrRettighet(ref.behandlingId(), nyOverstyrtRettighet);
        if (Set.of(Rettighetstype.BARE_FAR_RETT, Rettighetstype.BARE_FAR_RETT_MOR_UFØR).contains(nyOverstyrtRettighet)) {
            fagsakEgenskapRepository.leggTilFagsakMarkering(ref.fagsakId(), FagsakMarkering.BARE_FAR_RETT);
        } else {
            fagsakEgenskapRepository.fjernFagsakMarkering(ref.fagsakId(), FagsakMarkering.BARE_FAR_RETT);
        }
    }

    private List<HistorikkinnslagLinjeBuilder> rettHistorikkLinjer(Rettighetstype nyRettighetstype) {
        var vurdertUfør = nyRettighetstype.equals(Rettighetstype.BARE_FAR_RETT_MOR_UFØR) || nyRettighetstype.equals(Rettighetstype.BARE_FAR_RETT)
            || nyRettighetstype.equals(Rettighetstype.BARE_MOR_RETT);
        var vurdertEøs = nyRettighetstype.equals(Rettighetstype.BEGGE_RETT_EØS) || vurdertUfør; //Hvis ufør er endret så er eøs vurdert først
        List<HistorikkinnslagLinjeBuilder> linjer = new ArrayList<>();
        var til = konvertBooleanTilVerdiForAnnenforelderHarRett(nyRettighetstype.equals(Rettighetstype.BEGGE_RETT));
        linjer.add(new HistorikkinnslagLinjeBuilder().bold(til));

        if (vurdertEøs) {
            linjer.add(new HistorikkinnslagLinjeBuilder().til("Annen forelder har opptjent rett fra land i EØS",
                nyRettighetstype.equals(Rettighetstype.BEGGE_RETT_EØS)));
        }
        if (vurdertUfør) {
            linjer.add(
                new HistorikkinnslagLinjeBuilder().til("Mor mottar uføretrygd", nyRettighetstype.equals(Rettighetstype.BARE_FAR_RETT_MOR_UFØR)));
        }
        return linjer;
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

    private Rettighetstype finnRettighetstypeFørEndring(AksjonspunktOppdaterParameter param) {
        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(param.getBehandlingId());
        var relasjonsRolleType = param.getRef().relasjonRolle();
        return ytelseFordelingAggregat.getOverstyrtRettighetstype()
            .or(() -> ytelseFordelingAggregat.getAvklartRettighet().map(r -> tilRettighetstype(r, relasjonsRolleType)))
            .orElseGet(() -> tilRettighetstype(ytelseFordelingAggregat.getOppgittRettighet(), relasjonsRolleType));
    }

    private Rettighetstype tilRettighetstype(OppgittRettighetEntitet rettighet, RelasjonsRolleType relasjonsRolleType) {
        if (rettighet.getHarAleneomsorgForBarnet()) {
            return Rettighetstype.ALENEOMSORG;
        }
        if (rettighet.getHarAnnenForeldreRett()) {
            return Rettighetstype.BEGGE_RETT;
        }
        if (rettighet.getMorMottarUføretrygd()) {
            return Rettighetstype.BARE_FAR_RETT_MOR_UFØR;
        }
        if (rettighet.getAnnenForelderRettEØS()) {
            return Rettighetstype.BEGGE_RETT_EØS;
        }
        return relasjonsRolleType.erFarEllerMedMor() ? Rettighetstype.BARE_FAR_RETT : Rettighetstype.BARE_MOR_RETT;
    }
}
