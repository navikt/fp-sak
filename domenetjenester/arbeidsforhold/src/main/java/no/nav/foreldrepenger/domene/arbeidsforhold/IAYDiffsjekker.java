package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.diff.DiffEntity;
import no.nav.foreldrepenger.behandlingslager.diff.Node;
import no.nav.foreldrepenger.behandlingslager.diff.Pair;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraph;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraphConfig;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class IAYDiffsjekker {
    private DiffEntity diffEntity;
    private TraverseGraph traverseGraph;

    public IAYDiffsjekker() {
        this(true);
    }

    public IAYDiffsjekker(boolean onlyCheckTrackedFields) {

        var config = new TraverseGraphConfig();
        config.setIgnoreNulls(true);
        config.setOnlyCheckTrackedFields(onlyCheckTrackedFields);
        config.setInclusionFilter(TraverseGraphConfig.NO_FILTER);

        config.addLeafClasses(DatoIntervallEntitet.class);

        this.traverseGraph = new TraverseGraph(config);
        this.diffEntity = new DiffEntity(traverseGraph);
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

    public static Optional<Boolean> eksistenssjekkResultat(Optional<?> eksisterende, Optional<?> nytt) {
        if (eksisterende.isEmpty() && nytt.isEmpty()) {
            return Optional.of(Boolean.FALSE);
        }
        if (eksisterende.isPresent() && nytt.isEmpty()) {
            return Optional.of(Boolean.TRUE);
        }
        if (eksisterende.isEmpty() && nytt.isPresent()) { // NOSONAR  - "redundant" her er false pos.
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }
}
