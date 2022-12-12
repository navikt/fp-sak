package no.nav.foreldrepenger.behandlingslager.uttak;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
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
        var lås = behandlingLåsRepository.taLås(behandlingId);
        var behandlingsresultat = hentBehandlingsresultat(behandlingId);
        var tidligereOpt = getAktivtUttaksperiodegrense(behandlingsresultat);
        if (tidligereOpt.filter(tupg -> tupg.getMottattDato().equals(uttaksperiodegrense.getMottattDato())).isPresent()) {
            return;
        }
        tidligereOpt.ifPresent(tidligere -> {
            tidligere.setAktiv(false);
            entityManager.persist(tidligere);
            entityManager.flush();
        });
        uttaksperiodegrense.setBehandlingsresultat(behandlingsresultat);
        entityManager.persist(uttaksperiodegrense);
        verifiserBehandlingLås(lås);
        entityManager.flush();
    }

    public Uttaksperiodegrense hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Fant ikke uttaksperiodegrense for behandling " + behandlingId));
    }

    public Optional<Uttaksperiodegrense> hentHvisEksisterer(Long behandlingId) {
        var query = entityManager
            .createQuery("select u from Uttaksperiodegrense u " +
                "where u.behandlingsresultat.behandling.id = :behandlingId " +
                "and u.aktiv = true", Uttaksperiodegrense.class)
            .setParameter("behandlingId", behandlingId); // NOSONAR
        return HibernateVerktøy.hentUniktResultat(query);
    }

    private Behandlingsresultat hentBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Må ha behandlingsresultat ved lagring av uttak. Behandling "
                + behandlingId));
    }

    private Optional<Uttaksperiodegrense> getAktivtUttaksperiodegrense(Behandlingsresultat behandlingsresultat) {
        Objects.requireNonNull(behandlingsresultat, "behandlingsresultat"); // NOSONAR $NON-NLS-1$
        final var query = entityManager.createQuery("FROM Uttaksperiodegrense Upg " +
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
