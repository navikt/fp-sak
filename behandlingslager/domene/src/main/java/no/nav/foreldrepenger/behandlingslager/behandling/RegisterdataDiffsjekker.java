package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

import no.nav.foreldrepenger.behandlingslager.TraverseEntityGraphFactory;
import no.nav.foreldrepenger.behandlingslager.diff.DiffEntity;
import no.nav.foreldrepenger.behandlingslager.diff.Node;
import no.nav.foreldrepenger.behandlingslager.diff.Pair;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraph;

public class RegisterdataDiffsjekker {
    private DiffEntity diffEntity;
    private TraverseGraph traverseGraph;

    public RegisterdataDiffsjekker() {
        this(true);
    }

    public RegisterdataDiffsjekker(boolean onlyCheckTrackedFields) {
        traverseGraph = TraverseEntityGraphFactory.build(onlyCheckTrackedFields);
        diffEntity = new DiffEntity(traverseGraph);
    }

    public  <T> boolean erForskjellPå(Collection<T> list1, Collection<T> list2) {
        return list1.size() != list2.size() || !finnForskjellerPåLister(list1, list2).isEmpty();
    }

    public  <T> boolean erForskjellPåFiltrert(Collection<T> list1, Collection<T> list2, Predicate<Map.Entry<Node, Pair>> filter) {
        if (list1.size() != list2.size()) {
            return true;
        }
        var forskjeller = finnForskjellerPåLister(list1, list2);
        if (forskjeller.isEmpty()) {
            return false;
        }
        return forskjeller.entrySet().stream().anyMatch(filter);
    }

    private  <T> Map<Node, Pair> finnForskjellerPåLister(Collection<T> list1, Collection<T> list2) {
        return finnForskjellerPå(list1, list2);
    }

    public boolean erForskjellPå(Object object1, Object object2) {
        return !finnForskjellerPå(object1, object2).isEmpty();
    }

    private Map<Node, Pair> finnForskjellerPå(Object object1, Object object2) {
        var diff = diffEntity.diff(object1, object2);
        return diff.getLeafDifferences();
    }

    public DiffEntity getDiffEntity() {
        return diffEntity;
    }
}
