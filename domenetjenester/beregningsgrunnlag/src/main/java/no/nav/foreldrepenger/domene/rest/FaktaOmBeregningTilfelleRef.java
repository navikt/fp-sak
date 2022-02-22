package no.nav.foreldrepenger.domene.rest;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;


/**
 * Marker tilfelle av fakta om beregning for å skille ulike implementasjoner for f.eks. oppdaterere og historikk.
 *
 */
@Repeatable(FaktaOmBeregningTilfelleRef.ContainerOfFaktaOmBeregningTilfelleRef.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD })
@Documented
public @interface FaktaOmBeregningTilfelleRef {

    /**
     * Kode-verdi som skiller ulike implementasjoner for ulike fakta om beregning tilfeller.
     * <p>
     * Må matche ett innslag i <code>FAKTA_OM_BEREGNING_TILFELLE</code> tabell for å kunne kjøres.
     *
     * @see FaktaOmBeregningTilfelle
     */
    String value() default "*";

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    class FaktaOmBeregningTilfelleRefLiteral extends AnnotationLiteral<FaktaOmBeregningTilfelleRef> implements FaktaOmBeregningTilfelleRef {

        private String navn;

        FaktaOmBeregningTilfelleRefLiteral(String navn) {
            this.navn = navn;
        }

        @Override
        public String value() {
            return navn;
        }

    }

    final class Lookup {

        private Lookup() {
        }

        /**
         * Kan brukes til å finne instanser blant angitte som matcher følgende kode, eller default '*' implementasjon. Merk at Instance bør være
         * injected med riktig forventet klassetype og @Any qualifier.
         */
        public static <I> Optional<I> find(Instance<I> instances, FaktaOmBeregningTilfelle faktaOmBeregningTilfelle) {
            return find(null, instances, faktaOmBeregningTilfelle.getKode());
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, String faktaOmBeregningTilfelleKode) {
            Objects.requireNonNull(instances, "instances");

            for (var faktaOmBeregningLiteral : coalesce(faktaOmBeregningTilfelleKode, "*")) {
                var inst = select(cls, instances, new FaktaOmBeregningTilfelleRefLiteral(faktaOmBeregningLiteral));
                if (inst.isResolvable()) {
                    return Optional.of(getInstance(inst));
                }
                if (inst.isAmbiguous()) {
                    throw new IllegalStateException("Har flere matchende instanser for klasse : " + cls.getName() + ", faktaOmBeregnintTilfelle=" + faktaOmBeregningLiteral);
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
            return Arrays.stream(vals).filter(v -> v != null).distinct().collect(Collectors.toList());
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
    @interface ContainerOfFaktaOmBeregningTilfelleRef {
        FaktaOmBeregningTilfelleRef[] value();
    }
}
