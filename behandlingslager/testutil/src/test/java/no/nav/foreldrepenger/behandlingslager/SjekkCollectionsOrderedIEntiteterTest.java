package no.nav.foreldrepenger.behandlingslager;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.ManagedType;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.dbstoette.DatasourceConfiguration;
import no.nav.vedtak.felles.lokal.dbstoette.DBConnectionProperties;
import no.nav.vedtak.felles.lokal.dbstoette.DatabaseStøtte;

/** Lagt til web for å sjekke orm filer fra alle moduler. */
@RunWith(Parameterized.class)
public class SjekkCollectionsOrderedIEntiteterTest {

    private static final EntityManagerFactory entityManagerFactory;

    static {
        try {
            // trenger å konfigurere opp jndi etc.
            DBConnectionProperties connectionProperties = DBConnectionProperties.finnDefault(DatasourceConfiguration.UNIT_TEST.get()).get();
            DatabaseStøtte.settOppJndiForDefaultDataSource(Collections.singletonList(connectionProperties));
        } catch (FileNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
        entityManagerFactory = Persistence.createEntityManagerFactory("pu-default");
    }

    private String name;
    private Class<?> entityClass;

    public SjekkCollectionsOrderedIEntiteterTest(String name, Class<?> entityClass) {
        this.name = name;
        this.entityClass = entityClass;
    }

    @org.junit.runners.Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() throws Exception {

        Set<Class<?>> baseEntitetSubklasser = getEntityClasses(BaseEntitet.class::isAssignableFrom);
        Set<Class<?>> entityKlasser = getEntityClasses(c -> c.isAnnotationPresent(Entity.class));
        Map<String, Object[]> params = new LinkedHashMap<>();

        for (Class<?> c : baseEntitetSubklasser) {
            params.put(c.getName(), new Object[]{c.getSimpleName(), c});
        }
        assertThat(params).isNotEmpty();

        for (Class<?> c : entityKlasser) {
            params.put(c.getName(), new Object[]{c.getSimpleName(), c});
        }
        assertThat(params).isNotEmpty();

        return params.values();
    }

    public static Set<Class<?>> getEntityClasses(Predicate<Class<?>> filter) {
        Set<ManagedType<?>> managedTypes = entityManagerFactory.getMetamodel().getManagedTypes();
        return managedTypes.stream().map(javax.persistence.metamodel.Type::getJavaType).filter(c -> !Modifier.isAbstract(c.getModifiers())).filter(filter).collect(Collectors.toSet());
    }

    @Test
    public void sjekk_alle_lister_er_ordered() throws Exception {
        for (Field f : entityClass.getDeclaredFields()) {
            if (Collection.class.isAssignableFrom(f.getType())) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    ParameterizedType paramType = (ParameterizedType) f.getGenericType();
                    Class<?> cls = (Class<?>) paramType.getActualTypeArguments()[0];
                    Assume.assumeTrue(IndexKey.class.isAssignableFrom(cls));
                    assertThat(IndexKey.class).as(f + " definerer en liste i " + name).isAssignableFrom(cls);
                }
            }
        }
    }

}
