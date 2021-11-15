package no.nav.foreldrepenger.behandling.revurdering.flytkontroll;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;

/*
 * Metoder for å sekvensiere flyten av behandlinger for koblede saker.
 * Tar hensyn til kobling og hvor langt åpne behandlinger for annen parts sak har kommet.
 * Dessuten berørte behandlinger på egen sak.
 */

@ApplicationScoped
public class BehandlingFlytkontroll {

    private static final BehandlingStegType SYNK_STEG = StartpunktType.UTTAKSVILKÅR.getBehandlingSteg();

    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private BehandlingRepository behandlingRepository;

    public BehandlingFlytkontroll() {
        // For CDI proxy
    }

    @Inject
    public BehandlingFlytkontroll(BehandlingRevurderingRepository behandlingRevurderingRepository,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            BehandlingRepository behandlingRepository) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.behandlingRevurderingRepository = behandlingRevurderingRepository;
        this.behandlingRepository = behandlingRepository;
    }

    // Vente = true hvis egen sak har åpen berørt eller 2 part har åpen berørt eller
    // behandling som er i Uttak
    public boolean uttaksProsessenSkalVente(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var annenpartFagsak = behandlingRevurderingRepository.finnFagsakPåMedforelder(behandling.getFagsak()).orElse(null);
        if (annenpartFagsak == null) {
            return false;
        }
        var finnesBerørt = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(behandling.getFagsak().getId()).stream()
                .anyMatch(b -> !b.getId().equals(behandling.getId()) && SpesialBehandling.skalIkkeKøes(b));
        var annenpartÅpneBehandlinger = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(annenpartFagsak.getId());
        var annenPartHarBerørt = annenpartÅpneBehandlinger.stream()
                .anyMatch(SpesialBehandling::skalIkkeKøes);
        var annenPartAktivUttak = annenpartÅpneBehandlinger.stream()
                .anyMatch(b -> behandlingskontrollTjeneste.erStegPassert(b, SYNK_STEG));
        // TODO avklare om aktivUttak skal ekskludere behandling.isBehandlingPåVent();
        return finnesBerørt || annenPartHarBerørt || annenPartAktivUttak;
    }

    // Vente = true hvis egen sak har åpen berørt eller 2 part har åpen revurdering
    // (inkl berørt) eller førstegang som er i Uttak
    public boolean nyRevurderingSkalVente(Fagsak fagsak) {
        var annenpartFagsak = behandlingRevurderingRepository.finnFagsakPåMedforelder(fagsak).orElse(null);
        if (annenpartFagsak == null) {
            return false;
        }
        var finnesBerørt = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId()).stream()
                .anyMatch(SpesialBehandling::skalIkkeKøes);
        var annenPartRevurderingEllerAktivUttak = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(annenpartFagsak.getId()).stream()
                .anyMatch(b -> b.erRevurdering() || behandlingskontrollTjeneste.erStegPassert(b, SYNK_STEG));
        // TODO avklare om aktivUttak skal ekskludere behandling.isBehandlingPåVent();
        return finnesBerørt || annenPartRevurderingEllerAktivUttak;
    }

    public void settNyRevurderingPåVent(Behandling behandling) {
        behandlingskontrollTjeneste.settBehandlingPåVent(behandling, AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING, null, null,
                Venteårsak.VENT_ÅPEN_BEHANDLING);
    }

}
