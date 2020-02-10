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

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef.BehandlingTypeRefLiteral;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral;
import no.nav.foreldrepenger.behandlingskontroll.StartpunktRef.ContainerOfStartpunktRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Marker type som implementerer interface {@link BehandlingSteg} for å skille ulike implementasjoner av samme steg for ulike
 * behandlingtyper.<br>
 *
 * NB: Settes kun dersom det er flere implementasjoner av med samme {@link BehandlingStegRef}.
 */
@Repeatable(ContainerOfStartpunktRef.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Documented
public @interface StartpunktRef {

    /**
     * Kode-verdi som skiller ulike implementasjoner for ulike behandling typer.
     * <p>
     * Må matche ett innslag i <code>BEHANDling_TYPE</code> tabell for å kunne kjøres.
     *
     * @see no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType
     */
    String value() default "*";

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    public static class StartpunktRefLiteral extends AnnotationLiteral<StartpunktRef> implements StartpunktRef {

        private String navn;

        public StartpunktRefLiteral() {
            this.navn = "*";
        }

        public StartpunktRefLiteral(String navn) {
            this.navn = (navn == null ? "*" : navn);
        }

        @Override
        public String value() {
            return navn;
        }
    }

    @SuppressWarnings("unchecked")
    public static final class Lookup {

        private Lookup() {
        }

        public static <I> Optional<I> find(Class<I> cls, String ytelseTypeKode, String behandlingType, String startpunktRef) {
            return find(cls, (CDI<I>) CDI.current(), ytelseTypeKode, behandlingType, startpunktRef);
        }

        public static <I> Optional<I> find(Class<I> cls, FagsakYtelseType ytelseTypeKode, BehandlingType behandlingType, String startpunktRef) {
            return find(cls, (CDI<I>) CDI.current(), ytelseTypeKode, behandlingType, startpunktRef);
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, FagsakYtelseType ytelseTypeKode, BehandlingType behandlingType,
                                           String startpunktRef) {
            return find(cls, instances,
                ytelseTypeKode == null ? null : ytelseTypeKode.getKode(),
                behandlingType == null ? null : behandlingType.getKode(),
                startpunktRef);
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, String fagsakYtelseType, String behandlingType, String startpunktRef) { // NOSONAR
            Objects.requireNonNull(instances, "instances");

            for (var fagsakLiteral : coalesce(fagsakYtelseType, "*")) {
                var inst = select(cls, instances, new FagsakYtelseTypeRefLiteral(fagsakLiteral));
                if (inst.isUnsatisfied()) {
                    continue;
                } else {
                    for (var behandlingLiteral : coalesce(behandlingType, "*")) {
                        var binst = select(cls, inst, new BehandlingTypeRefLiteral(behandlingLiteral));
                        if (binst.isUnsatisfied()) {
                            continue;
                        }
                        for (var start : coalesce(startpunktRef, "*")) {
                            var sinst = select(cls, binst, new StartpunktRefLiteral(start));
                            if (sinst.isResolvable()) {
                                return Optional.of(getInstance(sinst));
                            } else {
                                if (sinst.isAmbiguous()) {
                                    throw new IllegalStateException("Har flere matchende instanser for klasse : " + cls.getName() + ", fagsakType="
                                        + fagsakLiteral + ", behandlingType=" + behandlingLiteral + ", startpunktRef=" + start);
                                }
                            }
                        }
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

        private static List<String> coalesce(String... vals) {
            return Arrays.asList(vals).stream().filter(v -> v != null).distinct().collect(Collectors.toList());
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
    public @interface ContainerOfStartpunktRef {
        StartpunktRef[] value();
    }
}
