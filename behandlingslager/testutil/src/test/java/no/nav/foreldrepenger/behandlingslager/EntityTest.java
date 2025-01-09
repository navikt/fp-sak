package no.nav.foreldrepenger.behandlingslager;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Persistence;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Type;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import no.nav.foreldrepenger.dbstoette.JpaExtension;

/**
 * Sjekker alle entiteter er mappet korrekt. Ligger i web slik at den fanger
 * alle orm filer lagt i ulike moduler.
 */
@ExtendWith(JpaExtension.class)
class EntityTest {

    private static final EntityManagerFactory entityManagerFactory;
    static {
        // Kan ikke skrus på nå - trigger på CHAR kolonner som kunne vært VARCHAR. Må
        // fikses først
        // System.setProperty("hibernate.hbm2ddl.auto", "validate");
        try {
            // trenger å konfigurere opp jndi etc.
            //TestDatabaseInit.settJdniOppslag();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        entityManagerFactory = Persistence.createEntityManagerFactory("pu-default");
    }

    private EntityManager em = entityManagerFactory.createEntityManager();

    @AfterAll
    public static void teardown() {
        System.clearProperty("hibernate.hbm2ddl.auto");
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

    private static boolean isDoubleOrFloat(Class<?> javaType) {
        return javaType == Double.class || javaType == Float.class || javaType.isPrimitive() && (javaType == Double.TYPE || javaType == Float.TYPE);
    }

    private static <V extends Annotation> V getInheritedAnnotation(Class<?> cls, Class<V> ann) {
        V res = null;
        while (res == null && cls != Object.class) {
            res = cls.getDeclaredAnnotation(ann);
            cls = cls.getSuperclass();
        }
        return res;
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void skal_ha_registrert_alle_entiteter_i_orm_xml(Class<?> entityClass) {
        try {
            entityManagerFactory.getMetamodel().managedType(entityClass);
        } catch (IllegalArgumentException e) {
            assertThat(e).as("Er ikke registrert i orm, må ryddes fra koden: " + entityClass.getSimpleName()).isNull(); // Skal alltid feile, kun for å utvide melding
            throw e;
        }
    }

    //@Disabled("Venter til etter migrering av aliased tables")
    @ParameterizedTest
    @MethodSource("parameters")
    public void sjekk_felt_mapping_primitive_felt_i_entiteter_må_ha_not_nullable_i_db(Class<?> entityClass) throws Exception {
        var managedType = entityManagerFactory.getMetamodel().managedType(entityClass);

        for (Attribute<?, ?> att : managedType.getAttributes()) {
            var javaType = att.getJavaType();
            if (javaType.isPrimitive()) {

                var member = att.getJavaMember();
                var field = member.getDeclaringClass().getDeclaredField(member.getName());
                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }

                var tableName = getInheritedAnnotation(entityClass, Table.class).name();
                var columnName = field.getDeclaredAnnotation(Column.class).name();
                var singleResult = getNullability(tableName, columnName);

                if (singleResult != null) {
                    var warn = "Primitiv " + member + " kan ha null i db. Kan medføre en smell ved lasting";
                    assertThat(singleResult).as(warn).isEqualTo("N");
                } else {
                    // forventer noe Dvh stuff som er Ok
                    assertThat(entityClass.getName()).endsWith("Dvh");
                }
            }
        }
    }

    // TODO: Slå på og gjennomgå resultatene
    @Disabled("Venter til etter migrering av aliased tables")
    @ParameterizedTest
    @MethodSource("parameters")
    public void sjekk_felt_ikke_primitive_wrappere_kan_ikke_være_not_nullable_i_db(Class<?> entityClass) throws Exception {
        var managedType = entityManagerFactory.getMetamodel().managedType(entityClass);

        if (Modifier.isAbstract(entityClass.getModifiers())) {
            return;
        }

        for (Attribute<?, ?> att : managedType.getAttributes()) {
            var member = att.getJavaMember();
            var field = member.getDeclaringClass().getDeclaredField(member.getName());

            if (Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            var id = field.getDeclaredAnnotation(Id.class);
            if (id != null) {
                continue;
            }

            var tableName = getTableName(entityClass, field);
            var column = field.getDeclaredAnnotation(Column.class);
            var joinColumn = field.getDeclaredAnnotation(JoinColumn.class);
            if (column == null && joinColumn == null) {
                continue;
            }
            var columnName = column != null ? column.name() : joinColumn.name();
            var nullable = column != null ? column.nullable() : joinColumn.nullable();
            var singleResult = getNullability(tableName, columnName);

            var warn = "Felt " + member
                    + " kan ikke ha null i db. Kan medføre en smell ved skriving. Bedre å bruke primitiv hvis kan (husk sjekk med innkommende kilde til data)";
            if (nullable) {
                assertThat(singleResult).as(warn).isEqualTo("Y");
            } else {
                assertThat(singleResult).as(warn).isEqualTo("N");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String getNullability(String tableName, String columnName) {
        List<String> result = em.createNativeQuery(
                "SELECT NULLABLE FROM ALL_TAB_COLS WHERE COLUMN_NAME=upper(:col) AND TABLE_NAME=upper(:table) AND OWNER=SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')")
                .setParameter("table", tableName)
                .setParameter("col", columnName)
                .getResultList();

        return result.isEmpty() ? null : String.valueOf(result.get(0));
    }

    private String getTableName(Class<?> entityClass, Field field) {
        var clazz = entityClass;
        if (field.getDeclaredAnnotation(OneToMany.class) != null) {
            var parameterizedType = (ParameterizedType) field.getGenericType();
            var type = parameterizedType.getActualTypeArguments()[0];
            clazz = (Class<?>) type;
        }
        return getInheritedAnnotation(clazz, Table.class).name();
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void sjekk_felt_ikke_er_Float_eller_Double(Class<?> entityClass) throws Exception {
        var managedType = entityManagerFactory.getMetamodel().managedType(entityClass);

        for (Attribute<?, ?> att : managedType.getAttributes()) {

            var javaType = att.getJavaType();

            if (isDoubleOrFloat(javaType)) {

                var member = att.getJavaMember();
                var field = member.getDeclaringClass().getDeclaredField(member.getName());
                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }

                var warn = "Primitiv wrapper (Float, Double) " + member
                        + " bør ikke brukes for felt som mappes til db.  Vil gi IEEE754 avrundingsfeil";

                assertThat(member).as(warn).isNull();
            }
        }
    }

}
