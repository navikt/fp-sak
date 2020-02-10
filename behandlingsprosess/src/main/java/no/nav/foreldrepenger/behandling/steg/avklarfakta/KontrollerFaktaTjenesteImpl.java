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
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.KontrollerFaktaAksjonspunktUtleder;

public abstract class KontrollerFaktaTjenesteImpl implements KontrollerFaktaAksjonspunktUtleder {

    private static final Logger logger = LoggerFactory.getLogger(KontrollerFaktaTjenesteImpl.class);

    private KontrollerFaktaUtledere utlederTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    protected KontrollerFaktaTjenesteImpl() {
        // for CDI proxy
    }

    protected KontrollerFaktaTjenesteImpl(KontrollerFaktaUtledere utlederTjeneste,
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
    public boolean skalOverstyringLøsesTilHøyreForStartpunkt(BehandlingReferanse ref, StartpunktType startpunktType, AksjonspunktDefinisjon apDef) {
        return behandlingskontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(
            ref.getFagsakYtelseType(), ref.getBehandlingType(), startpunktType.getBehandlingSteg(), apDef);
    }

    private List<AksjonspunktResultat> filtrerAksjonspunkterTilVenstreForStartpunkt(BehandlingReferanse referanse, List<AksjonspunktResultat> aksjonspunktResultat,
                                                                                    StartpunktType startpunkt) {
        // Fjerner aksjonspunkter som ikke skal løses i eller etter steget som følger av startpunktet:
        return aksjonspunktResultat.stream()
            .filter(ap -> skalBeholdeAksjonspunkt(referanse, startpunkt, ap.getAksjonspunktDefinisjon()))
            .collect(Collectors.toList());
    }

    private boolean skalBeholdeAksjonspunkt(BehandlingReferanse ref, StartpunktType startpunkt, AksjonspunktDefinisjon apDef) {
        boolean skalBeholde = behandlingskontrollTjeneste.skalAksjonspunktLøsesIEllerEtterSteg(
            ref.getFagsakYtelseType(), ref.getBehandlingType(), startpunkt.getBehandlingSteg(), apDef);
        if (!skalBeholde) {
            logger.debug("Fjerner aksjonspunkt {} da det skal løses før startsteg {}.",
                apDef.getKode(), startpunkt.getBehandlingSteg().getKode()); // NOSONAR
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
