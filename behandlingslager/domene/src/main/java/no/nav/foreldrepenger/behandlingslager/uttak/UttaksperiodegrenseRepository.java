package no.nav.foreldrepenger.behandlingslager.uttak;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.TraverseEntityGraphFactory;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.diff.DiffEntity;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraph;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class UttaksperiodegrenseRepository {

    private EntityManager entityManager;
    private BehandlingLåsRepository behandlingLåsRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public UttaksperiodegrenseRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.behandlingLåsRepository = new BehandlingLåsRepository(entityManager);
        this.behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
    }

    public UttaksperiodegrenseRepository() {
        //CDI
    }

    public void lagre(Long behandlingId, Uttaksperiodegrense uttaksperiodegrense) {
        final BehandlingLås lås = behandlingLåsRepository.taLås(behandlingId);
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingId);
        final Optional<Uttaksperiodegrense> tidligereAggregat = getAktivtUttaksperiodegrense(behandlingsresultat);
        if (tidligereAggregat.isPresent()) {
            final Uttaksperiodegrense aggregat = tidligereAggregat.get();
            boolean erForskjellig = uttaksperiodegrenseAggregatDiffer().areDifferent(aggregat, uttaksperiodegrense);
            if (erForskjellig) {
                aggregat.setAktiv(false);
                entityManager.persist(aggregat);
                entityManager.flush();
            }
        }
        behandlingsresultat.leggTilUttaksperiodegrense(uttaksperiodegrense);
        entityManager.persist(uttaksperiodegrense);
        verifiserBehandlingLås(lås);
        entityManager.flush();
    }

    public Uttaksperiodegrense hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    public Optional<Uttaksperiodegrense> hentHvisEksisterer(Long behandlingId) {
        TypedQuery<Uttaksperiodegrense> query = entityManager
            .createQuery("select u from Uttaksperiodegrense u " +
                "where u.behandlingsresultat.behandling.id = :behandlingId " +
                "and u.aktiv = true", Uttaksperiodegrense.class)
            .setParameter("behandlingId", behandlingId); // NOSONAR
        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void ryddUttaksperiodegrense(Long behandlingId) {
        BehandlingLås lås = behandlingLåsRepository.taLås(behandlingId);
        Behandlingsresultat behandlingsresultat = hentBehandlingsresultat(behandlingId);
        Optional<Uttaksperiodegrense> aktivtAggregat = getAktivtUttaksperiodegrense(behandlingsresultat);
        if (aktivtAggregat.isPresent()) {
            Uttaksperiodegrense aggregat = aktivtAggregat.get();
            aggregat.setAktiv(false);
            entityManager.persist(aggregat);
            verifiserBehandlingLås(lås);
            entityManager.flush();
        }
    }

    private DiffEntity uttaksperiodegrenseAggregatDiffer() {
        TraverseGraph traverser = TraverseEntityGraphFactory.build(false);
        return new DiffEntity(traverser);
    }

    private Behandlingsresultat hentBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Må ha behandlingsresultat ved lagring av uttak"));
    }

    private Optional<Uttaksperiodegrense> getAktivtUttaksperiodegrense(Behandlingsresultat behandlingsresultat) {
        Objects.requireNonNull(behandlingsresultat, "behandlingsresultat"); // NOSONAR $NON-NLS-1$
        final TypedQuery<Uttaksperiodegrense> query = entityManager.createQuery("FROM Uttaksperiodegrense Upg " +
            "WHERE Upg.behandlingsresultat.id = :behandlingresultatId " +
            "AND Upg.aktiv = :aktivt", Uttaksperiodegrense.class);
        query.setParameter("behandlingresultatId", behandlingsresultat.getId());
        query.setParameter("aktivt", true);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void verifiserBehandlingLås(BehandlingLås lås) {
        behandlingLåsRepository.oppdaterLåsVersjon(lås);
    }
}
