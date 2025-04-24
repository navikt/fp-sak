package no.nav.foreldrepenger.behandlingskontroll.impl.observer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg.TransisjonType;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingTransisjonEvent;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.StegTransisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;

/**
 * Håndtere opprydding i Aksjonspunkt og Vilkår ved overhopp framover
 */
@ApplicationScoped
public class BehandlingskontrollFremoverhoppTransisjonEventObserver {

    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    public BehandlingskontrollFremoverhoppTransisjonEventObserver(BehandlingskontrollServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    protected BehandlingskontrollFremoverhoppTransisjonEventObserver() {
        super();
        // for CDI proxy
    }

    public void observerBehandlingSteg(@Observes BehandlingTransisjonEvent transisjonEvent) {
        if (!StegTransisjon.HOPPOVER.equals(transisjonEvent.getStegTransisjon())) {
            return;
        }
        //         if (!FellesTransisjoner.erFremhoppTransisjon(transisjonEvent.getTransisjonIdentifikator()) || !transisjonEvent.erOverhopp()) {
        //            return;
        //        }
        var behandling = serviceProvider.hentBehandling(transisjonEvent.getBehandlingId());
        var modell = getModell(behandling);

        var førsteSteg = transisjonEvent.getFørsteSteg();
        var sisteSteg = transisjonEvent.getSisteSteg();
        var førsteStegStatus = transisjonEvent.getFørsteStegStatus();

        var medInngangFørsteSteg = førsteStegStatus.isEmpty() || førsteStegStatus.get().erVedInngang();

        var aksjonspunktDefinisjonerEtterFra = modell.finnAksjonspunktDefinisjonerFraOgMed(førsteSteg, medInngangFørsteSteg);
        var aksjonspunktDefinisjonerEtterTil = modell.finnAksjonspunktDefinisjonerFraOgMed(sisteSteg, true);

        Set<AksjonspunktDefinisjon> mellomliggende = new HashSet<>(aksjonspunktDefinisjonerEtterFra);
        mellomliggende.removeAll(aksjonspunktDefinisjonerEtterTil);

        List<Aksjonspunkt> avbrutte = new ArrayList<>();
        behandling.getAksjonspunkter().stream()
                .filter(a -> mellomliggende.contains(a.getAksjonspunktDefinisjon()))
                .filter(Aksjonspunkt::erÅpentAksjonspunkt)
                .forEach(a -> {
                    avbrytAksjonspunkt(a);
                    avbrutte.add(a);
                });

        if (skalBesøkeStegene(transisjonEvent.getStegTransisjon())) {
            if (!medInngangFørsteSteg) {
                // juster til neste steg dersom vi står ved utgang av steget.
                førsteSteg = modell.finnNesteSteg(førsteSteg).getBehandlingStegType();
            }

            var finalFørsteSteg = førsteSteg;
            modell.hvertStegFraOgMedTil(førsteSteg, sisteSteg, false)
                    .forEach(s -> hoppFramover(s, transisjonEvent, sisteSteg, finalFørsteSteg));

        }
        // Lagre oppdateringer; eventhåndteringen skal være autonom og selv ferdigstille
        // oppdateringer på behandlingen
        lagre(transisjonEvent, behandling);
        if (!avbrutte.isEmpty() && serviceProvider.getEventPubliserer() != null) {
            serviceProvider.getEventPubliserer().fireEvent(new AksjonspunktStatusEvent(transisjonEvent.getKontekst(), avbrutte, førsteSteg));
        }

    }

    protected void lagre(BehandlingTransisjonEvent transisjonEvent, Behandling behandling) {
        serviceProvider.getBehandlingRepository().lagre(behandling, transisjonEvent.getKontekst().getSkriveLås());
    }

    protected void avbrytAksjonspunkt(Aksjonspunkt a) {
        serviceProvider.getAksjonspunktKontrollRepository().setTilAvbrutt(a);
    }

    protected void hoppFramover(BehandlingStegModell stegModell, BehandlingTransisjonEvent transisjonEvent, BehandlingStegType sisteSteg,
            final BehandlingStegType finalFørsteSteg) {
        stegModell.getSteg().vedTransisjon(transisjonEvent.getKontekst(), stegModell, TransisjonType.HOPP_OVER_FRAMOVER, finalFørsteSteg, sisteSteg);
    }

    protected BehandlingModell getModell(Behandling behandling) {
        return serviceProvider.getBehandlingModellRepository().getModell(behandling.getType(), behandling.getFagsakYtelseType());
    }

    private boolean skalBesøkeStegene(StegTransisjon transisjon) {
        return !StegTransisjon.FLYOVER.equals(transisjon);
    }

}
