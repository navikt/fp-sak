package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.KontrollerFaktaInngangsVilkårUtleder;

public abstract class KontrollerFaktaTjenesteInngangsVilkår implements KontrollerFaktaInngangsVilkårUtleder {

    private static final Logger LOG = LoggerFactory.getLogger(KontrollerFaktaTjenesteInngangsVilkår.class);

    private KontrollerFaktaUtledere utlederTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    protected KontrollerFaktaTjenesteInngangsVilkår() {
        // for CDI proxy
    }

    protected KontrollerFaktaTjenesteInngangsVilkår(KontrollerFaktaUtledere utlederTjeneste,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        this.utlederTjeneste = utlederTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkter(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return utled(ref, stp);
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterTilHøyreForStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, StartpunktType startpunktType) {
        var aksjonspunktResultat = utledAksjonspunkter(ref, stp);
        return filtrerAksjonspunkterTilVenstreForStartpunkt(ref, aksjonspunktResultat, startpunktType);
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFomSteg(BehandlingReferanse ref, Skjæringstidspunkt stp, BehandlingStegType steg) {
        return utledAksjonspunkter(ref, stp).stream()
                .filter(ap -> skalBeholdeAksjonspunkt(ref, steg, ap.getAksjonspunktDefinisjon()))
                .toList();
    }

    @Override
    public boolean skalOverstyringLøsesTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunktType, AksjonspunktDefinisjon apDef) {
        return behandlingskontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(
                ref.fagsakYtelseType(), ref.behandlingType(), startpunktType.getBehandlingSteg(), apDef);
    }

    private List<AksjonspunktResultat> filtrerAksjonspunkterTilVenstreForStartpunkt(BehandlingReferanse referanse,
            List<AksjonspunktResultat> aksjonspunktResultat,
            StartpunktType startpunkt) {
        // Fjerner aksjonspunkter som ikke skal løses i eller etter steget som følger av
        // startpunktet:
        return aksjonspunktResultat.stream()
                .filter(ap -> skalBeholdeAksjonspunkt(referanse, startpunkt.getBehandlingSteg(), ap.getAksjonspunktDefinisjon()))
                .toList();
    }

    private boolean skalBeholdeAksjonspunkt(BehandlingReferanse ref, BehandlingStegType steg, AksjonspunktDefinisjon apDef) {
        var skalBeholde = behandlingskontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(
                ref.fagsakYtelseType(), ref.behandlingType(), steg, apDef);
        if (!skalBeholde) {
            LOG.debug("Fjerner aksjonspunkt {} da det skal løses før startsteg {}.",
                    apDef.getKode(), steg.getKode());
        }
        return skalBeholde;
    }

    private List<AksjonspunktResultat> utled(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var aksjonspunktUtleders = utlederTjeneste.utledUtledereFor(ref);
        List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();
        for (var aksjonspunktUtleder : aksjonspunktUtleders) {
            aksjonspunktResultater.addAll(aksjonspunktUtleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref, stp)));
        }
        return aksjonspunktResultater.stream()
                .distinct() // Unngå samme aksjonspunkt flere multipliser
                .collect(toList());
    }
}
