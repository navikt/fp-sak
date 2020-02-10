package no.nav.foreldrepenger.behandlingslager.diff;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

/**
 * Denne klassen kan traverse en Entity graph og trekk ut verdier som key/value.
 * <p>
 * Genererte verdier, {@link Id}, {@link Version}, {@link GeneratedValue} vil ignoreres.
 *
 * Bør opprette ny instans for hver gang det brukes til sammenligning.
 */
public class TraverseGraph {

    private final TraverseGraphConfig graphConfig;

    private final ListPositionEquality listPositionEq = new ListPositionEquality();

    public TraverseGraph(TraverseGraphConfig config) {
        this.graphConfig = Objects.requireNonNull(config, "config");
    }

    public TraverseResult traverse(Object target, String rootName) {
        Node rootNode = new Node(rootName, null, target);
        TraverseResult result = new TraverseResult();
        result.roots.put(rootNode, target);
        traverseDispatch(rootNode, target, result);

        return result;
    }

    public TraverseResult traverse(Object target) {
        if (target == null) {
            return new TraverseResult();
        }
        return traverse(target, target.getClass().getSimpleName());
    }

    private void traverseRecursiveInternal(Object obj, Node currentPath, TraverseResult result) {
        try {
            if (obj != null && result.cycleDetector.contains(obj)) {
                return;
            } else if (obj == null) {
                if (!graphConfig.isIgnoreNulls()) {
                    result.values.put(currentPath, null);
                }
                return;
            } else if (graphConfig.isLeaf(obj)) {
                result.values.put(currentPath, obj);
                return;
            }

            result.cycleDetector.add(obj);
        } catch (TraverseEntityGraphException t) {
            throw t;
        } catch (RuntimeException e) {
            throw new TraverseEntityGraphException("Kunne ikke lese grafen [" + currentPath + "]", e); //$NON-NLS-1$ //$NON-NLS-2$
        }

        if (obj instanceof Collection) { // NOSONAR
            traverseCollection(currentPath, (Collection<?>) obj, result);
        } else if (obj instanceof Map) { // NOSONAR
            traverseMap(currentPath, (Map<?, ?>) obj, result);
        } else {
            // hånter alt annet (vanlige felter)
            doTraverseRecursiveInternal(currentPath, result, obj);
        }

    }

    private void doTraverseRecursiveInternal(Node currentPath, TraverseResult result, Object obj) {
        if (obj instanceof HibernateProxy) {
            // PKMANTIS-1395 nødvendig for at lazy children av entitet loades
            obj = Hibernate.unproxy(obj);
        }

        if (!graphConfig.inclusionFilter.apply(obj)) {
            return;
        }

        Class<?> targetClass = obj.getClass();
        graphConfig.valider(currentPath, targetClass);
        
        Class<?> currentClass = targetClass;

        while (!graphConfig.isRoot(currentClass)) {
            for (final Field field : currentClass.getDeclaredFields()) {
                if (graphConfig.isTraverseField(field)) {
                    Node newPath = new Node(field.getName(), currentPath, obj);
                    try {
                        field.setAccessible(true);
                        Object value = field.get(obj);
                        traverseDispatch(newPath, value, result);
                    } catch (IllegalAccessException e) {
                        throw new IllegalArgumentException(String.valueOf(newPath), e);
                    }
                }
            }

            currentClass = currentClass.getSuperclass();
        }
    }

    /**
     * Håndter recursion for Map, Collection eller vanlige verdier. Skaper stabile nøkler i grafen.
     *
     * @param result
     */
    private void traverseDispatch(Node newPath, Object value, TraverseResult result) {
        // en sjelden grei bruk av instanceof. Garantert å håndtere alle varianter pga else til slutt
        if (value instanceof Collection) { // NOSONAR
            traverseCollection(newPath, (Collection<?>) value, result);
        } else if (value instanceof Map) { // NOSONAR
            traverseMap(newPath, (Map<?, ?>) value, result);
        } else {
            // hånter alt annet (vanlige felter)
            traverseRecursiveInternal(value, newPath, result);
        }
    }

    private void traverseMap(Node newPath, Map<?, ?> map, TraverseResult result) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Node collNode = new Node("{" + (entry.getKey()) + "}", newPath, map); //$NON-NLS-1$ //$NON-NLS-2$
            traverseRecursiveInternal(entry.getValue(), collNode, result);
        }
    }

    private void traverseCollection(Node newPath, Collection<?> value, TraverseResult result) {
        for (Object v : value) {
            String collectionKey;
            if (v instanceof IndexKey) {
                collectionKey = ((IndexKey) v).getIndexKey();
            } else {
                collectionKey = String.valueOf(listPositionEq.getKey(newPath, v));
            }

            Node collNode = new Node("[" + (collectionKey) + "]", newPath, v); //$NON-NLS-1$ //$NON-NLS-2$
            traverseRecursiveInternal(v, collNode, result);
        }
    }

    public static class TraverseResult {
        Map<Node, Object> values = new LinkedHashMap<>();
        Map<Node, Object> roots = new LinkedHashMap<>();
        Set<Object> cycleDetector = Collections.newSetFromMap(new IdentityHashMap<>());

        public Map<Node, Object> getValues() {
            return values;
        }

        public Map<Node, Object> getRoots() {
            return roots;
        }
    }
}
