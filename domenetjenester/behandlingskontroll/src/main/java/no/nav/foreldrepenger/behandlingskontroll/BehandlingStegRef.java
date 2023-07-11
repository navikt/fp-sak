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

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef.ContainerOfBehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef.BehandlingTypeRefLiteral;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Marker type som implementerer interface {@link BehandlingSteg}.<br>
 */
@Repeatable(ContainerOfBehandlingStegRef.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Documented
public @interface BehandlingStegRef {

    /**
     * Kode-verdi som identifiserer behandlingsteget.
     * <p>
     * Må matche ett innslag i <code>BEHANDLING_STEG_TYPE</code> tabell for å kunne
     * kjøres.
     *
     * @see BehandlingStegType
     */
    BehandlingStegType value();

    /**
     * AnnotationLiteral som kan brukes i CDI søk.
     * <p>
     * Eks. for bruk i:<br>
     * {@link CDI#current#select(javax.enterprise.util.TypeLiteral,
     * Annotation...)}.
     */
    class BehandlingStegRefLiteral extends AnnotationLiteral<BehandlingStegRef> implements BehandlingStegRef {

        private BehandlingStegType stegType;

        public BehandlingStegRefLiteral(BehandlingStegType stegType) {
            this.stegType = stegType;
        }


        @Override
        public BehandlingStegType value() {
            return stegType;
        }

    }

    @SuppressWarnings("unchecked")
    final class Lookup {

        private Lookup() {
        }

        public static <I> Optional<I> find(Class<I> cls, FagsakYtelseType ytelseType, BehandlingType behandlingType,
                BehandlingStegType behandlingStegRef) {
            return find(cls, (CDI<I>) CDI.current(), ytelseType, behandlingType, behandlingStegRef);
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, FagsakYtelseType ytelseType, BehandlingType behandlingType,
                BehandlingStegType behandlingStegRef) {
            Objects.requireNonNull(instances, "instances");

            for (var fagsakLiteral : coalesce(ytelseType, FagsakYtelseType.UDEFINERT)) {
                var inst = select(cls, instances, new FagsakYtelseTypeRefLiteral(fagsakLiteral));
                if (inst.isUnsatisfied()) {
                    continue;
                }
                for (var behandlingLiteral : coalesce(behandlingType, BehandlingType.UDEFINERT)) {
                    var binst = select(cls, inst, new BehandlingTypeRefLiteral(behandlingLiteral));
                    if (binst.isUnsatisfied()) {
                        continue;
                    }
                    var cinst = select(cls, binst, new BehandlingStegRefLiteral(behandlingStegRef));
                    if (cinst.isResolvable()) {
                        return Optional.of(getInstance(cinst));
                    }
                    if (cinst.isAmbiguous()) {
                        throw new IllegalStateException("Har flere matchende instanser for klasse : " + cls.getName() + ", fagsakType="
                            + fagsakLiteral + ", behandlingType=" + behandlingLiteral + ", behandlingStegRef=" + behandlingStegRef);
                    }
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

        private static List<FagsakYtelseType> coalesce(FagsakYtelseType... vals) {
            return Arrays.stream(vals).filter(Objects::nonNull).distinct().toList();
        }

        private static List<BehandlingType> coalesce(BehandlingType... vals) {
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
    @Target({ ElementType.TYPE })
    @Documented
    @interface ContainerOfBehandlingStegRef {
        BehandlingStegRef[] value();
    }

}
