package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUtenOmsorgEntitet;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.familiehendelse.rest.BekreftFaktaForOmsorgVurderingDto;
import no.nav.foreldrepenger.familiehendelse.rest.PeriodeDto;
import no.nav.foreldrepenger.familiehendelse.rest.PeriodeKonverter;
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
        var perioderUtenOmsorg = ytelseFordelingAggregat.getPerioderUtenOmsorg();
        List<PeriodeDto> periodeUtenOmsorgListe = new ArrayList<>();

        var harOmsorgForBarnetSokVersjon = ytelseFordelingAggregat
            .getOppgittRettighet().getHarOmsorgForBarnetIHelePerioden();

        Boolean harOmsorgForBarnetBekreftetVersjon = null;
        if (perioderUtenOmsorg.isPresent()) {
            harOmsorgForBarnetBekreftetVersjon = perioderUtenOmsorg.get().getPerioder().isEmpty();
            periodeUtenOmsorgListe = PeriodeKonverter.mapUtenOmsorgperioder(perioderUtenOmsorg.get().getPerioder());
        }

        var erEndret = opprettHistorikkInnslagForOmsorg(dto, periodeUtenOmsorgListe, harOmsorgForBarnetBekreftetVersjon);

        var avkreftet = avkrefterBrukersOpplysninger(harOmsorgForBarnetSokVersjon, dto.getOmsorg());

        var totrinn = setToTrinns(perioderUtenOmsorg, erEndret, avkreftet);

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.FAKTA_FOR_OMSORG);

        ytelseFordelingTjeneste.aksjonspunktBekreftFaktaForOmsorg(behandlingId, dto.getOmsorg(), PeriodeKonverter.mapIkkeOmsorgsperioder(dto.getIkkeOmsorgPerioder(), dto.getOmsorg()));

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private boolean opprettHistorikkInnslagForOmsorg(BekreftFaktaForOmsorgVurderingDto dto, List<PeriodeDto> periodeUtenOmsorgListe,
                                                     Boolean harOmsorgForBarnetBekreftetVersjon) {
        boolean erEndretTemp;

        if (Boolean.FALSE.equals(dto.getOmsorg())) {
            if (!periodeUtenOmsorgListe.isEmpty()) {
                erEndretTemp = oppdaterVedEndretVerdi(HistorikkEndretFeltType.IKKE_OMSORG_PERIODEN,
                    PeriodeKonverter.konvertPerioderTilString(periodeUtenOmsorgListe), PeriodeKonverter.konvertPerioderTilString(dto.getIkkeOmsorgPerioder()),
                    null);
            } else {
                erEndretTemp = oppdaterVedEndretVerdi(HistorikkEndretFeltType.OMSORG,
                    konverterBooleanTilVerdiForOmsorgForBarnet(harOmsorgForBarnetBekreftetVersjon),
                    konverterBooleanTilVerdiForOmsorgForBarnet(dto.getOmsorg()), PeriodeKonverter.konvertPerioderTilString(dto.getIkkeOmsorgPerioder()));
            }
        } else {
            erEndretTemp = oppdaterVedEndretVerdi(HistorikkEndretFeltType.OMSORG,
                konverterBooleanTilVerdiForOmsorgForBarnet(harOmsorgForBarnetBekreftetVersjon),
                konverterBooleanTilVerdiForOmsorgForBarnet(dto.getOmsorg()), null);
        }
        return erEndretTemp;
    }

    private boolean setToTrinns(Optional<PerioderUtenOmsorgEntitet> perioderUtenOmsorg, boolean erEndret, boolean avkreftet) {
        // Totrinns er sett hvis saksbehandler avkreftet først gang eller endret etter han bekreftet
        return avkreftet || (erEndret && perioderUtenOmsorg.isPresent());
    }

    private String konverterBooleanTilVerdiForOmsorgForBarnet(Boolean omsorgForBarnet) {
        if (omsorgForBarnet == null) {
            return null;
        }
        // TODO PFP-8740 midlertidig løsning. Inntil en løsning for å støtte dette
        return omsorgForBarnet ? "Søker har omsorg for barnet" : "Søker har ikke omsorg for barnet";
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, String original, String bekreftet, String perioder) {
        if (!Objects.equals(bekreftet, original)) {
            if (perioder == null) {
                historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            } else {
                // TODO PFP-8740 midlertidig løsning. Inntil en løsning for å støtte dette
                historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet.toString().concat(" i perioden ").concat(perioder));
            }
            return true;
        }
        return false;
    }

    private boolean avkrefterBrukersOpplysninger(Object original, Object bekreftet) {
        return !Objects.equals(bekreftet, original);
    }
}
