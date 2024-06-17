package no.nav.foreldrepenger.behandlingslager.kodeverk;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class LagretKodeverdiRepository {

    private EntityManager entityManager;

    LagretKodeverdiRepository() {
        // for CDI proxy
    }

    @Inject
    public LagretKodeverdiRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager");
        this.entityManager = entityManager;
    }

    public Map<String, LagretKodeverdiNavn> hentLagretKodeverk(String kodeverk) {
        var query = entityManager.createQuery("from LagretKodeverdiNavn where kodeverk = :kodeverk", LagretKodeverdiNavn.class)
            .setParameter("kodeverk", kodeverk);

        return query.getResultList().stream().collect(Collectors.toMap(LagretKodeverdiNavn::getKode, a -> a));
    }

    public void lagre(LagretKodeverdiNavn lagretKodeverdiNavn) {
        entityManager.persist(lagretKodeverdiNavn);
        entityManager.flush();
    }

}
