package no.nav.foreldrepenger.behandlingslager.geografisk;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.DiscriminatorValue;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.QueryHints;

import no.nav.foreldrepenger.behandlingslager.kodeverk.BasisKodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeliste;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;
import no.nav.vedtak.util.LRUCache;

/**
 * @deprecated Bør fjernes, kan tolke nødvendige språkkoder fra TPS direkte? Brukes p.t. til å finne brukers foretrukne språk basert på
 *             'mangled' TPS språk koder.
 */
@ApplicationScoped
public class SpråkKodeverkRepository {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
    private LRUCache<String, Kodeliste> kodelisteCache = new LRUCache<>(5000, CACHE_ELEMENT_LIVE_TIME_MS);
    private EntityManager entityManager;

    SpråkKodeverkRepository() {
        // for CDI proxy
    }

    @Inject
    public SpråkKodeverkRepository(@VLPersistenceUnit EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Språkkode> finnSpråkMedKodeverkEiersKode(String kodeverkEiersKode) {
        try {
            return finnForKodeverkEiersKode(Språkkode.class, kodeverkEiersKode);
        } catch (TekniskException e) { // NOSONAR
            return Optional.empty();
        }
    }

    /**
     * Finn instans av Kodeliste innslag for angitt offisiell kode verdi.
     * 
     * @deprecated Kan antagelig fjernes? brukes kun av SpråkkodeRepository per nå (hvor den sanns. ikke har noe liv)
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    private <V extends Kodeliste> Optional<V> finnForKodeverkEiersKode(Class<V> cls, String offisiellKode) {
        String cacheKey = cls.getName() + offisiellKode;
        Optional<V> fraCache = Optional.ofNullable((V) kodelisteCache.get(cacheKey));
        var v = fraCache.orElseGet(() -> {
            V result = finnForKodeverkEiersKodeFraEM(cls, offisiellKode);
            kodelisteCache.put(cacheKey, result);
            return result;
        });
        return Optional.ofNullable(v);
    }

    private <V extends Kodeliste> V finnForKodeverkEiersKodeFraEM(Class<V> cls, String offisiellKode) {
        CriteriaQuery<V> criteria = createCriteria(cls, "offisiellKode", Collections.singletonList(offisiellKode));
        try {
            V result = entityManager.createQuery(criteria)
                .setHint(QueryHints.HINT_READONLY, "true")
                .setHint("javax.persistence.fetchgraph", entityManager.getEntityGraph("KodelistMedNavn")) // NOSONAR
                .getSingleResult();
            return detachKodeverk(result);
        } catch (NoResultException e) {
            return null;
        }
    }

    private <V extends BasisKodeverdi> V detachKodeverk(V result) {
        entityManager.detach(result);
        return result;
    }

    private <V extends BasisKodeverdi> CriteriaQuery<V> createCriteria(Class<V> cls, String felt, List<String> koder) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<V> criteria = builder.createQuery(cls);

        DiscriminatorValue discVal = cls.getDeclaredAnnotation(DiscriminatorValue.class);
        Objects.requireNonNull(discVal, "Mangler @DiscriminatorValue i klasse:" + cls); //$NON-NLS-1$
        String kodeverk = discVal.value();
        Root<V> from = criteria.from(cls);
        criteria.where(builder.and(
            builder.equal(from.get("kodeverk"), kodeverk), //$NON-NLS-1$
            from.get(felt).in(koder))); // $NON-NLS-1$
        return criteria;
    }

}
