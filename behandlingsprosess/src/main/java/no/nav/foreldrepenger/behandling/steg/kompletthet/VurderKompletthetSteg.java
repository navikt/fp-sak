package no.nav.foreldrepenger.behandling.steg.kompletthet;

import static no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetStegFelles.kanPassereKompletthet;
import static no.nav.foreldrepenger.behandling.steg.kompletthet.VurderKompletthetStegFelles.skalPassereKompletthet;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.kompletthet.Kompletthetsjekker;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(BehandlingStegType.VURDER_KOMPLETT_BEH)
@ApplicationScoped
public class VurderKompletthetSteg implements BehandlingSteg {
    private Kompletthetsjekker kompletthetsjekker;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public VurderKompletthetSteg() {
        // CDI
    }

    @Inject
    public VurderKompletthetSteg(Kompletthetsjekker kompletthetsjekker, BehandlingRepository behandlingRepository,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.kompletthetsjekker = kompletthetsjekker;
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (skalPassereKompletthet(behandling)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType()) && kanPassereKompletthet(behandling)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        var forsendelseMottatt = kompletthetsjekker.vurderForsendelseKomplett(BehandlingReferanse.fra(behandling), skjæringstidspunkter);
        if (forsendelseMottatt.erOppfylt()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (VurderKompletthetStegFelles.autopunktAlleredeUtført(AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, behandling)) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()) && !behandling.erRevurdering()) {
            // kompletthetsresultat kan være langt fram i tid dersom tidlig fødsel
            var brukfrist = kanPassereKompletthet(behandling) && !forsendelseMottatt.erFristUtløpt()
                && forsendelseMottatt.ventefrist().isAfter(LocalDateTime.now().plusWeeks(1)) ?
                LocalDate.now().plusWeeks(1).atStartOfDay() : forsendelseMottatt.ventefrist();
            return VurderKompletthetStegFelles.evaluerUoppfylt(forsendelseMottatt, brukfrist, AUTO_VENTER_PÅ_KOMPLETT_SØKNAD);
        }

        return VurderKompletthetStegFelles.evaluerUoppfylt(forsendelseMottatt, AUTO_VENTER_PÅ_KOMPLETT_SØKNAD);


    }
}
