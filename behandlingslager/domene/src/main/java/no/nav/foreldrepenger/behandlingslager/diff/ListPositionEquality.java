package no.nav.foreldrepenger.behandlingslager.diff;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class ListPositionEquality {
    private final Map<Node, AtomicInteger> equalsNodeCounter = new HashMap<>();
    private final Map<Object, NodeWrap> equalsMap = new HashMap<>();

    int getKey(Node node, Object o) {
        var counter = equalsNodeCounter.computeIfAbsent(node, n -> new AtomicInteger());
        return equalsMap.computeIfAbsent(o, v -> new NodeWrap(node, counter.getAndIncrement())).pos();
    }

    record NodeWrap(Node root, int pos) {
    }
}
