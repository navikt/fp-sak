package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.KontrollerFaktaUttakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app.KontrollerOppgittFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.OverstyringFaktaUttakDto;

@ApplicationScoped
public class FaktaUttakOverstyringFelles {

    private KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste;
    private KontrollerFaktaUttakTjeneste kontrollerFaktaUttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public FaktaUttakOverstyringFelles(KontrollerOppgittFordelingTjeneste kontrollerOppgittFordelingTjeneste,
                                       KontrollerFaktaUttakTjeneste kontrollerFaktaUttakTjeneste,
                                       UttakInputTjeneste uttakInputTjeneste) {
        this.kontrollerOppgittFordelingTjeneste = kontrollerOppgittFordelingTjeneste;
        this.kontrollerFaktaUttakTjeneste = kontrollerFaktaUttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    FaktaUttakOverstyringFelles() {
        //CDI
    }

    public OppdateringResultat håndterOverstyring(OverstyringFaktaUttakDto dto, Behandling behandling) {
        kontrollerOppgittFordelingTjeneste.bekreftOppgittePerioder(dto.getBekreftedePerioder(), behandling);

        var builder = OppdateringResultat.utenTransisjon();
        var utledetAksjonspunkt = reutledAksjonspunkter(behandling);
        for (var def : utledetAksjonspunkt) {
            builder.medEkstraAksjonspunktResultat(def, AksjonspunktStatus.OPPRETTET);
        }
        return builder.build();
    }

    private List<AksjonspunktDefinisjon> reutledAksjonspunkter(Behandling behandling) {
        var input = uttakInputTjeneste.lagInput(behandling);
        return kontrollerFaktaUttakTjeneste.reutledAksjonspunkterVedOppdateringAvYtelseFordeling(input);
    }
}
