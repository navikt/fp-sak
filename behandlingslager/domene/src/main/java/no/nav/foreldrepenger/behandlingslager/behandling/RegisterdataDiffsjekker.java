package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    public <T extends Comparable<? super T>> boolean erForskjellPå(List<T> list1, List<T> list2) {
        var leafDifferences = finnForskjellerPåLister(list1, list2);
        return leafDifferences.size() > 0;
    }

    public <T extends Comparable<? super T>> Map<Node, Pair> finnForskjellerPåLister(List<T> list1, List<T> list2) {
        Collections.sort(list1);
        Collections.sort(list2);
        return finnForskjellerPå(list1, list2);
    }

    public boolean erForskjellPå(Object object1, Object object2) {
        return !finnForskjellerPå(object1, object2).isEmpty();
    }

    public Map<Node, Pair> finnForskjellerPå(Object object1, Object object2) {
        var diff = diffEntity.diff(object1, object2);
        return diff.getLeafDifferences();
    }

    public DiffEntity getDiffEntity() {
        return diffEntity;
    }
}
