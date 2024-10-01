package no.nav.foreldrepenger.behandling.steg.startuttak.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandling.steg.startuttak.InngangUttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@BehandlingStegRef(BehandlingStegType.INNGANG_UTTAK)
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD) // Førstegangssøknad
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) // Foreldrepenger
@ApplicationScoped
public class InngangUttakStegFørstegangsøknad implements InngangUttakSteg {

    private static final Logger LOG = LoggerFactory.getLogger(InngangUttakStegFørstegangsøknad.class);

    private BehandlingFlytkontroll flytkontroll;
    private BehandlingsresultatRepository behandlingsresultatRepository;


    InngangUttakStegFørstegangsøknad() {
        // CDI
    }

    @Inject
    public InngangUttakStegFørstegangsøknad(BehandlingFlytkontroll flytkontroll,
                                            BehandlingsresultatRepository behandlingsresultatRepository) {
        this.flytkontroll = flytkontroll;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var avslag = behandlingsresultatRepository.hentHvisEksisterer(kontekst.getBehandlingId())
            .map(Behandlingsresultat::getVilkårResultat)
            .map(VilkårResultat::getVilkårene).orElse(List.of()).stream()
            .anyMatch(Vilkår::erIkkeOppfylt);
        if (!avslag && flytkontroll.uttaksProsessenSkalVente(kontekst.getBehandlingId())) {
            LOG.info("Flytkontroll UTTAK: Setter behandling {} førstegang på vent grunnet annen part", kontekst.getBehandlingId());
            var køAutopunkt = AksjonspunktResultat.opprettForAksjonspunktMedFrist(AUTO_KØET_BEHANDLING, Venteårsak.VENT_ÅPEN_BEHANDLING, null);
            return BehandleStegResultat.utførtMedAksjonspunktResultat(køAutopunkt);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
