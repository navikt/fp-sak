package no.nav.foreldrepenger.behandlingslager.lagretvedtak;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.hibernate.jpa.HibernateHints;

import no.nav.foreldrepenger.behandlingslager.BehandlingslagerRepository;

@ApplicationScoped
public class LagretVedtakRepository implements BehandlingslagerRepository {

    private EntityManager entityManager;

    LagretVedtakRepository() {
        // for CDI proxy
    }

    @Inject
    public LagretVedtakRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    // Vedtak i Vedtakslager
    public long lagre(LagretVedtak lagretVedtak) {
        if (entityManager.contains(lagretVedtak)) {
            // Eksisterende og persistent - ikke gjør noe
            @SuppressWarnings("unused") var brkpt = 1;
        } else if (lagretVedtak.getId() != null) {
            // Eksisterende men detached - oppdater
            entityManager.merge(lagretVedtak);
        } else {
            // Ny - insert
            entityManager.persist(lagretVedtak);
        }
        return lagretVedtak.getId();
    }

    public long oppdater(LagretVedtak lagretVedtak, String nyVedtakXml) {
        lagretVedtak.setXmlClob(nyVedtakXml);
        var merge = entityManager.merge(lagretVedtak);
        return merge.getId();
    }

    public LagretVedtak hentLagretVedtakForBehandling(long behandlingId) {
        var query = entityManager.createQuery("from LagretVedtak where behandlingId=:behandlingId", LagretVedtak.class);
        query.setParameter("behandlingId", behandlingId);
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        return hentEksaktResultat(query);
    }

    public LagretVedtak hentLagretVedtakForBehandlingForOppdatering(long behandlingId) {
        var query = entityManager.createQuery("from LagretVedtak where behandlingId=:behandlingId", LagretVedtak.class);
        query.setParameter("behandlingId", behandlingId);
        return hentEksaktResultat(query);
    }

    public List<LagretVedtak> hentLagreteVedtakPåFagsak(long fagsakId) {
        var query = entityManager.createQuery("from LagretVedtak where fagsakId=:fagsakId", LagretVedtak.class);
        query.setParameter("fagsakId", fagsakId);
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }

    public List<Long> hentLagreteVedtakBehandlingId(LocalDateTime fom, LocalDateTime tom){

        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");

        var sql = "SELECT " +
            "BEHANDLING_ID " +
            "FROM LAGRET_VEDTAK  " +
            "WHERE OPPRETTET_TID >= :fom " +
            "AND OPPRETTET_TID <= :tom ";

        var query = entityManager.createNativeQuery(sql);
        query.setParameter("fom", fom);
        query.setParameter("tom", tom);

        @SuppressWarnings("unchecked")
        List<Number> resultater = query.getResultList();
        return resultater.stream().map(Number::longValue).toList();

    }
}
