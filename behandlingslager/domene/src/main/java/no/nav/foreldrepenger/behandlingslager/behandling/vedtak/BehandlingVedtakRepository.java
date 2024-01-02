package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

@ApplicationScoped
public class BehandlingVedtakRepository {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;

    public BehandlingVedtakRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingVedtakRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
        this.behandlingRepository = new BehandlingRepository(entityManager);
    }

    private EntityManager getEntityManager() {
        return entityManager;
    }

    public Optional<BehandlingVedtak> hentForBehandlingHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        var query = getEntityManager().createQuery("from BehandlingVedtak where behandlingsresultat.behandling.id=:behandlingId", BehandlingVedtak.class);
        query.setParameter("behandlingId", behandlingId);
        return optionalFirstVedtak(query.getResultList());
    }

    public BehandlingVedtak hentForBehandling(Long behandlingId) {
        return hentForBehandlingHvisEksisterer(behandlingId).orElseThrow(() ->
            new IllegalStateException("Finner ikke vedtak for behandling " + behandlingId));
    }

    public BehandlingVedtak hentBehandlingVedtakFraRevurderingensOriginaleBehandling(Behandling behandling) {
        if (!behandling.erRevurdering()) {
            throw new IllegalStateException("Utviklerfeil: Metoden skal bare kalles for revurderinger");
        }
        var originalBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
        return hentForBehandlingHvisEksisterer(originalBehandlingId)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Original behandling har ikke behandlingsvedtak - skal ikke skje"));
    }

    /**
     * Lagrer vedtak på behandling. Sørger for at samtidige oppdateringer på samme Behandling, eller andre Behandlinger
     * på samme Fagsak ikke kan gjøres samtidig.
     *
     * @see BehandlingLås
     */
    public Long lagre(BehandlingVedtak vedtak, BehandlingLås lås) {
        getEntityManager().persist(vedtak);
        verifiserBehandlingLås(lås);
        getEntityManager().flush();
        return vedtak.getId();
    }

    public Optional<BehandlingVedtak> hentGjeldendeVedtak(Fagsak fagsak) {
        return hentGjeldendeVedtak(fagsak.getId());
    }

    public Optional<BehandlingVedtak> hentGjeldendeVedtak(Long fagsakId) {
        var avsluttedeIkkeHenlagteBehandlinger = behandlingRepository.finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsakId);
        if (avsluttedeIkkeHenlagteBehandlinger.isEmpty()) {
            return Optional.empty();
        }

        var behandlingerMedSisteVedtakstidspunkt = behandlingMedSisteVedtakstidspunkt(avsluttedeIkkeHenlagteBehandlinger);
        //Før PFP-8620 hadde vedtak bare dato og ikke klokkeslett
        if (behandlingerMedSisteVedtakstidspunkt.size() > 1) {
            var behandlingMedGjeldendeVedtak = sisteEndretVedtak(behandlingerMedSisteVedtakstidspunkt);
            return hentForBehandlingHvisEksisterer(behandlingMedGjeldendeVedtak);
        }
        return hentForBehandlingHvisEksisterer(behandlingerMedSisteVedtakstidspunkt.get(0).getId());
    }

    private List<Behandling> behandlingMedSisteVedtakstidspunkt(List<Behandling> behandlinger) {
        var senestVedtak = LocalDateTime.MIN;
        List<Behandling> resultat = new ArrayList<>();
        for (var behandling : behandlinger) {
            var vedtakstidspunkt = vedtakstidspunktForBehandling(behandling);
            if (vedtakstidspunkt.isEqual(senestVedtak)) {
                resultat.add(behandling);
            } else if (vedtakstidspunkt.isAfter(senestVedtak)) {
                senestVedtak = vedtakstidspunkt;
                resultat.clear();
                resultat.add(behandling);
            }
        }
        return resultat;
    }

    private LocalDateTime vedtakstidspunktForBehandling(Behandling behandling) {
        return hentForBehandling(behandling.getId()).getVedtakstidspunkt();
    }

    private Long sisteEndretVedtak(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            throw new IllegalArgumentException("Behandlinger må ha minst ett element");
        }
        BehandlingVedtak sistEndretVedtak = null;
        for (var behandling : behandlinger) {
            var vedtak = hentForBehandling(behandling.getId());
            if (sistEndretVedtak == null || vedtak.getOpprettetTidspunkt().isAfter(sistEndretVedtak.getOpprettetTidspunkt())) {
                sistEndretVedtak = vedtak;
            }
        }
        return Optional.ofNullable(sistEndretVedtak).map(BehandlingVedtak::getBehandlingsresultat).map(Behandlingsresultat::getBehandlingId).orElse(null);
    }

    // sjekk lås og oppgrader til skriv
    private void verifiserBehandlingLås(BehandlingLås lås) {
        var låsHåndterer = new BehandlingLåsRepository(getEntityManager());
        låsHåndterer.oppdaterLåsVersjon(lås);
    }

    private static Optional<BehandlingVedtak> optionalFirstVedtak(List<BehandlingVedtak> behandlinger) {
        return behandlinger.isEmpty() ? Optional.empty() : Optional.of(behandlinger.get(0));
    }

}
