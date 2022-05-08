package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.aksjonspunkt;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.domene.uttak.fakta.KontrollerFaktaUttakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakHistorikkTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.FaktaUttakToTrinnsTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.KontrollerOppgittFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FaktaUttakDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FaktaUttakDto.class, adapter = AksjonspunktOppdaterer.class)
public class FaktaUttakOppdaterer implements AksjonspunktOppdaterer<FaktaUttakDto> {

    private KontrollerFaktaUttakTjeneste kontrollerFaktaUttakTjeneste;
    private KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste;
    private FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste;
    private UttakInputTjeneste uttakInputtjeneste;

    FaktaUttakOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public FaktaUttakOppdaterer(UttakInputTjeneste uttakInputTjeneste,
                                KontrollerFaktaUttakTjeneste kontrollerFaktaUttakTjeneste,
                                KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste,
                                FaktaUttakHistorikkTjeneste faktaUttakHistorikkTjeneste) {
        this.uttakInputtjeneste = uttakInputTjeneste;
        this.kontrollerFaktaUttakTjeneste = kontrollerFaktaUttakTjeneste;
        this.kontrollerOppgittFordelingTjeneste = kontrollerOppgittFordelingTjeneste;
        this.faktaUttakHistorikkTjeneste = faktaUttakHistorikkTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FaktaUttakDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = param.getBehandling();
        var resultatBuilder = OppdateringResultat.utenTransisjon();
        avbrytÅpneOverstyringAksjonspunkt(behandling, resultatBuilder);

        kontrollerOppgittFordelingTjeneste.bekreftOppgittePerioder(dto.getBekreftedePerioder(), behandling);
        faktaUttakHistorikkTjeneste.byggHistorikkinnslag(dto.getBekreftedePerioder(), dto.getSlettedePerioder(), behandling, false);

        var totrinn = FaktaUttakToTrinnsTjeneste.oppdaterTotrinnskontrollVedEndringerFaktaUttak(dto);
        resultatBuilder.medTotrinnHvis(totrinn);

        if (skalBeholdeAksjonspunktÅpent(behandling, dto)) {
            resultatBuilder.medBeholdAksjonspunktÅpent();
        }
        return resultatBuilder.build();
    }

    private boolean skalBeholdeAksjonspunktÅpent(Behandling behandling, FaktaUttakDto dto) {
        var input = uttakInputtjeneste.lagInput(behandling);
        var aksjonspunktReutledet = kontrollerFaktaUttakTjeneste.reutledAksjonspunkterVedOppdateringAvYtelseFordeling(input);
        return aksjonspunktReutledet.stream()
            .anyMatch(apDef -> apDef.equals(dto.getAksjonspunktDefinisjon()));
    }

    private void avbrytÅpneOverstyringAksjonspunkt(Behandling behandling, OppdateringResultat.Builder resultatBuilder) {
        //fjern manuell avklar fakta 6070 siden trenger ikke ha både 5070 og 6070 på en behandling
        åpentSaksbehandlerOverstyringAksjonspunkt(behandling)
            .ifPresent(ap -> resultatBuilder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        //avbryt overstyring aksjonspunkt 6013
        åpentOverstyringAksjonspunkt(behandling)
            .ifPresent(ap -> resultatBuilder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
    }

    private Optional<Aksjonspunkt> åpentSaksbehandlerOverstyringAksjonspunkt(Behandling behandling) {
        return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING);
    }

    private Optional<Aksjonspunkt> åpentOverstyringAksjonspunkt(Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.OVERSTYRING_AV_FAKTA_UTTAK);
    }

}
