package no.nav.foreldrepenger.behandlingslager.kodeverk;

import static no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkFeil.FEILFACTORY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.DiscriminatorValue;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.QueryHints;

import no.nav.vedtak.felles.jpa.VLPersistenceUnit;
import no.nav.vedtak.util.LRUCache;

/**
 * Få tilgang til kodeverk.
 */
@ApplicationScoped
public class KodeverkRepository {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
    private LRUCache<String, Kodeliste> kodelisteCache = new LRUCache<>(5000, CACHE_ELEMENT_LIVE_TIME_MS);
    private EntityManager entityManager;

    protected KodeverkRepository() {
        // for CDI proxy
    }

    @Inject
    public KodeverkRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Map<String, Set<? extends Kodeliste>> hentAlle(List<Class<? extends Kodeliste>> classes) {
        Map<String, Set<? extends Kodeliste>> kodeverdier = new LinkedHashMap<>();

        // slå opp resten
        Set<Class<? extends Kodeliste>> andreKlasser = new LinkedHashSet<>(classes);

        if (!andreKlasser.isEmpty()) {
            var query = entityManager.createQuery(
                "FROM Kodeliste kl " +
                    "WHERE kl.kodeverk IN (:kodeverk) " +
                    "AND kl.kode != '-'",
                Kodeliste.class)
                .setParameter("kodeverk", getKodeverk(andreKlasser))
                .setHint(QueryHints.HINT_READONLY, "true")
                .setHint(QueryHints.HINT_CACHE_MODE, "IGNORE")
                .setHint(QueryHints.HINT_FETCH_SIZE, "50")
                .setHint("javax.persistence.fetchgraph", entityManager.getEntityGraph("KodelistMedNavn")); // NOSONAR
            var koderAndre = query.getResultStream()
                .peek(it -> entityManager.detach(it))
                .collect(Collectors.groupingBy(en -> en.getClass().getSimpleName(), Collectors.toSet()));

            kodeverdier.putAll(koderAndre);
        }
        return kodeverdier;
    }

    private List<String> getKodeverk(Collection<Class<? extends Kodeliste>> classes) {
        List<String> kodeverk = new ArrayList<>();
        for (Class<? extends BasisKodeverdi> aClass : classes) {
            kodeverk.add(aClass.getAnnotation(DiscriminatorValue.class).value());
        }
        return kodeverk;
    }

    /**
     * Finn instans av Kodeliste innslag for angitt kode verdi.
     */
    @SuppressWarnings("unchecked")
    public <V extends Kodeliste> V finn(Class<V> cls, String kode) {
        // bro til gamle Kodeliste entiteter
        final String cacheKey = cls.getName() + kode;
        Optional<V> fraCache = Optional.ofNullable((V) kodelisteCache.get(cacheKey));
        return fraCache.orElseGet(() -> {
            V finnEM = finnFraEM(cls, kode);
            kodelisteCache.put(cacheKey, finnEM);
            return finnEM;
        });
    }


    /**
     * Finn instans av Kodeliste innslag for angitt Kodeliste.
     * For oppslag av fulle instanser fra de ufullstendige i hver konkrete subklasse av Kodeliste.
     */
    public <V extends Kodeliste> V finn(Class<V> cls, V kodelisteKonstant) {
        return finn(cls, kodelisteKonstant.getKode());
    }

    /**
     * Hent alle innslag for en gitt kodeliste og gitte koder.
     */
    @SuppressWarnings("unchecked")
    public <V extends Kodeliste> List<V> finnListe(Class<V> cls, List<String> koder) {
        List<String> koderIkkeICache = new ArrayList<>();
        List<Kodeliste> result = new ArrayList<>();
        koder.stream().forEach(kode -> {
            // For hver kode
            V kodeliste = (V) kodelisteCache.get(kode);
            if (kodeliste == null) {
                koderIkkeICache.add(kode);
            } else {
                result.add(kodeliste);
            }
        });
        // kan kun ha Kodeliste instanser her
        result.addAll(finnListeFraEm((Class<Kodeliste>) cls, koderIkkeICache));
        return (List<V>) result;
    }

    /**
     * Finn instans av Kodeliste innslag for angitt offisielt navn for koden, eller en default value hvis offisielt navn ikke gir treff.
     */
    public <V extends Kodeliste> Optional<V> finnForKodeverkEiersNavn(Class<V> cls, String navn) {
        String språk = Kodeliste.hentLoggedInBrukerSpråk();
        DiscriminatorValue discVal = cls.getDeclaredAnnotation(DiscriminatorValue.class);
        Objects.requireNonNull(discVal, "Mangler @DiscriminatorValue i klasse:" + cls); //$NON-NLS-1$
        String kodeverk = discVal.value();

        Query query = entityManager.createNativeQuery(
            "SELECT kl_kode " +
                "FROM kodeliste_navn_i18n " +
                "WHERE kl_kodeverk = ? " +
                "AND navn = ? " +
                "AND sprak = ?");
        query.setParameter(1, kodeverk);
        query.setParameter(2, navn);
        query.setParameter(3, språk);

        @SuppressWarnings("unchecked")
        List<String> rowList = query.getResultList();

        for (String row : rowList) {
            return Optional.ofNullable(finn(cls, row));
        }
        return Optional.empty();
    }

    private <V extends Kodeliste> List<V> finnListeFraEm(Class<V> cls, List<String> koder) {
        CriteriaQuery<V> criteria = createCriteria(cls, koder);
        List<V> result = entityManager.createQuery(criteria)
            .setHint(QueryHints.HINT_READONLY, "true")
            .setHint("javax.persistence.fetchgraph", entityManager.getEntityGraph("KodelistMedNavn")) // NOSONAR
            .getResultList();
        return detachKodeverk(result);
    }

    private <V extends BasisKodeverdi> V finnFraEM(Class<V> cls, String kode) {
        CriteriaQuery<V> criteria = createCriteria(cls, Collections.singletonList(kode));
        try {
            V result = entityManager.createQuery(criteria)
                .setHint(QueryHints.HINT_READONLY, "true")
                .setHint("javax.persistence.fetchgraph", entityManager.getEntityGraph("KodelistMedNavn")) // NOSONAR
                .getSingleResult();
            return detachKodeverk(result);
        } catch (NoResultException e) {
            throw FEILFACTORY.kanIkkeFinneKodeverk(cls.getSimpleName(), kode, e).toException();
        }
    }

    private <V extends BasisKodeverdi> CriteriaQuery<V> createCriteria(Class<V> cls, List<String> koder) {
        return createCriteria(cls, "kode", koder);
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

    private <V extends BasisKodeverdi> V detachKodeverk(V result) {
        entityManager.detach(result);
        return result;
    }

    private <V extends Kodeliste> List<V> detachKodeverk(List<V> result) {
        for (V res : result) {
            detachKodeverk(res);
        }
        return result;
    }

}
