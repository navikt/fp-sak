package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class HistorikkRepository {
    private EntityManager entityManager;

    HistorikkRepository() {
        // for CDI proxy
    }

    @Inject
    public HistorikkRepository( EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public void lagre(Historikkinnslag historikkinnslag) {

        if (HistorikkAktør.SØKER.equals(historikkinnslag.getAktør()) && NavBrukerKjønn.UDEFINERT.equals(historikkinnslag.getKjoenn())) {
            var kjoenn = entityManager
                    .createQuery("select f.brukerRolle from Fagsak f where f.id = :fagsakId", RelasjonsRolleType.class)
                    .setParameter("fagsakId", historikkinnslag.getFagsakId())
                    .getSingleResult();
            if(RelasjonsRolleType.erRegistrertForeldre(kjoenn)) {
                if(kjoenn.equals(RelasjonsRolleType.MORA) || kjoenn.equals(RelasjonsRolleType.MEDMOR)) {
                    historikkinnslag.setKjoenn(NavBrukerKjønn.KVINNE);
                } else if(kjoenn.equals(RelasjonsRolleType.FARA)) {
                    historikkinnslag.setKjoenn(NavBrukerKjønn.MANN);
                }
            } else {
                historikkinnslag.setKjoenn(NavBrukerKjønn.UDEFINERT);
            }
        }

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

    public List<Historikkinnslag> hentHistorikk(Long behandlingId) {

        var fagsakId = getFagsakId(behandlingId);

        return entityManager.createQuery(
            "select h from Historikkinnslag h where (h.behandlingId = :behandlingId OR h.behandlingId = NULL) AND h.fagsakId = :fagsakId ",
            Historikkinnslag.class)
            .setParameter("fagsakId", fagsakId)
            .setParameter("behandlingId", behandlingId)
            .getResultList();
    }

    private Long getFagsakId(long behandlingId) {
        return entityManager.createQuery("select b.fagsak.id from Behandling b where b.id = :behandlingId", Long.class)
                .setParameter("behandlingId", behandlingId)
                .getSingleResult();
    }

    public List<Historikkinnslag> hentHistorikkForSaksnummer(Saksnummer saksnummer) {
        return entityManager.createQuery(
                "select h from Historikkinnslag h inner join Fagsak f On f.id = h.fagsakId where f.saksnummer= :saksnummer",
                Historikkinnslag.class)
                .setParameter("saksnummer", saksnummer)
                .getResultList();
    }
}
