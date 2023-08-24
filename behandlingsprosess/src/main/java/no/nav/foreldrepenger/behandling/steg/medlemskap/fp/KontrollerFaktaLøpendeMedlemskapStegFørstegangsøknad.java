package no.nav.foreldrepenger.behandling.steg.medlemskap.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandling.steg.medlemskap.KontrollerFaktaLøpendeMedlemskapSteg;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP)
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD) // Førstegangssøknad
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) // Foreldrepenger
@ApplicationScoped
public class KontrollerFaktaLøpendeMedlemskapStegFørstegangsøknad implements KontrollerFaktaLøpendeMedlemskapSteg {

    private static final Logger LOG = LoggerFactory.getLogger(KontrollerFaktaLøpendeMedlemskapStegFørstegangsøknad.class);

    private BehandlingFlytkontroll flytkontroll;


    KontrollerFaktaLøpendeMedlemskapStegFørstegangsøknad() {
        // CDI
    }

    @Inject
    public KontrollerFaktaLøpendeMedlemskapStegFørstegangsøknad(BehandlingFlytkontroll flytkontroll) {
        this.flytkontroll = flytkontroll;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (flytkontroll.uttaksProsessenSkalVente(kontekst.getBehandlingId())) {
            LOG.info("Flytkontroll UTTAK: Setter behandling {} førstegang på vent grunnet annen part", kontekst.getBehandlingId());
            var køAutopunkt = AksjonspunktResultat.opprettForAksjonspunktMedFrist(AUTO_KØET_BEHANDLING, Venteårsak.VENT_ÅPEN_BEHANDLING, null);
            return BehandleStegResultat.utførtMedAksjonspunktResultat(køAutopunkt);
        }
        // kan utvides ved behov for sjekk av løpende medlemskap.
        // er tomt nå fordi startpunktutlederen peker på KOFAK_LOP_MEDL for uttak.
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
