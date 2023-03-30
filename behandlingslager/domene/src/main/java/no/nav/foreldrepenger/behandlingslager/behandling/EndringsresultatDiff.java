package no.nav.foreldrepenger.behandlingslager.behandling;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;

// Bruker en primitiv variant av Composite for å kunne vurderes enkeltvis (løvnode) og sammensatt (rotnode)
public class EndringsresultatDiff {

    private Object grunnlagId1;
    private Object grunnlagId2;
    private Class<?> grunnlagKlasse;
    private boolean støtterSporingsendringer;
    private boolean erSporedeFeltEndret;
    private DiffResult diffResult = null;

    private List<EndringsresultatDiff> children = emptyList();

    // Brukes som Composite-rotnode
    private EndringsresultatDiff(boolean støtterSporingsendringer) {
        this.grunnlagKlasse = this.getClass(); // rot
        this.støtterSporingsendringer = støtterSporingsendringer;
        children = new ArrayList<>();
    }

    // Brukes som Composite-løvnode
    private EndringsresultatDiff(Class<?> grunnlagKlasse, Object grunnlagId1, Object grunnlagId2, boolean støtterSporingsendringer, boolean erSporedeFeltEndret, DiffResult diffResultat) {
        this.grunnlagKlasse = grunnlagKlasse;
        this.grunnlagId1 = grunnlagId1;
        this.grunnlagId2 = grunnlagId2;
        this.støtterSporingsendringer = støtterSporingsendringer;
        this.erSporedeFeltEndret = erSporedeFeltEndret;
        this.diffResult = diffResultat;
    }

    // Oppretter Composite-rotnode
    public static EndringsresultatDiff opprett() {
        var støtterSporingsendringer = false;
        return new EndringsresultatDiff(støtterSporingsendringer);
    }

    public static EndringsresultatDiff opprettForSporingsendringer() {
        var støtterSporingsendringer = true;
        return new EndringsresultatDiff(støtterSporingsendringer);
    }

    // Oppretter Composite-løvnode
    public static EndringsresultatDiff medDiff(Class<?> grunnlagKlasse, Object grunnlagId1, Object grunnlagId2) {
        var støtterSporingsendringer = false;
        return new EndringsresultatDiff(grunnlagKlasse, grunnlagId1, grunnlagId2, støtterSporingsendringer, false, null);
    }

    public static EndringsresultatDiff medDiffPåSporedeFelt(EndringsresultatDiff diff, boolean erSporedeFeltEndret, DiffResult diffResultat) {
        var støtterSporingsendringer = true;
        return new EndringsresultatDiff(diff.grunnlagKlasse, diff.grunnlagId1, diff.grunnlagId2, støtterSporingsendringer, erSporedeFeltEndret, diffResultat);
    }

    public boolean erSporedeFeltEndret() {
        if(!støtterSporingsendringer) {
            throw new IllegalArgumentException("Utviklerfeil: ikke satt opp til å støtte sporing på felter");
        }
        return erSporedeFeltEndret  || getChildren().stream().anyMatch(EndringsresultatDiff::erSporedeFeltEndret);
    }

    private List<EndringsresultatDiff> getChildren() {
        return children;
    }

    public List<EndringsresultatDiff> hentDelresultater() {
        return children.isEmpty() ? singletonList(this) : children;
    }

    public List<EndringsresultatDiff> hentKunDelresultater() {
        return getChildren();
    }

    public Optional<EndringsresultatDiff> hentDelresultat(Class<?> grunnlagKlasse) {
        return getChildren().stream()
            .filter(it -> it.getGrunnlag().equals(grunnlagKlasse))
            .findFirst();
    }

    @SuppressWarnings("unchecked")
    public <C> Class<C> getGrunnlag() {
        return (Class<C>) grunnlagKlasse;
    }

    public EndringsresultatDiff leggTilIdDiff(EndringsresultatDiff endringsresultat) {
        getChildren().add(endringsresultat);
        return this;
    }


    public EndringsresultatDiff leggTilSporetEndring(EndringsresultatDiff endringsresultat, Supplier<DiffResult> sporedeFeltSjekkSupplier) {
        boolean erSporetFeltEndret;
        DiffResult diffResultat = null;
        var id1 = endringsresultat.getGrunnlagId1();
        var id2 = endringsresultat.getGrunnlagId2();

        if (Objects.equals(id1, id2)) {
            // Sporede felt kan ikke være endret dersom id-er er like
            erSporetFeltEndret = false;
        } else if ((id1 == null && id2 != null) || (id1 != null && id2 == null)) {  // NOSONAR - false positive
            // Grunnlaget har gått fra å ikke eksistere til å eksistere -> antas alltid å være en sporbar endring
            erSporetFeltEndret = true;
        } else {
            // Id-er er forskjellige -> deleger endringssjekk på sporede felt (@ChangeTracked) til domenetjenestene
            diffResultat = sporedeFeltSjekkSupplier.get();
            erSporetFeltEndret = !diffResultat.isEmpty();
        }

        var diff = EndringsresultatDiff.medDiffPåSporedeFelt(endringsresultat, erSporetFeltEndret, diffResultat);
        getChildren().add(diff);
        return this;
    }

    @Override
    public String toString() {
        return "Endringer{" +
            "grunnlagKlasse='" + grunnlagKlasse.getSimpleName() + '\'' +
            ", grunnlagId1=" + grunnlagId1 +
            ", grunnlagId2=" + grunnlagId2 +
            ", erSporedeFeltEndret=" + erSporedeFeltEndret +
            ", antallFeltEndringer=" + Optional.ofNullable(diffResult)
            .map(diff -> diff.getLeafDifferences().size())
            .orElse(0) +
            ", type=" + (children.isEmpty() ? "løvnode" : "rotnode") +
            (children.isEmpty() ? "" : (", children=" + children)) +
            '}' + "\n";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EndringsresultatDiff that)) return false;

        return Objects.equals(grunnlagKlasse, that.grunnlagKlasse)
            && Objects.equals(grunnlagId1, that.grunnlagId1)
            && Objects.equals(grunnlagId2, that.grunnlagId2)
            && Objects.equals(erSporedeFeltEndret, that.erSporedeFeltEndret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(grunnlagKlasse, grunnlagId1, grunnlagId2, erSporedeFeltEndret);
    }

    public Object getGrunnlagId1() { return grunnlagId1;
    }

    public Object getGrunnlagId2() {
        return grunnlagId2;
    }
}
