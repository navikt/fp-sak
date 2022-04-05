package no.nav.foreldrepenger.behandlingskontroll;

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
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef.ContainerOfBehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Marker type som implementerer interface {@link BehandlingSteg} for å skille
 * ulike implementasjoner av samme steg for ulike behandlingtyper.<br>
 *
 * NB: Settes kun dersom det er flere implementasjoner av med samme
 * {@link BehandlingStegRef}.
 */
@Repeatable(ContainerOfBehandlingTypeRef.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Documented
public @interface BehandlingTypeRef {

    /**
     * Kode-verdi som skiller ulike implementasjoner for ulike behandling typer.
     * <p>
     * Må matche ett innslag i <code>BEHANDling_TYPE</code> tabell for å kunne
     * kjøres.
     *
     * @see BehandlingType
     */
    BehandlingType value() default BehandlingType.UDEFINERT;

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    class BehandlingTypeRefLiteral extends AnnotationLiteral<BehandlingTypeRef> implements BehandlingTypeRef {

        private BehandlingType behandlingType;

        public BehandlingTypeRefLiteral() {
            this.behandlingType = BehandlingType.UDEFINERT;
        }

        public BehandlingTypeRefLiteral(BehandlingType behandlingType) {
            this.behandlingType = (behandlingType== null ? BehandlingType.UDEFINERT : behandlingType);
        }

        @Override
        public BehandlingType value() {
            return behandlingType;
        }
    }

    @SuppressWarnings("unchecked")
    final class Lookup {

        private Lookup() {
        }

        public static <I> Optional<I> find(Class<I> cls, FagsakYtelseType ytelseType, BehandlingType behandlingType) {
            return find(cls, (CDI<I>) CDI.current(), ytelseType, behandlingType);
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, FagsakYtelseType ytelseType, BehandlingType behandlingType) { // NOSONAR
            Objects.requireNonNull(instances, "instances");

            for (var fagsakLiteral : coalesce(ytelseType, FagsakYtelseType.UDEFINERT)) {
                var inst = select(cls, instances, new FagsakYtelseTypeRefLiteral(ytelseType));

                if (inst.isUnsatisfied()) {
                    continue;
                }
                for (var behandlingLiteral : coalesce(behandlingType, BehandlingType.UDEFINERT)) {
                    var binst = select(cls, inst, new BehandlingTypeRefLiteral(behandlingLiteral));
                    if (binst.isResolvable()) {
                        return Optional.of(getInstance(binst));
                    }
                    if (binst.isAmbiguous()) {
                            throw new IllegalStateException("Har flere matchende instanser for klasse : " + cls.getName() + ", fagsakType="
                                    + fagsakLiteral + ", behandlingType=" + behandlingLiteral);
                        }
                }

            }
            return Optional.empty();
        }

        private static <I> I getInstance(Instance<I> inst) {
            var i = inst.get();
            if (i.getClass().isAnnotationPresent(Dependent.class)) {
                throw new IllegalStateException(
                        "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + i.getClass());
            }
            return i;
        }

        private static List<FagsakYtelseType> coalesce(FagsakYtelseType... vals) {
            return Arrays.asList(vals).stream().filter(v -> v != null).distinct().collect(Collectors.toList());
        }

        private static List<BehandlingType> coalesce(BehandlingType... vals) {
            return Arrays.asList(vals).stream().filter(v -> v != null).distinct().collect(Collectors.toList());
        }

        private static <I> Instance<I> select(Class<I> cls, Instance<I> instances, Annotation anno) {
            return cls != null
                    ? instances.select(cls, anno)
                    : instances.select(anno);
        }

    }

    /**
     * container for repeatable annotations.
     *
     * @see https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
    @Documented
    @interface ContainerOfBehandlingTypeRef {
        BehandlingTypeRef[] value();
    }
}
