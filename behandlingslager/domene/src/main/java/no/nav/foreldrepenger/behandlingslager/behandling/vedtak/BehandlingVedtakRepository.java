package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class BehandlingVedtakRepository {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;

    public BehandlingVedtakRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingVedtakRepository(@VLPersistenceUnit EntityManager entityManager,
                                          BehandlingRepository behandlingRepository) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
    }

    public BehandlingVedtakRepository(EntityManager entityManager) {
        this(entityManager, new BehandlingRepository(entityManager));
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    public Optional<BehandlingVedtak> hentBehandlingvedtakForBehandlingId(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
        TypedQuery<BehandlingVedtak> query = getEntityManager().createQuery("from BehandlingVedtak where behandlingsresultat.behandling.id=:behandlingId", BehandlingVedtak.class);
        query.setParameter("behandlingId", behandlingId); // $NON-NLS-1$
        return optionalFirstVedtak(query.getResultList());
    }

    public BehandlingVedtak hentBehandlingVedtakFraRevurderingensOriginaleBehandling(Behandling behandling) {
        if (!behandling.erRevurdering()) {
            throw new IllegalStateException("Utviklerfeil: Metoden skal bare kalles for revurderinger");
        }
        Behandling originalBehandling = behandling.getOriginalBehandling()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
        return hentBehandlingvedtakForBehandlingId(originalBehandling.getId())
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
        List<Behandling> avsluttedeIkkeHenlagteBehandlinger = behandlingRepository.finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsak.getId());
        if (avsluttedeIkkeHenlagteBehandlinger.isEmpty()) {
            return Optional.empty();
        }

        List<Behandling> behandlingerMedSisteVedtakstidspunkt = behandlingMedSisteVedtakstidspunkt(avsluttedeIkkeHenlagteBehandlinger);
        //Før PFP-8620 hadde vedtak bare dato og ikke klokkeslett
        if (behandlingerMedSisteVedtakstidspunkt.size() > 1) {
            Behandling behandlingMedGjeldendeVedtak = sisteEndretVedtak(behandlingerMedSisteVedtakstidspunkt);
            return hentBehandlingvedtakForBehandlingId(behandlingMedGjeldendeVedtak.getId());
        } else {
            return hentBehandlingvedtakForBehandlingId(behandlingerMedSisteVedtakstidspunkt.get(0).getId());
        }
    }

    private List<Behandling> behandlingMedSisteVedtakstidspunkt(List<Behandling> behandlinger) {
        LocalDateTime senestVedtak = LocalDateTime.MIN;
        List<Behandling> resultat = new ArrayList<>();
        for (Behandling behandling : behandlinger) {
            LocalDateTime vedtakstidspunkt = vedtakstidspunktForBehandling(behandling);
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
        return hentBehandlingvedtakForBehandlingId(behandling.getId()).orElseThrow().getVedtakstidspunkt();
    }

    private Behandling sisteEndretVedtak(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            throw new IllegalArgumentException("Behandlinger må ha minst ett element");
        }
        BehandlingVedtak sistEndretVedtak = null;
        for (Behandling behandling : behandlinger) {
            BehandlingVedtak vedtak = hentBehandlingvedtakForBehandlingId(behandling.getId()).orElseThrow();
            if (sistEndretVedtak == null || vedtak.getOpprettetTidspunkt().isAfter(sistEndretVedtak.getOpprettetTidspunkt())) {
                sistEndretVedtak = vedtak;
            }
        }
        return sistEndretVedtak.getBehandlingsresultat().getBehandling();
    }

    // sjekk lås og oppgrader til skriv
    private void verifiserBehandlingLås(BehandlingLås lås) {
        BehandlingLåsRepository låsHåndterer = new BehandlingLåsRepository(getEntityManager());
        låsHåndterer.oppdaterLåsVersjon(lås);
    }

    private static Optional<BehandlingVedtak> optionalFirstVedtak(List<BehandlingVedtak> behandlinger) {
        return behandlinger.isEmpty() ? Optional.empty() : Optional.of(behandlinger.get(0));
    }

}
