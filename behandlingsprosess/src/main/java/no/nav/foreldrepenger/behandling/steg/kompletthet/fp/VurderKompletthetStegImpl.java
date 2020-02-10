package no.nav.foreldrepenger.behandling.steg.kompletthet.fp;

import static no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetStegFelles.autopunktAlleredeUtført;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetSteg;
import no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.kompletthet.KompletthetResultat;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "VURDERKOMPLETT")
@BehandlingTypeRef("BT-002")
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class VurderKompletthetStegImpl implements VurderKompletthetSteg {

    private Kompletthetsjekker kompletthetsjekker;
    private BehandlingRepository behandlingRepository;
    private VurderKompletthetStegFelles vurderKompletthetStegFelles;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    VurderKompletthetStegImpl() {
    }

    @Inject
    public VurderKompletthetStegImpl(@FagsakYtelseTypeRef("FP") @BehandlingTypeRef("BT-002") Kompletthetsjekker kompletthetsjekker,
                                       BehandlingRepositoryProvider provider,
                                       VurderKompletthetStegFelles vurderKompletthetStegFelles,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.kompletthetsjekker = kompletthetsjekker;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.vurderKompletthetStegFelles = vurderKompletthetStegFelles;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        KompletthetResultat søknadMottatt = kompletthetsjekker.vurderSøknadMottattForTidlig(ref);
        if (!søknadMottatt.erOppfylt()) {
            return vurderKompletthetStegFelles.evaluerUoppfylt(søknadMottatt, VENT_PGA_FOR_TIDLIG_SØKNAD);
        }

        KompletthetResultat forsendelseMottatt = kompletthetsjekker.vurderForsendelseKomplett(ref);
        if (!forsendelseMottatt.erOppfylt() && !autopunktAlleredeUtført(AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, behandling)) {
            return vurderKompletthetStegFelles.evaluerUoppfylt(forsendelseMottatt, AUTO_VENTER_PÅ_KOMPLETT_SØKNAD);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
