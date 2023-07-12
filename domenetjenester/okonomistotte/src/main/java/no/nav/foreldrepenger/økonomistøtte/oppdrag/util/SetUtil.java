package no.nav.foreldrepenger.økonomistøtte.oppdrag.util;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class SetUtil {

    private SetUtil() {
    }

    @SafeVarargs
    public static <T> Set<T> union(Set<T>... settene) {
        Set<T> resultat = new HashSet<>();
        for (var sett : settene) {
            resultat.addAll(sett);
        }
        return resultat;
    }

    @SafeVarargs
    public static <T extends Comparable<T>> SortedSet<T> sortertUnionOfKeys(Map<T, ?>... maps) {
        SortedSet<T> resultat = new TreeSet<>();
        for (var map : maps) {
            resultat.addAll(map.keySet());
        }
        return resultat;
    }

    @SafeVarargs
    public static <T> SortedSet<T> sortertUnion(Comparator<T> comparator, Set<T>... settene) {
        var resultat = new TreeSet<>(comparator);
        for (var sett : settene) {
            resultat.addAll(sett);
        }
        return resultat;
    }


}
