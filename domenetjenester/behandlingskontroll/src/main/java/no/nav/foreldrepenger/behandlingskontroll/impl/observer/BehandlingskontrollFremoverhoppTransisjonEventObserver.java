package no.nav.foreldrepenger.behandlingskontroll.impl.observer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg.TransisjonType;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.events.AksjonspunktStatusEvent;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingTransisjonEvent;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;

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
        Behandling behandling = serviceProvider.hentBehandling(transisjonEvent.getBehandlingId());
        BehandlingModell modell = getModell(behandling);
        if (!(FellesTransisjoner.erFremhoppTransisjon(transisjonEvent.getTransisjonIdentifikator())) || !transisjonEvent.erOverhopp()) {
            return;
        }

        BehandlingStegType førsteSteg = transisjonEvent.getFørsteSteg();
        BehandlingStegType sisteSteg = transisjonEvent.getSisteSteg();
        Optional<BehandlingStegStatus> førsteStegStatus = transisjonEvent.getFørsteStegStatus();

        boolean medInngangFørsteSteg = førsteStegStatus.isEmpty() || førsteStegStatus.get().erVedInngang();

        Set<String> aksjonspunktDefinisjonerEtterFra = modell.finnAksjonspunktDefinisjonerFraOgMed(førsteSteg, medInngangFørsteSteg);
        Set<String> aksjonspunktDefinisjonerEtterTil = modell.finnAksjonspunktDefinisjonerFraOgMed(sisteSteg, true);

        Set<String> mellomliggende = new HashSet<>(aksjonspunktDefinisjonerEtterFra);
        mellomliggende.removeAll(aksjonspunktDefinisjonerEtterTil);

        List<Aksjonspunkt> avbrutte = new ArrayList<>();
        behandling.getAksjonspunkter().stream()
            .filter(a -> mellomliggende.contains(a.getAksjonspunktDefinisjon().getKode()))
            .filter(Aksjonspunkt::erÅpentAksjonspunkt)
            .forEach(a -> {
                avbrytAksjonspunkt(a);
                avbrutte.add(a);
            });

        if (skalBesøkeStegene(transisjonEvent.getTransisjonIdentifikator())) {
            if (!medInngangFørsteSteg) {
                // juster til neste steg dersom vi står ved utgang av steget.
                førsteSteg = modell.finnNesteSteg(førsteSteg).getBehandlingStegType();
            }

            final BehandlingStegType finalFørsteSteg = førsteSteg;
            modell.hvertStegFraOgMedTil(førsteSteg, sisteSteg, false)
                .forEach(s -> hoppFramover(s, transisjonEvent, sisteSteg, finalFørsteSteg));

        }
        // Lagre oppdateringer; eventhåndteringen skal være autonom og selv ferdigstille oppdateringer på behandlingen
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

    private boolean skalBesøkeStegene(TransisjonIdentifikator transisjon) {
        return !FellesTransisjoner.erSpolfremTransisjon(transisjon);
    }

    private void håndterAksjonspunkter(Behandling behandling, Set<String> mellomliggendeAksjonspunkt,
                                       Consumer<Aksjonspunkt> action) {

    }

}
