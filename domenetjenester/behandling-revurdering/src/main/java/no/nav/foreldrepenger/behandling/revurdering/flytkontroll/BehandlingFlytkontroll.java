package no.nav.foreldrepenger.behandling.revurdering.flytkontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;

/*
 * Metoder for å sekvensiere flyten av behandlinger for koblede saker.
 * Tar hensyn til kobling og hvor langt åpne behandlinger for annen parts sak har kommet.
 * Dessuten berørte behandlinger på egen sak.
 */

@ApplicationScoped
public class BehandlingFlytkontroll {

    private Logger LOG = LoggerFactory.getLogger(BehandlingFlytkontroll.class);

    private static final BehandlingStegType SYNK_STEG = StartpunktType.UTTAKSVILKÅR.getBehandlingSteg();

    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private BehandlingRepository behandlingRepository;

    public BehandlingFlytkontroll() {
        // For CDI proxy
    }

    @Inject
    public BehandlingFlytkontroll(BehandlingRevurderingTjeneste behandlingRevurderingTjeneste,
                                  BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                  BehandlingRepository behandlingRepository) {
        this.behandlingRevurderingTjeneste = behandlingRevurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    // Vente = true hvis egen sak har åpen berørt eller 2 part har åpen berørt eller behandling som er i Uttak
    public boolean uttaksProsessenSkalVente(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var annenpartFagsak = behandlingRevurderingTjeneste.finnFagsakPåMedforelder(behandling.getFagsak()).orElse(null);
        if (annenpartFagsak == null) {
            return false;
        }
        var finnesBerørt = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(behandling.getFagsak().getId()).stream()
                .anyMatch(b -> !b.getId().equals(behandling.getId()) && SpesialBehandling.skalIkkeKøes(b));
        var annenpartÅpneBehandlinger = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(annenpartFagsak.getId());
        var annenPartHarBerørt = annenpartÅpneBehandlinger.stream()
                .anyMatch(SpesialBehandling::skalIkkeKøes);
        var annenPartAktivUttak = annenpartÅpneBehandlinger.stream()
                .anyMatch(b -> behandlingProsesseringTjeneste.erBehandlingEtterSteg(b, SYNK_STEG));
        // Berørt skal bare køes dersom annenpart har en berørt som står i uttak.
        if (SpesialBehandling.skalIkkeKøes(behandling)) {
            // TODO fjern sjekk på ventVedSynk og fortsettAnnenPart dersom ikke inntreffer ila nov 2022
            var køetAnnenpartBerørt = annenpartÅpneBehandlinger.stream().filter(b -> SpesialBehandling.skalIkkeKøes(b) && venterVedUttakSynk(b)).findFirst();
            køetAnnenpartBerørt.ifPresent(b -> {
                LOG.warn("Berørt behandling: Annenpart har en berørt på vent ved uttak. Fortsetter annenpart og går selv på vent.");
                behandlingProsesseringTjeneste.dekøBehandling(b);
            });
            return annenpartÅpneBehandlinger.stream().anyMatch(b -> SpesialBehandling.skalIkkeKøes(b) && (passertUttakSynk(b) || venterVedUttakSynk(b)));
        }
        // TODO avklare om aktivUttak skal ekskludere behandling.isBehandlingPåVent();
        return finnesBerørt || annenPartHarBerørt || annenPartAktivUttak;
    }

    // Vente = true hvis egen sak har åpen berørt eller 2 part har åpen revurdering
    // (inkl berørt) eller førstegang som er i Uttak
    public boolean nyRevurderingSkalVente(Fagsak fagsak) {
        var annenpartFagsak = behandlingRevurderingTjeneste.finnFagsakPåMedforelder(fagsak).orElse(null);
        if (annenpartFagsak == null) {
            return false;
        }
        var finnesBerørt = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId()).stream()
                .anyMatch(SpesialBehandling::skalIkkeKøes);
        var annenPartRevurderingEllerAktivUttak = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(annenpartFagsak.getId()).stream()
                .anyMatch(b -> b.erRevurdering() || behandlingProsesseringTjeneste.erBehandlingEtterSteg(b, SYNK_STEG));
        // TODO avklare om aktivUttak skal ekskludere behandling.isBehandlingPåVent();
        return finnesBerørt || annenPartRevurderingEllerAktivUttak;
    }

    public void settNyRevurderingPåVent(Behandling behandling) {
        behandlingProsesseringTjeneste.enkøBehandling(behandling);
    }

    public boolean finnesÅpenBerørtForFagsak(Long fagsakId, Long behandlingId) {
        return behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakId).stream()
            .anyMatch(b -> !b.getId().equals(behandlingId) && SpesialBehandling.skalIkkeKøes(b));
    }

    private boolean passertUttakSynk(Behandling behandling) {
        return behandlingProsesseringTjeneste.erBehandlingEtterSteg(behandling, SYNK_STEG) || venterVedUttakSynk(behandling);
    }

    private boolean venterVedUttakSynk(Behandling behandling) {
        return SYNK_STEG.equals(behandling.getAktivtBehandlingSteg()) && behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.AUTO_KØET_BEHANDLING);
    }

}
