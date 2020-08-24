package no.nav.foreldrepenger.behandling.steg.beregnytelse.fp;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.AksjonspunktutlederTilbaketrekk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.List;
import java.util.Optional;

@BehandlingStegRef(kode = "VURDER_TILBAKETREKK")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef("FP")
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class VurderTilbaketrekkSteg implements BehandlingSteg {

    private AksjonspunktutlederTilbaketrekk aksjonspunktutlederTilbaketrekk;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    VurderTilbaketrekkSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderTilbaketrekkSteg(AksjonspunktutlederTilbaketrekk aksjonspunktutlederTilbaketrekk,
                                  BehandlingRepository behandlingRepository,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.aksjonspunktutlederTilbaketrekk = aksjonspunktutlederTilbaketrekk;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        Optional<Aksjonspunkt> apForVurderRefusjon = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_REFUSJON_BERGRUNN);
        boolean harVurdertStartdatoForTilkomneRefusjonskrav = apForVurderRefusjon.map(Aksjonspunkt::erUtført).orElse(false);
        if (harVurdertStartdatoForTilkomneRefusjonskrav) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        List<AksjonspunktResultat> aksjonspunkter = aksjonspunktutlederTilbaketrekk.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
        }
}
