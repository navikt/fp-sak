package no.nav.foreldrepenger.behandling.steg.medlemskap.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.skjæringstidspunkt.UtsettelseBehandling2021;

@BehandlingStegRef(kode = "KOFAK_LOP_MEDL")
@BehandlingTypeRef("BT-002") // Førstegangssøknad
@FagsakYtelseTypeRef("FP") // Foreldrepenger
@ApplicationScoped
public class KontrollerFaktaLøpendeMedlemskapStegFørstegangsøknad implements KontrollerFaktaLøpendeMedlemskapSteg {

    private static final Logger LOG = LoggerFactory.getLogger(KontrollerFaktaLøpendeMedlemskapStegFørstegangsøknad.class);

    private static boolean PROD = Environment.current().isProd();

    private static LocalDateTime FRI_UTTAK_UTSETT_BEHANDLING_TIL = LocalDate.of(2021, 10, 5).atStartOfDay();

    private BehandlingFlytkontroll flytkontroll;
    private UtsettelseBehandling2021 utsettelseBehandling2021;


    KontrollerFaktaLøpendeMedlemskapStegFørstegangsøknad() {
        // CDI
    }

    @Inject
    public KontrollerFaktaLøpendeMedlemskapStegFørstegangsøknad(BehandlingFlytkontroll flytkontroll, UtsettelseBehandling2021 utsettelseBehandling2021) {
        this.flytkontroll = flytkontroll;
        this.utsettelseBehandling2021 = utsettelseBehandling2021;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (PROD && LocalDateTime.now().isBefore(FRI_UTTAK_UTSETT_BEHANDLING_TIL)
            && !utsettelseBehandling2021.kreverSammenhengendeUttak(kontekst.getBehandlingId())
            && utsettelseBehandling2021.usikkertFrittUttak(kontekst.getBehandlingId())) {
            LOG.info("FRITT UTTAK: Setter behandling {} førstegang på vent til 5/10", kontekst.getBehandlingId());
            var køAutopunkt = AksjonspunktResultat.opprettForAksjonspunktMedFrist(AUTO_KØET_BEHANDLING, Venteårsak.AVV_FODSEL, FRI_UTTAK_UTSETT_BEHANDLING_TIL);
            return BehandleStegResultat.utførtMedAksjonspunktResultater(List.of(køAutopunkt));
        }
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
