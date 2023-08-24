package no.nav.foreldrepenger.domene.rest;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;

import java.lang.annotation.*;
import java.util.Objects;
import java.util.Optional;


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
    FaktaOmBeregningTilfelle value();

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    class FaktaOmBeregningTilfelleRefLiteral extends AnnotationLiteral<FaktaOmBeregningTilfelleRef> implements FaktaOmBeregningTilfelleRef {

        private FaktaOmBeregningTilfelle tilfelle;

        FaktaOmBeregningTilfelleRefLiteral(FaktaOmBeregningTilfelle tilfelle) {
            this.tilfelle = tilfelle;
        }

        @Override
        public FaktaOmBeregningTilfelle value() {
            return tilfelle;
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
            Objects.requireNonNull(instances, "instances");


            var inst = select(instances, new FaktaOmBeregningTilfelleRefLiteral(faktaOmBeregningTilfelle));
            if (inst.isResolvable()) {
                return Optional.of(getInstance(inst));
            }
            if (inst.isAmbiguous()) {
                throw new IllegalStateException("Har flere matchende instanser for faktaOmBeregningTilfelle=" + faktaOmBeregningTilfelle);
            }


            return Optional.empty();
        }

        private static <I> Instance<I> select(Instance<I> instances, Annotation anno) {
            return instances.select(anno);
        }

        private static <I> I getInstance(Instance<I> inst) {
            var i = inst.get();
            if (i.getClass().isAnnotationPresent(Dependent.class)) {
                throw new IllegalStateException(
                    "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + i.getClass());
            }
            return i;
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
