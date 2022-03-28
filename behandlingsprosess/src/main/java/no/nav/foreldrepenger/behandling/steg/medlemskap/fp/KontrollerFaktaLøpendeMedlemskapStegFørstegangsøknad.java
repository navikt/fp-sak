package no.nav.foreldrepenger.behandling.steg.medlemskap.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandling.steg.medlemskap.KontrollerFaktaLøpendeMedlemskapSteg;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegKoder;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;

@BehandlingStegRef(kode = BehandlingStegKoder.KONTROLLER_LØPENDE_MEDLEMSKAP_KODE)
@BehandlingTypeRef("BT-002") // Førstegangssøknad
@FagsakYtelseTypeRef("FP") // Foreldrepenger
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
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(køAutopunkt));
        }
        // kan utvides ved behov for sjekk av løpende medlemskap.
        // er tomt nå fordi startpunktutlederen peker på KOFAK_LOP_MEDL for uttak.
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
