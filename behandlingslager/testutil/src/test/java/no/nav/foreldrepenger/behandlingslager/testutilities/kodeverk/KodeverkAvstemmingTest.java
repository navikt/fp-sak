package no.nav.foreldrepenger.behandlingslager.testutilities.kodeverk;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.DiscriminatorValue;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.persistence.metamodel.ManagedType;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeliste;
import no.nav.foreldrepenger.dbstoette.DatasourceConfiguration;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.lokal.dbstoette.DBConnectionProperties;
import no.nav.vedtak.felles.lokal.dbstoette.DatabaseStøtte;

@RunWith(Parameterized.class)
public class KodeverkAvstemmingTest {

    private static final EntityManagerFactory entityManagerFactory;

    static {
        // Kan ikke skrus på nå - trigger på CHAR kolonner som kunne vært VARCHAR.  Må fikses først
        //System.setProperty("hibernate.hbm2ddl.auto", "validate");
        try {
            // trenger å konfigurere opp jndi etc.
            DBConnectionProperties connectionProperties = DBConnectionProperties.finnDefault(DatasourceConfiguration.UNIT_TEST.get()).get();
            DatabaseStøtte.settOppJndiForDefaultDataSource(Collections.singletonList(connectionProperties));
        } catch (FileNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
        entityManagerFactory = Persistence.createEntityManagerFactory("pu-default");
    }

    
    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager em = repoRule.getEntityManager();

    private Class<?> kodeverkClass;

    @org.junit.runners.Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() throws Exception {
       var baseEntitetSubklasser = getEntityClasses(c -> Kodeliste.class.isAssignableFrom(c) && c.isAnnotationPresent(DiscriminatorValue.class));
       
       Map<String, Object[]> params = new LinkedHashMap<>();

       for (Class<?> c : baseEntitetSubklasser) {
           params.put(c.getName(), new Object[]{c.getSimpleName(), c, c.getAnnotation(DiscriminatorValue.class).value()});
       }
       assertThat(params).isNotEmpty();
       
       return params.values();

    }

    @SuppressWarnings("unused")
    public KodeverkAvstemmingTest(String className, @SuppressWarnings("rawtypes") Class kodeverkClass, String kodeverk) {
        this.kodeverkClass = kodeverkClass;
    }

    @Test
    public void sjekk_at_kodeliste_konstanter_finnes_også_i_database() throws Exception {
        String feilFantIkke = "Fant ikke verdi av felt";

        Query query = em.createQuery(" from " + kodeverkClass.getName());
        @SuppressWarnings("unchecked")
        List<Kodeliste> resultList = query.getResultList();

        Map<Class<?>, List<Kodeliste>> gruppert = resultList.stream().collect(Collectors.groupingBy(Object::getClass));

        assertThat(gruppert).as(kodeverkClass.getSimpleName()).isNotEmpty();

        for (Map.Entry<Class<?>, List<Kodeliste>> entry : gruppert.entrySet()) {
            Map<String, Kodeliste> koder = entry.getValue().stream()
                .collect(Collectors.toMap(Kodeliste::getKode, Function.identity()));

            Class<?> cls = entry.getKey();
            List.of(cls.getDeclaredFields()).stream()
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .filter(f -> f.getType().equals(cls))
                .forEach(f -> {
                    Kodeliste k;
                    try {
                        f.setAccessible(true);
                        k = (Kodeliste) f.get(null);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new AssertionError(feilFantIkke + f, e);
                    }
                    if (k.getKode() != null) {
                        assertThat(koder).as(k + "").containsKey(k.getKode());
                    }
                });
        }
    }
    
    public static Set<Class<?>> getEntityClasses(Predicate<Class<?>> filter) {
        Set<ManagedType<?>> managedTypes = entityManagerFactory.getMetamodel().getManagedTypes();
        return managedTypes.stream().map(javax.persistence.metamodel.Type::getJavaType).filter(c -> !Modifier.isAbstract(c.getModifiers())).filter(filter).collect(Collectors.toSet());
    }
}
