package no.nav.foreldrepenger.behandlingslager.behandling;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import javax.persistence.Entity;

/**
 * Marker type som implementerer interface {@link StartpunktUtleder}.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface GrunnlagRef {

    String IAY_GRUNNLAG = "InntektArbeidYtelseGrunnlag";
    String MEDLEM_GRUNNLAG = "MedlemskapAggregat";
    String YTELSE_FORDELING_GRUNNLAG = "YtelseFordelingAggregat";

    /**
     * Settes til navn på forretningshendelse slik det defineres i KODELISTE-tabellen.
     */
    String value();

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    class GrunnlagRefLiteral extends AnnotationLiteral<GrunnlagRef> implements GrunnlagRef {

        private String navn;

        public GrunnlagRefLiteral(String navn) {
            this.navn = navn;
        }

        @Override
        public String value() {
            return navn;
        }
    }


    @SuppressWarnings("unchecked")
    final class Lookup {

        private Lookup() {
        }

        public static <I> Optional<I> find(Class<I> lookupClass, String aggregatClassName) {
            return find(lookupClass, (CDI<I>) CDI.current(), aggregatClassName);
        }

        public static <I> Optional<I> find(Class<I> cls, Class<?> aggregatClass) {
            return find(cls, (CDI<I>) CDI.current(), getName(aggregatClass));
        }

        private static String getName(Class<?> aggregat) {
            return aggregat.isAnnotationPresent(Entity.class) ? aggregat.getAnnotation(Entity.class).name()  : aggregat.getSimpleName();
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, Class<?> aggregatClass) {
            return find(cls, instances, getName(aggregatClass));
        }

        /**
         * Kan brukes til å finne instanser blant angitte som matcher følgende kode, eller default '*' implementasjon. Merk at Instance bør være
         * injected med riktig forventet klassetype og @Any qualifier.
         */
        public static <I> Optional<I> find(Instance<I> instances, Class<?> aggregatClass) {
            return find(null, instances, getName(aggregatClass));
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, String aggregatClassName) {
            Objects.requireNonNull(instances, "instances");

            for (var grunnlagLiteral : coalesce(aggregatClassName, "*")) {
                var inst = select(cls, instances, new GrunnlagRefLiteral(grunnlagLiteral));
                if (inst.isResolvable()) {
                    return Optional.of(getInstance(inst));
                }
                if (inst.isAmbiguous()) {
                    throw new IllegalStateException("Har flere matchende instanser for klasse : " + cls.getName() + ", grunnlag=" + grunnlagLiteral);
                }
            }

            return Optional.empty();
        }

        private static <I> Instance<I> select(Class<I> cls, Instance<I> instances, Annotation anno) {
            return cls != null
                ? instances.select(cls, anno)
                : instances.select(anno);
        }

        private static <I> I getInstance(Instance<I> inst) {
            var i = inst.get();
            if (i.getClass().isAnnotationPresent(Dependent.class)) {
                throw new IllegalStateException(
                    "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + i.getClass());
            }
            return i;
        }

        private static List<String> coalesce(String... vals) {
            return Arrays.stream(vals).filter(Objects::nonNull).distinct().toList();
        }

    }

    /**
     * container for repeatable annotations.
     *
     * @see https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD })
    @Documented
    @interface ContainerOfGrunnlagRef {
        GrunnlagRef[] value();
    }
}
