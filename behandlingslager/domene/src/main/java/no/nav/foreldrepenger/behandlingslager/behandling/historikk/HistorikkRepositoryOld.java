package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

@Deprecated(forRemoval = true) // Bruk HistorikkinnslagRepository
@ApplicationScoped
public class HistorikkRepositoryOld {
    private EntityManager entityManager;

    HistorikkRepositoryOld() {
        // for CDI proxy
    }

    @Inject
    public HistorikkRepositoryOld(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public void lagre(HistorikkinnslagOld historikkinnslag) {

        if (historikkinnslag.getFagsakId() == null) {
            historikkinnslag.setFagsakId(getFagsakId(historikkinnslag.getBehandlingId()));
        }

        entityManager.persist(historikkinnslag);
        for (var historikkinnslagDel : historikkinnslag.getHistorikkinnslagDeler()) {
            entityManager.persist(historikkinnslagDel);
            for (var historikkinnslagFelt : historikkinnslagDel.getHistorikkinnslagFelt()) {
                entityManager.persist(historikkinnslagFelt);
            }
        }
        entityManager.flush();
    }

    public List<HistorikkinnslagOld> hentHistorikk(Long behandlingId) {

        var fagsakId = getFagsakId(behandlingId);

        return entityManager.createQuery(
            "select h from HistorikkinnslagOld h where (h.behandlingId = :behandlingId OR h.behandlingId = NULL) AND h.fagsakId = :fagsakId ",
            HistorikkinnslagOld.class)
            .setParameter("fagsakId", fagsakId)
            .setParameter("behandlingId", behandlingId)
            .getResultList();
    }

    private Long getFagsakId(long behandlingId) {
        return entityManager.createQuery("select b.fagsak.id from Behandling b where b.id = :behandlingId", Long.class)
                .setParameter("behandlingId", behandlingId)
                .getSingleResult();
    }

    public List<HistorikkinnslagOld> hentHistorikkForSaksnummer(Saksnummer saksnummer) {
        return entityManager.createQuery(
                "select h from HistorikkinnslagOld h inner join Fagsak f On f.id = h.fagsakId where f.saksnummer= :saksnummer",
                HistorikkinnslagOld.class)
                .setParameter("saksnummer", saksnummer)
                .getResultList();
    }
}
