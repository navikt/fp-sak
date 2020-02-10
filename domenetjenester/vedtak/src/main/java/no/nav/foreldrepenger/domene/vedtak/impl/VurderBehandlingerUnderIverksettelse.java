package no.nav.foreldrepenger.domene.vedtak.impl;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class VurderBehandlingerUnderIverksettelse {

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    VurderBehandlingerUnderIverksettelse() {
        // for CDI proxy
    }

    @Inject
    public VurderBehandlingerUnderIverksettelse(BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    /**
     * Håndter kø av iverksetting ytelsesbehandlinger (1gang, revurdering) basert på vedtakstidspunkt (obs berørt)
     *
     * @param behandling en {@link Behandling}
     * @return true hvis det finnes annet vedtak i samme sak under iverksetting (vedtatt tidligere)
     */
    public boolean vurder(Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            return false;
        }
        LocalDateTime vedtaksTidspunkt = utledVedtakstidspunkt(behandling);
        // Kan ikke se på behandlingId pga kø-sniking rundt berørt behandling
        return finnBehandlingerUnderIverksetting(behandling).stream()
            .map(this::utledVedtakstidspunkt)
            .anyMatch(vedtaksTidspunkt::isAfter);
    }

    /**
     * Håndter kø av iverksetting ytelsesbehandlinger (1gang, revurdering) basert på vedtakstidspunkt (obs berørt)
     *
     * @param behandling en {@link Behandling}
     * @return behandling hvis det finnes en annen behandling på samme sak som skal iverksettes
     */
    public Optional<Behandling> finnBehandlingSomVenterIverksetting(Behandling behandling) {
        if (!behandling.erYtelseBehandling()) {
            return Optional.empty();
        }
        // Finn behandlinger i samme sak. OBS på at berørt sniker i køen så man bør se på vedtakstidspunkt
        Optional<BehandlingVedtak> venter = finnBehandlingerUnderIverksetting(behandling).stream()
            .filter(beh -> BehandlingStegStatus.STARTET.equals(beh.getBehandlingStegStatus()))
            .map(beh -> behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(beh.getId()).orElse(null))
            .filter(Objects::nonNull)
            .min(Comparator.comparing(BehandlingVedtak::getOpprettetTidspunkt));
        return venter.map(BehandlingVedtak::getBehandlingsresultat).map(Behandlingsresultat::getBehandling);
    }

    private List<Behandling> finnBehandlingerUnderIverksetting(Behandling behandling) {
        // Finn behandlinger i samme sak. OBS på at berørt sniker i køen så man bør se på vedtakstidspunkt
        return behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(behandling.getFagsakId()).stream()
            .filter(beh -> BehandlingStatus.IVERKSETTER_VEDTAK.equals(beh.getStatus()))
            .filter(Behandling::erYtelseBehandling)
            .filter(beh -> !behandling.getId().equals(beh.getId()))
            .collect(Collectors.toList());
    }

    private LocalDateTime utledVedtakstidspunkt(Behandling behandling) {
        return behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandling.getId())
            .map(BehandlingVedtak::getOpprettetTidspunkt).orElse(Tid.TIDENES_ENDE.atStartOfDay());
    }
}
