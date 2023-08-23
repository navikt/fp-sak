package no.nav.foreldrepenger.behandlingslager.behandling.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import org.hibernate.jpa.HibernateHints;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Ulike spesialmetoder for å hente opp behandlinger som er kandidater for videre spesiell prosessering, slik som
 * etterkontroll gjenopptagelse av behandlinger på vent og lignende.
 * <p>
 * Disse vil bil brukt i en trigging av videre prosessering, behandling, kontroll, evt. henlegging eller avslutting.
 */

@ApplicationScoped
public class BehandlingKandidaterRepository {

    private static final Set<BehandlingStatus> AVSLUTTENDE_STATUS = BehandlingStatus.getFerdigbehandletStatuser();
    private static final String AVSLUTTENDE_KEY = "avsluttetOgIverksetterStatus";
    private EntityManager entityManager;

    BehandlingKandidaterRepository() {
        // for CDI proxy
    }

    @Inject
    public BehandlingKandidaterRepository( EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected EntityManager getEntityManager() {
        return entityManager;
    }

    public List<Behandling> finnBehandlingerForAutomatiskGjenopptagelse() {

        var query = getEntityManager().createQuery("""
             SELECT DISTINCT b
                 FROM Aksjonspunkt ap
                 INNER JOIN ap.behandling b
                 WHERE ap.status IN (:aapneAksjonspunktKoder)
                   AND ap.fristTid < :naa
                """, Behandling.class)
            .setHint(HibernateHints.HINT_READ_ONLY, "true")
            .setParameter("naa", LocalDateTime.now())
            .setParameter("aapneAksjonspunktKoder", AksjonspunktStatus.getÅpneAksjonspunktStatuser());
        return query.getResultList();
    }

    public List<Behandling> finnBehandlingerIkkeAvsluttetPåAngittEnhet(String enhetId) {

        var query = entityManager.createQuery(
            "FROM Behandling behandling " +
                "WHERE behandling.status NOT IN (:avsluttetOgIverksetterStatus) " +
                "  AND behandling.behandlendeEnhet = :enhet ",
            Behandling.class);

        query.setParameter("enhet", enhetId);
        query.setParameter(AVSLUTTENDE_KEY, AVSLUTTENDE_STATUS);
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }

    public List<Behandling> finnÅpneBehandlingerUtenÅpneAksjonspunktEllerAutopunkt() {

        var query = entityManager.createQuery(
            "SELECT bh FROM Behandling bh " +
                "WHERE bh.status NOT IN (:avsluttetOgIverksetterStatus) " +
                "  AND NOT EXISTS (SELECT ap FROM Aksjonspunkt ap WHERE ap.behandling=bh AND ap.status = :status) ",
            Behandling.class);

        query.setParameter(AVSLUTTENDE_KEY, AVSLUTTENDE_STATUS);
        query.setParameter("status", AksjonspunktStatus.OPPRETTET);
        query.setHint(HibernateHints.HINT_READ_ONLY, "true");
        return query.getResultList();
    }
}
