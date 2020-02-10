package no.nav.foreldrepenger.domene.vedtak.repo;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentEksaktResultat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.BehandlingslagerRepository;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtak;
import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtakMedBehandlingType;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class LagretVedtakRepository implements BehandlingslagerRepository {

    private EntityManager entityManager;

    LagretVedtakRepository() {
        // for CDI proxy
    }

    @Inject
    public LagretVedtakRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    // Vedtak i Vedtakslager
    public long lagre(LagretVedtak lagretVedtak) {
        if (entityManager.contains(lagretVedtak)) {
            // Eksisterende og persistent - ikke gjør noe
            @SuppressWarnings("unused")
            int brkpt = 1; // NOSONAR
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
        LagretVedtak merge = entityManager.merge(lagretVedtak);
        return merge.getId();
    }


    public LagretVedtak hentLagretVedtak(long lagretVedtakId) {
        TypedQuery<LagretVedtak> query = entityManager.createQuery("from LagretVedtak where id=:lagretVedtakId", LagretVedtak.class); //$NON-NLS-1$
        query.setParameter("lagretVedtakId", lagretVedtakId); //$NON-NLS-1$
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return hentEksaktResultat(query);
    }

    public LagretVedtak hentLagretVedtakForBehandling(long behandlingId) {
        TypedQuery<LagretVedtak> query = entityManager.createQuery("from LagretVedtak where BEHANDLING_ID=:behandlingId", LagretVedtak.class); //$NON-NLS-1$
        query.setParameter("behandlingId", behandlingId); //$NON-NLS-1$
        query.setHint(QueryHints.HINT_READONLY, "true"); //$NON-NLS-1$
        return hentEksaktResultat(query);
    }

    public LagretVedtak hentLagretVedtakForBehandlingForOppdatering(long behandlingId) {
        TypedQuery<LagretVedtak> query = entityManager.createQuery("from LagretVedtak where BEHANDLING_ID=:behandlingId", LagretVedtak.class); //$NON-NLS-1$
        query.setParameter("behandlingId", behandlingId); //$NON-NLS-1$
        return hentEksaktResultat(query);
    }

    public List<LagretVedtakMedBehandlingType> hentLagreteVedtakPåFagsak(long fagsakId) {
        Objects.requireNonNull(fagsakId, "fagsakId"); //NOSONAR

        String sql = "SELECT " +
            "l.BEHANDLING_ID id, " +
            "b.BEHANDLING_TYPE behandlingType, " +
            "l.opprettet_tid opprettetDato " +
            "FROM LAGRET_VEDTAK l " +
            "JOIN BEHANDLING b ON b.id = l.BEHANDLING_ID " +
            "WHERE l.FAGSAK_ID = :fagsakId";

        Query query = entityManager.createNativeQuery(sql, "LagretVedtakResult");
        query.setParameter("fagsakId", fagsakId);

        @SuppressWarnings("unchecked")
        List<LagretVedtakMedBehandlingType> resultater = query.getResultList();
        return resultater;
    }

    public List<Long> hentLagreteVedtakBehandlingId(LocalDateTime fom, LocalDateTime tom){

        Objects.requireNonNull(fom, "fom"); //NOSONAR
        Objects.requireNonNull(tom, "tom"); //NOSONAR

        String sql = "SELECT " +
            "BEHANDLING_ID " +
            "FROM LAGRET_VEDTAK  " +
            "WHERE OPPRETTET_TID >= :fom " +
            "AND OPPRETTET_TID <= :tom ";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("fom", fom);
        query.setParameter("tom", tom);

        @SuppressWarnings("unchecked")
        List<BigDecimal> resultater = query.getResultList();
        List<Long> liste = resultater.stream().map(s ->  s.longValue()).collect(Collectors.toList());
        return liste;

    }

    public List<Long> hentLagretVedtakBehandlingId(LocalDateTime fom, LocalDateTime tom, FagsakYtelseType fagsakYtelseType) {
        Objects.requireNonNull(fom, "fom"); //NOSONAR
        Objects.requireNonNull(tom, "tom"); //NOSONAR
        Objects.requireNonNull(fagsakYtelseType, "fagsakYtelseType");

        String sql = "select lv.behandling_id from LAGRET_VEDTAK lv, FAGSAK fs where lv.OPPRETTET_TID >= :fom and lv.OPPRETTET_TID <= :tom and lv.FAGSAK_ID = fs.ID and fs.YTELSE_TYPE = :fagsakYtelseType";

        var query = entityManager.createNativeQuery(sql);
        query.setParameter("fom", fom); // NOSONAR $NON-NLS-1$
        query.setParameter("tom", tom); // NOSONAR $NON-NLS-1$
        query.setParameter("fagsakYtelseType", fagsakYtelseType.getKode()); // NOSONAR $NON-NLS-1$

        @SuppressWarnings("unchecked")
        List<BigDecimal> resultater = query.getResultList();
        List<Long> liste = resultater.stream().map(s ->  s.longValue()).collect(Collectors.toList());
        return liste;
    }

}
