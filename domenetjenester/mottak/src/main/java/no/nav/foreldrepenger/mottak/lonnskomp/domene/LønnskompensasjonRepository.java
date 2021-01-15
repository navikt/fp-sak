package no.nav.foreldrepenger.mottak.lonnskomp.domene;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.ytelse.LønnskompensasjonVedtak;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class LønnskompensasjonRepository {

    private static final Logger log = LoggerFactory.getLogger(LønnskompensasjonRepository.class);
    private EntityManager entityManager;

    LønnskompensasjonRepository() {
        // CDI
    }

    @Inject
    public LønnskompensasjonRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public void lagre(LønnskompensasjonVedtak vedtak) {
        LønnskompensasjonVedtak eksisterende = hentSak(vedtak.getSakId(), vedtak.getAktørId()).orElse(null);
        if (eksisterende != null) {
            // Deaktiver eksisterende innslag
            eksisterende.setAktiv(false);
            entityManager.persist(eksisterende);
            entityManager.flush();
        }
        entityManager.persist(vedtak);
        entityManager.flush();
    }

    public List<LønnskompensasjonVedtak> hentSak(String sakId) {
        Objects.requireNonNull(sakId, "sakId");

        TypedQuery<LønnskompensasjonVedtak> query = entityManager.createQuery("SELECT v FROM LonnskompVedtakEntitet v " +
            "WHERE v.aktiv = true AND v.sakId = :sakId ", LønnskompensasjonVedtak.class);
        query.setParameter("sakId", sakId);

        return new ArrayList<>(query.getResultList());
    }

    public Optional<LønnskompensasjonVedtak> hentSak(String sakId, AktørId aktørId) {
        Objects.requireNonNull(sakId, "sakId");

        TypedQuery<LønnskompensasjonVedtak> query = entityManager.createQuery("SELECT v FROM LonnskompVedtakEntitet v " +
            "WHERE v.aktiv = true AND v.sakId = :sakId and v.aktørId = :fnr", LønnskompensasjonVedtak.class);
        query.setParameter("sakId", sakId);
        query.setParameter("fnr", aktørId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Set<LønnskompensasjonVedtak> hentLønnskompensasjonForIPeriode(AktørId aktørId, LocalDate fom, LocalDate tom) {
        TypedQuery<LønnskompensasjonVedtak> query = entityManager.createQuery("FROM LonnskompVedtakEntitet " +
            "WHERE aktørId = :aktørId " +
            "AND periode.fomDato <= :tom AND periode.tomDato >= :fom " +
            "AND aktiv = true", LønnskompensasjonVedtak.class);
        query.setParameter("aktørId", aktørId);
        query.setParameter("fom", fom);
        query.setParameter("tom", tom);
        query.setHint(QueryHints.HINT_READONLY, true);

        Set<LønnskompensasjonVedtak> resultat = new LinkedHashSet<>();
        var allevedtak = query.getResultList();
        for (LønnskompensasjonVedtak v : allevedtak) {
            if (resultat.stream().noneMatch(e -> LønnskompensasjonVedtak.erLikForBrukerOrg(e,v)))
                resultat.add(v);
        }
        return resultat;
    }

    public void oppdaterFødselsnummer(String fnr, AktørId aktørId) {
        Objects.requireNonNull(fnr, "fnr");

        entityManager.createNativeQuery("UPDATE lonnskomp_vedtak SET aktoer_id = :aid WHERE fnr = :fnr")
            .setParameter("aid", aktørId.getId()).setParameter("fnr", fnr).executeUpdate();
        entityManager.flush();
    }

    public boolean skalLagreVedtak(LønnskompensasjonVedtak eksisterende, LønnskompensasjonVedtak vedtak) {
        if (eksisterende == null)
            return true;
        var likeUtenomForrigeVedtak = Objects.equals(eksisterende, vedtak);
        if (likeUtenomForrigeVedtak) {
            log.info("Lønnskomp forkastes pga likt innhold {}", vedtak);
        }
        return !likeUtenomForrigeVedtak;
    }

}
