package no.nav.foreldrepenger.behandlingslager.lagretvedtak;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.BehandlingslagerRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

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


    public LagretVedtak hentLagretVedtak(long lagretVedtakId) {
        var query = entityManager.createQuery("from LagretVedtak where id=:lagretVedtakId", LagretVedtak.class);
        query.setParameter("lagretVedtakId", lagretVedtakId);
        query.setHint(QueryHints.HINT_READONLY, "true");
        return hentEksaktResultat(query);
    }

    public LagretVedtak hentLagretVedtakForBehandling(long behandlingId) {
        var query = entityManager.createQuery("from LagretVedtak where BEHANDLING_ID=:behandlingId", LagretVedtak.class);
        query.setParameter("behandlingId", behandlingId);
        query.setHint(QueryHints.HINT_READONLY, "true");
        return hentEksaktResultat(query);
    }

    public LagretVedtak hentLagretVedtakForBehandlingForOppdatering(long behandlingId) {
        var query = entityManager.createQuery("from LagretVedtak where BEHANDLING_ID=:behandlingId", LagretVedtak.class);
        query.setParameter("behandlingId", behandlingId);
        return hentEksaktResultat(query);
    }

    public List<LagretVedtak> hentLagreteVedtakPåFagsak(long fagsakId) {
        var query = entityManager.createQuery("from LagretVedtak where FAGSAK_ID=:fagsakId", LagretVedtak.class);
        query.setParameter("fagsakId", fagsakId);
        query.setHint(QueryHints.HINT_READONLY, "true");
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
        List<BigDecimal> resultater = query.getResultList();
        return resultater.stream().map(Number::longValue).toList();

    }

    public List<Long> hentLagretVedtakBehandlingId(LocalDateTime fom, LocalDateTime tom, FagsakYtelseType fagsakYtelseType) {
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");
        Objects.requireNonNull(fagsakYtelseType, "fagsakYtelseType");

        var sql = "select lv.behandling_id from LAGRET_VEDTAK lv, FAGSAK fs where lv.OPPRETTET_TID >= :fom and lv.OPPRETTET_TID <= :tom and lv.FAGSAK_ID = fs.ID and fs.YTELSE_TYPE = :fagsakYtelseType";

        var query = entityManager.createNativeQuery(sql);
        query.setParameter("fom", fom);
        query.setParameter("tom", tom);
        query.setParameter("fagsakYtelseType", fagsakYtelseType.getKode());

        @SuppressWarnings("unchecked")
        List<BigDecimal> resultater = query.getResultList();
        return resultater.stream().map(Number::longValue).toList();
    }

}
