package no.nav.foreldrepenger.behandling.steg.avklarfakta;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.KontrollerFaktaInngangsVilkårUtleder;

public abstract class KontrollerFaktaTjenesteInngangsVilkår implements KontrollerFaktaInngangsVilkårUtleder {

    private static final Logger logger = LoggerFactory.getLogger(KontrollerFaktaTjenesteInngangsVilkår.class);

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
    public List<AksjonspunktResultat> utledAksjonspunkter(BehandlingReferanse ref) {
        return utled(ref);
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunktType) {
        List<AksjonspunktResultat> aksjonspunktResultat = utledAksjonspunkter(ref);
        return filtrerAksjonspunkterTilVenstreForStartpunkt(ref, aksjonspunktResultat, startpunktType);
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFomSteg(BehandlingReferanse ref, BehandlingStegType steg) {
        return utledAksjonspunkter(ref).stream()
            .filter(ap -> skalBeholdeAksjonspunkt(ref, steg, ap.getAksjonspunktDefinisjon()))
            .collect(Collectors.toList());
    }

    @Override
    public boolean skalOverstyringLøsesTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunktType, AksjonspunktDefinisjon apDef) {
        return behandlingskontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(
            ref.getFagsakYtelseType(), ref.getBehandlingType(), startpunktType.getBehandlingSteg(), apDef);
    }

    private List<AksjonspunktResultat> filtrerAksjonspunkterTilVenstreForStartpunkt(BehandlingReferanse referanse, List<AksjonspunktResultat> aksjonspunktResultat,
                                                                                    StartpunktType startpunkt) {
        // Fjerner aksjonspunkter som ikke skal løses i eller etter steget som følger av startpunktet:
        return aksjonspunktResultat.stream()
            .filter(ap -> skalBeholdeAksjonspunkt(referanse, startpunkt.getBehandlingSteg(), ap.getAksjonspunktDefinisjon()))
            .collect(Collectors.toList());
    }

    private boolean skalBeholdeAksjonspunkt(BehandlingReferanse ref, BehandlingStegType steg, AksjonspunktDefinisjon apDef) {
        boolean skalBeholde = behandlingskontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(
            ref.getFagsakYtelseType(), ref.getBehandlingType(), steg, apDef);
        if (!skalBeholde) {
            logger.debug("Fjerner aksjonspunkt {} da det skal løses før startsteg {}.",
                apDef.getKode(), steg.getKode()); // NOSONAR
        }
        return skalBeholde;
    }

    private List<AksjonspunktResultat> utled(BehandlingReferanse ref) {
        final List<AksjonspunktUtleder> aksjonspunktUtleders = utlederTjeneste.utledUtledereFor(ref);
        List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();
        for (AksjonspunktUtleder aksjonspunktUtleder : aksjonspunktUtleders) {
            aksjonspunktResultater.addAll(aksjonspunktUtleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref)));
        }
        return aksjonspunktResultater.stream()
            .distinct() // Unngå samme aksjonspunkt flere multipliser
            .collect(toList());
    }
}
