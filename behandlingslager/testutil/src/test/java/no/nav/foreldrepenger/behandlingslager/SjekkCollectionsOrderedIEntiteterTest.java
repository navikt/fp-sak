package no.nav.foreldrepenger.behandlingslager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.metamodel.Type;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.dbstoette.JpaExtension;

/** Lagt til web for å sjekke orm filer fra alle moduler. */
@ExtendWith(JpaExtension.class)
class SjekkCollectionsOrderedIEntiteterTest {

    private static final EntityManagerFactory entityManagerFactory;

    static {
        entityManagerFactory = Persistence.createEntityManagerFactory("pu-default");
    }

    private static Collection<Class<?>> parameters() {

        var baseEntitetSubklasser = getEntityClasses(BaseEntitet.class::isAssignableFrom);
        var entityKlasser = getEntityClasses(c -> c.isAnnotationPresent(Entity.class));

        Collection<Class<?>> params = new HashSet<>(baseEntitetSubklasser);
        assertThat(params).isNotEmpty();

        params.addAll(entityKlasser);
        assertThat(params).isNotEmpty();

        return params;
    }

    public static Set<Class<?>> getEntityClasses(Predicate<Class<?>> filter) {
        var managedTypes = entityManagerFactory.getMetamodel().getManagedTypes();
        return managedTypes.stream().map(Type::getJavaType).filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .filter(filter).collect(Collectors.toSet());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void sjekk_alle_lister_er_ordered(Class<?> entityClass) {
        for (var f : entityClass.getDeclaredFields()) {
            if (Collection.class.isAssignableFrom(f.getType())) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    var paramType = (ParameterizedType) f.getGenericType();
                    var cls = (Class<?>) paramType.getActualTypeArguments()[0];
                    assumeTrue(IndexKey.class.isAssignableFrom(cls));
                    assertThat(IndexKey.class).as(f + " definerer en liste i " + entityClass.getSimpleName()).isAssignableFrom(cls);
                }
            }
        }
    }

}
