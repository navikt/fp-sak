package no.nav.foreldrepenger.behandlingslager.etterkontroll;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

/**
 * Oppdatering av tilstand for etterkontroll av behandling.
 */
@ApplicationScoped
public class EtterkontrollRepository {

    private EntityManager entityManager;

    EtterkontrollRepository() {
        // for CDI proxy
    }

    @Inject
    public EtterkontrollRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<Etterkontroll> finnEtterkontrollForFagsak(long fagsakId, KontrollType kontrollType) {
        return entityManager.createQuery(
                "from Etterkontroll " +
                        "where fagsakId = :fagsakId and kontrollType = :kontrollType",
                Etterkontroll.class)
                .setParameter("fagsakId", fagsakId)
                .setParameter("kontrollType", kontrollType)
                .getResultList();
    }

    /**
     * Lagrer etterkontroll på en fagsak
     *
     * @return id for {@link Etterkontroll} opprettet
     */
    public Long lagre(Etterkontroll etterkontroll) {
        entityManager.persist(etterkontroll);
        entityManager.flush();
        return etterkontroll.getId();
    }

    /**
     * Setter sak til behandlet=Y i etterkontroll slik at batch ikke plukker saken
     * opp for revurdering
     *
     * @param fagsakId id i databasen
     */
    public void avflaggDersomEksisterer(Long fagsakId, KontrollType kontrollType) {
        finnEtterkontrollForFagsak(fagsakId, kontrollType).forEach(ek -> {
            ek.setErBehandlet(true);
            lagre(ek);
        });
    }

    public List<Long> finnKandidaterForAutomatiskEtterkontroll() {

        var datoTidTilbake = LocalDate.now().atStartOfDay().plusHours(1);

        var query = entityManager.createQuery(
                "select f from Fagsak f inner join Etterkontroll k on f.id = k.fagsakId " +
                        "where k.erBehandlet = false and k.kontrollTidspunkt <= :periodeTilbake",
                Fagsak.class);
        query.setParameter("periodeTilbake", datoTidTilbake);

        return query.getResultList().stream()
                .map(Fagsak::getId)
                .toList();
    }
}
