package no.nav.foreldrepenger.behandling.steg.kompletthet.svp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetSteg;
import no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetStegFelles;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD;

@BehandlingStegRef(BehandlingStegType.VURDER_KOMPLETTHET)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class VurderKompletthetStegImpl implements VurderKompletthetSteg {
    private Kompletthetsjekker kompletthetsjekker;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public VurderKompletthetStegImpl() {
        // CDI
    }

    @Inject
    public VurderKompletthetStegImpl(@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) Kompletthetsjekker kompletthetsjekker, BehandlingRepository behandlingRepository,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.kompletthetsjekker = kompletthetsjekker;
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        if (skalPassereKompletthet(behandling) || kanPassereKompletthet(behandling)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var søknadMottatt = kompletthetsjekker.vurderSøknadMottattForTidlig(ref);
        if (!søknadMottatt.erOppfylt()) {
            return VurderKompletthetStegFelles.evaluerUoppfylt(søknadMottatt, VENT_PGA_FOR_TIDLIG_SØKNAD);
        }

        var forsendelseMottatt = kompletthetsjekker.vurderForsendelseKomplett(ref);
        if (!forsendelseMottatt.erOppfylt() && !VurderKompletthetStegFelles.autopunktAlleredeUtført(AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, behandling)) {
            return VurderKompletthetStegFelles.evaluerUoppfylt(forsendelseMottatt, AUTO_VENTER_PÅ_KOMPLETT_SØKNAD);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
