package no.nav.foreldrepenger.behandlingslager.behandling;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

// Bruker en primitiv variant av Composite for å kunne vurderes enkeltvis (løvnode) og sammensatt (rotnode)
public class EndringsresultatSnapshot {

    private Object grunnlagId;
    private Class<?> grunnlagKlasse;

    private List<EndringsresultatSnapshot> children = emptyList();

    // Brukes som Composite-rotnode
    private EndringsresultatSnapshot() {
        this.grunnlagKlasse = this.getClass(); // rot
        children = new ArrayList<>();

    }

    // Brukes som Composite-løvnode
    private EndringsresultatSnapshot(Class<?> grunnlagKlasse, Object grunnlagId) {
        this.grunnlagKlasse = grunnlagKlasse;
        this.grunnlagId = grunnlagId;
    }

    // Oppretter Composite-rotnode
    public static EndringsresultatSnapshot opprett() {
        return new EndringsresultatSnapshot();
    }

    // Oppretter Composite-løvnode
    public static EndringsresultatSnapshot medSnapshot(Class<?> aggregat, Object id) {
        return new EndringsresultatSnapshot(aggregat, id);
    }

    // Oppretter Composite-løvnode
    public static EndringsresultatSnapshot utenSnapshot(Class<?> aggregat) {
        return new EndringsresultatSnapshot(aggregat, null);
    }

    private List<EndringsresultatSnapshot> getChildren() {
        return children;
    }

    public List<EndringsresultatSnapshot> hentDelresultater() {
        return children.isEmpty() ? singletonList(this) : children;
    }

    @SuppressWarnings("unchecked")
    public <C> Class<C> getGrunnlag() {
        return (Class<C>) grunnlagKlasse;
    }

    public EndringsresultatSnapshot leggTil(EndringsresultatSnapshot endringsresultat) {
        getChildren().add(endringsresultat);
        return this;
    }

    @Override
    public String toString() {
        return "Endringer{" +
            "grunnlagKlasse='" + grunnlagKlasse.getSimpleName() + '\'' +
            ", grunnlagId=" + grunnlagId +
            ", type=" + (children.isEmpty() ? "løvnode" : "rotnode") +
            (children.isEmpty() ? "" : (", children=" + children)) +
            '}' + "\n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o==null || !(o instanceof EndringsresultatSnapshot)) return false;

        var that = (EndringsresultatSnapshot) o;

        return Objects.equals(grunnlagId, that.grunnlagId) &&
            Objects.equals(grunnlagKlasse, that.grunnlagKlasse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(grunnlagId, grunnlagKlasse);
    }

    public EndringsresultatDiff minus(EndringsresultatSnapshot etter) {
        var før = this;
        var idDiff = EndringsresultatDiff.opprett();

        Map<Class<?>, Object> førMap = new HashMap<>();
        før.children.stream().forEach(endring ->
            førMap.put(endring.grunnlagKlasse, endring.grunnlagId));

        Map<Class<?>, Object> etterMap = new HashMap<>();
        etter.children.stream().forEach(endring ->
            etterMap.put(endring.grunnlagKlasse, endring.grunnlagId));

        var alleGrunnlagsklasser = Stream.concat(førMap.keySet().stream(), etterMap.keySet().stream())
            .collect(toSet());

        alleGrunnlagsklasser.forEach(grunnlag ->
            idDiff.leggTilIdDiff(EndringsresultatDiff.medDiff(grunnlag, førMap.get(grunnlag), etterMap.get(grunnlag))));

        return idDiff;
    }

    public Object getGrunnlagId() {
        return grunnlagId;
    }
}
