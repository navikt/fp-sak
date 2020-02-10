package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class BehandlingsresultatRepository {

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;

    @Inject
    public BehandlingsresultatRepository(@VLPersistenceUnit EntityManager entityManager) {
        this.entityManager = entityManager;
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    protected BehandlingsresultatRepository() {
    }

    public Optional<Behandlingsresultat> hentHvisEksisterer(Long behandlingId) {
        TypedQuery<Behandlingsresultat> query = entityManager.createQuery("from Behandlingsresultat where behandling.id = :behandlingId", Behandlingsresultat.class);
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
