package no.nav.foreldrepenger.behandling.steg.startuttak.fp;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.flytkontroll.BehandlingFlytkontroll;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandling.steg.startuttak.InngangUttakSteg;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.SkalKopiereUttakTjeneste;

@BehandlingStegRef(BehandlingStegType.INNGANG_UTTAK)
@BehandlingTypeRef(BehandlingType.REVURDERING) // Revurdering
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) // Foreldrepenger
@ApplicationScoped
public class InngangUttakStegRevurdering implements InngangUttakSteg {

    private static final Logger LOG = LoggerFactory.getLogger(InngangUttakStegRevurdering.class);

    private BehandlingFlytkontroll flytkontroll;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SkalKopiereUttakTjeneste skalKopiereUttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    @Inject
    public InngangUttakStegRevurdering(BehandlingFlytkontroll flytkontroll,
                                       BehandlingsresultatRepository behandlingsresultatRepository,
                                       SkalKopiereUttakTjeneste skalKopiereUttakTjeneste,
                                       UttakInputTjeneste uttakInputTjeneste) {
        this.flytkontroll = flytkontroll;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.skalKopiereUttakTjeneste = skalKopiereUttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    InngangUttakStegRevurdering() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var avslag = behandlingsresultatRepository.hentHvisEksisterer(behandlingId)
            .map(Behandlingsresultat::getVilkårResultat)
            .map(VilkårResultat::getVilkårene).orElse(List.of()).stream()
            .anyMatch(Vilkår::erIkkeOppfylt);
        if (avslag) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        List<AksjonspunktResultat> aksjonspunkter = new ArrayList<>();
        if (flytkontroll.uttaksProsessenSkalVente(kontekst.getBehandlingId())) {
            LOG.info("Flytkontroll UTTAK: Setter behandling {} revurdering på vent grunnet berørt eller annen part", kontekst.getBehandlingId());
            aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunktMedFrist(AUTO_KØET_BEHANDLING, Venteårsak.VENT_ÅPEN_BEHANDLING, null));
        }
        var uttakInput = uttakInputTjeneste.lagInput(behandlingId);
        if (skalKopiereUttakTjeneste.skalKopiereStegResultat(uttakInput)) {
            return BehandleStegResultat.fremoverførtMedAksjonspunktResultater(FellesTransisjoner.FREMHOPP_TIL_BEREGN_YTELSE, aksjonspunkter);
        }
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }
}
