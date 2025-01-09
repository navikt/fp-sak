package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class Historikkinnslag2Repository {

    private EntityManager entityManager;

    @Inject
    public Historikkinnslag2Repository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Historikkinnslag2Repository() {
        //CDI
    }

    public List<Historikkinnslag2> hent(Saksnummer saksnummer) {
        return entityManager.createQuery("select h from Historikkinnslag2 h inner join Fagsak f On f.id = h.fagsakId where f.saksnummer= :saksnummer",
            Historikkinnslag2.class).setParameter("saksnummer", saksnummer).getResultStream().toList();
    }

    public void lagre(Historikkinnslag2 historikkinnslag) {
        entityManager.persist(historikkinnslag);
        for (var linje : historikkinnslag.getLinjer()) {
            entityManager.persist(linje);
        }
        for (var dokument : historikkinnslag.getDokumentLinker()) {
            entityManager.persist(dokument);
        }
        entityManager.flush();
    }
}
