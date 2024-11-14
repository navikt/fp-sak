package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.ArrayList;
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

    Historikkinnslag2Repository() {
        //CDI
    }

    public List<Historikkinnslag2> hent(Saksnummer saksnummer) {
        return entityManager.createQuery("select h from Historikkinnslag2 h inner join Fagsak f On f.id = h.fagsakId where f.saksnummer= :saksnummer",
            Historikkinnslag2.class).setParameter("saksnummer", saksnummer).getResultStream().toList();
    }

    public void lagre(HistorikkinnslagV2 historikkinnslag) {
        lagre(map(historikkinnslag));
    }

    public void lagre(Historikkinnslag2 historikkinnslag) {
        entityManager.persist(historikkinnslag);
        for (var tekstlinje : historikkinnslag.getTekstlinjer()) {
            entityManager.persist(tekstlinje);
        }
        entityManager.flush();
    }

    private static Historikkinnslag2 map(HistorikkinnslagV2 historikkinnslag) {
        var tekstlinjer = new ArrayList<Historikkinnslag2Tekstlinje>();
        for (int i = 0; i < historikkinnslag.getLinjer().size(); i++) {
            var linje = map(historikkinnslag.getLinjer().get(i), i);
            tekstlinjer.add(linje);
        }
        return new Historikkinnslag2(historikkinnslag.getFagsakId(), historikkinnslag.getBehandlingId(), historikkinnslag.getAktør(),
            historikkinnslag.getSkjermlenke(), historikkinnslag.getTittel(), tekstlinjer);
    }

    private static Historikkinnslag2Tekstlinje map(HistorikkinnslagV2.Tekstlinje t, int indeks) {
        return new Historikkinnslag2Tekstlinje(t.asString(), String.valueOf(indeks));
    }
}
