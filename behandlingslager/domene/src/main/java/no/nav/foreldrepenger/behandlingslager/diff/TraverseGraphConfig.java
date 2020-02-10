package no.nav.foreldrepenger.behandlingslager.diff;

import static java.util.Arrays.asList;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.threeten.extra.Interval;

import javassist.Modifier;

public class TraverseGraphConfig {

    /** Return alltid true, dvs. vil aldri filtrere bort noe. */
    public static final Function<Object, Boolean> NO_FILTER = new Function<Object, Boolean>() {
        @Override
        public Boolean apply(Object t) {
            return Boolean.TRUE;
        }
    };

    /** Final klasser som ikke trenger videre forklaring. For raskest oppslag. */
    private static final Set<Class<?>> LEAVES_FINAL = Set.of(
        String.class, Character.class, Character.TYPE, //
        Long.class, Double.class, Integer.class, Short.class, Byte.class, Boolean.class, //
        Long.TYPE, Double.TYPE, Integer.TYPE, Short.TYPE, Byte.TYPE, Boolean.TYPE, //
        BigInteger.class, BigDecimal.class, //
        LocalDate.class, LocalDateTime.class, Interval.class, OffsetDateTime.class, ZonedDateTime.class, Instant.class,
        UUID.class, URI.class, URL.class //
    );

    /** Rot klasser som ikke skal inspiseres i et hierarki. */
    private static final Set<Class<?>> ROOTS_CLASSES = Set.of(Object.class);

    /**
     * Ikke final - men Interfacer/Abstract klasser som fanger store grupper av LEAF objekter (eks. Temporal --
     * LocalDate, Number -- Long, osv).
     */
    private static final Set<Class<?>> LEAVES_EXTENDABLE = Set.of(Number.class, Enum.class, TemporalAccessor.class, TemporalAmount.class, TemporalField.class,
        TraverseValue.class);

    private Set<Class<?>> leafFinalClasses = LEAVES_FINAL;
    private Set<Class<?>> leafExtendableClasses = LEAVES_EXTENDABLE;
    private Set<Class<?>> rootClasses = ROOTS_CLASSES;

    private boolean ignoreNulls;

    private boolean onlyCheckTrackedFields;

    /** Filter - returnerer false dersom objekt ikke skal sammmenlignes. Default sammenligner alt. */
    Function<Object, Boolean> inclusionFilter = NO_FILTER;

    public boolean isMappedField(Field fld) {
        int mods = fld.getModifiers();
        if (Modifier.isStatic(mods) || Modifier.isTransient(mods) || Modifier.isVolatile(mods)) {
            return false;
        }
        return true;
    }
    
    public void addRootClasses(Class<?>... moreRootClasses) {
        this.rootClasses = new HashSet<>(this.rootClasses);
        this.rootClasses.addAll(asList(moreRootClasses));
    }

    public void setIgnoreNulls(boolean ignoreNulls) {
        this.ignoreNulls = ignoreNulls;
    }

    public void setOnlyCheckTrackedFields(boolean onlyCheckTrackedFields) {
        this.onlyCheckTrackedFields = onlyCheckTrackedFields;
    }

    public boolean isTraverseField(final Field field) {
        return (isOnlyCheckTrackedFields() && isChangeTrackedField(field))
            || (!isOnlyCheckTrackedFields() && isMappedField(field));
    }

    public boolean isLeaf(Object obj) {
        Class<?> targetClass = obj.getClass();
        if (leafFinalClasses.contains(targetClass)) {
            return true;
        } else {
            for (Class<?> leaf : leafExtendableClasses) {
                if (leaf.isAssignableFrom(targetClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isChangeTrackedField(Field fld) {
        return fld.isAnnotationPresent(ChangeTracked.class);
    }
    public boolean isRoot(Class<?> cls) {
        return cls == null || rootClasses.contains(cls);
    }

    @SafeVarargs
    public final void addLeafClasses(Class<?>... leafClasses) {
        List<Class<?>> newLeafClasses = asList(leafClasses);

        this.leafExtendableClasses = new HashSet<>(this.leafExtendableClasses);
        this.leafExtendableClasses.addAll(newLeafClasses);

        this.leafFinalClasses = new HashSet<>(this.leafFinalClasses);
        this.leafFinalClasses.addAll(newLeafClasses);

    }

    public void setInclusionFilter(Function<Object, Boolean> inclusionFilter) {
        this.inclusionFilter = Objects.requireNonNull(inclusionFilter, "inclusionFilter");
    }

    public boolean isIgnoreNulls() {
        return ignoreNulls;
    }

    public boolean isOnlyCheckTrackedFields() {
        return onlyCheckTrackedFields;
    }

    @SuppressWarnings("unused")
    public void valider(Node currentPath, Class<?> targetClass) {
        // template method, ingen sjekk her
    }
    
}