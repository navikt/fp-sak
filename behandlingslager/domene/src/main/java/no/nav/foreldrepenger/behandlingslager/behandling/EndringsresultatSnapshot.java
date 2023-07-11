package no.nav.foreldrepenger.behandlingslager.behandling;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Bruker en primitiv variant av Composite for å kunne vurderes enkeltvis (løvnode) og sammensatt (rotnode)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EndringsresultatSnapshot {

    @JsonProperty(value = "grunnlagId")
    private Long grunnlagId;

    @JsonProperty(value = "grunnlagUuid")
    private UUID grunnlagUuid;

    @JsonProperty(value = "grunnlagKlasse", required = true)
    private Class<?> grunnlagKlasse;

    @JsonProperty(value = "children", required = true)
    private List<EndringsresultatSnapshot> children = emptyList();

    // Brukes som Composite-rotnode
    private EndringsresultatSnapshot() {
        this.grunnlagKlasse = this.getClass(); // rot
        children = new ArrayList<>();
    }

    // Brukes som Composite-løvnode
    private EndringsresultatSnapshot(Class<?> grunnlagKlasse, Long grunnlagId) {
        this.grunnlagKlasse = grunnlagKlasse;
        this.grunnlagId = grunnlagId;
    }

    public EndringsresultatSnapshot(Class<?> grunnlagKlasse, UUID id) {
        this.grunnlagKlasse = grunnlagKlasse;
        this.grunnlagUuid = id;
    }

    // Oppretter Composite-rotnode
    public static EndringsresultatSnapshot opprett() {
        return new EndringsresultatSnapshot();
    }

    // Oppretter Composite-løvnode
    public static EndringsresultatSnapshot medSnapshot(Class<?> aggregat, Long id) {
        return new EndringsresultatSnapshot(aggregat, id);
    }

    // Oppretter Composite-løvnode
    public static EndringsresultatSnapshot medSnapshot(Class<?> aggregat, UUID id) {
        return new EndringsresultatSnapshot(aggregat, id);
    }

    // Oppretter Composite-løvnode
    public static EndringsresultatSnapshot utenSnapshot(Class<?> aggregat) {
        return new EndringsresultatSnapshot(aggregat, (UUID) null);
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

    public Object getGrunnlagRef() {
        if (grunnlagId != null) {
            return grunnlagId;
        }
        if (grunnlagUuid != null) {
            return grunnlagUuid;
        }
        return null;
    }

    public EndringsresultatSnapshot leggTil(EndringsresultatSnapshot endringsresultat) {
        getChildren().add(endringsresultat);
        return this;
    }

    @Override
    public String toString() {
        return "Endringer{" + "grunnlagKlasse='" + grunnlagKlasse.getSimpleName() + '\'' + ", grunnlagId=" + getGrunnlagRef() + ", type="
            + (children.isEmpty() ? "løvnode" : "rotnode") + (children.isEmpty() ? "" : ", children=" + children) + '}' + "\n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EndringsresultatSnapshot that)) {
            return false;
        }

        return Objects.equals(getGrunnlagRef(), that.getGrunnlagRef()) &&
            Objects.equals(grunnlagKlasse, that.grunnlagKlasse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getGrunnlagRef(), grunnlagKlasse);
    }

    public EndringsresultatDiff minus(EndringsresultatSnapshot etter) {
        var før = this;
        var idDiff = EndringsresultatDiff.opprett();

        Map<Class<?>, Object> førMap = new HashMap<>();
        før.children.forEach(endring ->
            førMap.put(endring.grunnlagKlasse, endring.getGrunnlagRef()));

        Map<Class<?>, Object> etterMap = new HashMap<>();
        etter.children.forEach(endring ->
            etterMap.put(endring.grunnlagKlasse, endring.getGrunnlagRef()));

        var alleGrunnlagsklasser = Stream.concat(førMap.keySet().stream(), etterMap.keySet().stream())
            .collect(toSet());

        alleGrunnlagsklasser.forEach(grunnlag ->
            idDiff.leggTilIdDiff(EndringsresultatDiff.medDiff(grunnlag, førMap.get(grunnlag), etterMap.get(grunnlag))));

        return idDiff;
    }
}
