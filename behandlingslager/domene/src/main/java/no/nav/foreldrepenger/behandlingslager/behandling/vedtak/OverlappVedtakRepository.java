package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ApplicationScoped
public class OverlappVedtakRepository {

    private EntityManager entityManager;

    public OverlappVedtakRepository() {
        // for CDI proxy
    }

    @Inject
    public OverlappVedtakRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<OverlappVedtak> hentForSaksnummer(Saksnummer saksnummer) {
        var query = entityManager
            .createQuery("from OverlappVedtak where saksnummer=:saksnummer",
                OverlappVedtak.class);
        query.setParameter("saksnummer", saksnummer);
        return query.getResultList();
    }

    public void slettAvstemtEnkeltsak(Saksnummer saksnummer) {
        var query = entityManager.createNativeQuery(
            "DELETE FROM OVERLAPP_VEDTAK WHERE saksnummer=:saksnummer and hendelse=:hendelse");
        query.setParameter("saksnummer", saksnummer);
        query.setParameter("hendelse", OverlappVedtak.HENDELSE_AVSTEM_SAK + "-" + saksnummer.getVerdi());
        query.executeUpdate();
    }

    public void slettAvstemtPeriode(LocalDate før) {
        entityManager.createNativeQuery(
            "DELETE FROM OVERLAPP_VEDTAK WHERE opprettet_tid < :foer")
            .setParameter("foer", før.atStartOfDay())
            .executeUpdate();
    }

    public void slettAvstemtPeriode(LocalDate før, String hendelse) {
        entityManager.createNativeQuery("DELETE FROM OVERLAPP_VEDTAK WHERE opprettet_tid < :foer and hendelse=:hendelse")
            .setParameter("foer", før.atStartOfDay())
            .setParameter("hendelse", hendelse)
            .executeUpdate();
    }

    public void lagre(OverlappVedtak.Builder overlappBuilder) {
        var overlapp = overlappBuilder.build();
        entityManager.persist(overlapp);
        entityManager.flush();
    }
    public void lagre(OverlappVedtak overlapp) {
        entityManager.persist(overlapp);
        entityManager.flush();
    }

    public void lagre(List<OverlappVedtak.Builder> overlappene) {
        overlappene.stream().map(OverlappVedtak.Builder::build).forEach(entityManager::persist);
        entityManager.flush();
    }

}
