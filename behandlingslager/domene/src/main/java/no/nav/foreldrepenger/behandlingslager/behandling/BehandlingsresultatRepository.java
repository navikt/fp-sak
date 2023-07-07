package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class BehandlingsresultatRepository {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;

    @Inject
    public BehandlingsresultatRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
        this.behandlingRepository = new BehandlingRepository(entityManager);
    }

    protected BehandlingsresultatRepository() {
    }

    public Optional<Behandlingsresultat> hentHvisEksisterer(Long behandlingId) {
        var query = entityManager.createQuery("from Behandlingsresultat where behandling.id = :behandlingId", Behandlingsresultat.class);
        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Behandlingsresultat hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow(() -> new IllegalStateException("Forventet behandlingsresultat"));
    }

    public void lagre(Long behandlingId, Behandlingsresultat resultat) {
        // midlertidig må hente Behandling (inntil vi får fjernet Behandlingresultat#getBehandling()
        // dropper foreløpig lås her
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        resultat.setBehandling(behandling);
        entityManager.persist(resultat);
        entityManager.flush();
    }

}
