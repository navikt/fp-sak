package no.nav.foreldrepenger.behandlingslager.behandling.beregning;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity(name = "Beregningsresultat")
@Table(name = "BR_BEREGNINGSRESULTAT")
public class BeregningsresultatEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BR_BEREGNINGSRESULTAT")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsresultat", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<BeregningsresultatPeriode> beregningsresultatPerioder = new ArrayList<>();

    @Lob
    @Column(name = "regel_input", nullable = false)
    private String regelInput;

    @Lob
    @Column(name = "regel_sporing", nullable = false)
    private String regelSporing;

    public BeregningsresultatEntitet() {
    }

    public Long getId() {
        return id;
    }

    public String getRegelInput() {
        return regelInput;
    }

    public String getRegelSporing() {
        return regelSporing;
    }

    public List<BeregningsresultatPeriode> getBeregningsresultatPerioder() {
        return Collections.unmodifiableList(beregningsresultatPerioder);
    }

    public void addBeregningsresultatPeriode(BeregningsresultatPeriode brPeriode){
        Objects.requireNonNull(brPeriode, "beregningsresultatPeriode");
        if (!beregningsresultatPerioder.contains(brPeriode)) {  // Class defines List based fields but uses them like Sets: Ingening å tjene på å bytte til Set ettersom det er små lister
            beregningsresultatPerioder.add(brPeriode);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeregningsresultatEntitet other)) {
            return false;
        }
        return Objects.equals(this.getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(BeregningsresultatEntitet beregningsresultat) {
        return new Builder(beregningsresultat);
    }

    public static class Builder {
        private BeregningsresultatEntitet beregningsresultatFPMal;

        public Builder() {
            this.beregningsresultatFPMal = new BeregningsresultatEntitet();
        }

        public Builder(BeregningsresultatEntitet beregningsresultat) {
            this.beregningsresultatFPMal = beregningsresultat;
        }

        public Builder medRegelInput(String regelInput){
            beregningsresultatFPMal.regelInput = regelInput;
            return this;
        }

        public Builder medRegelSporing(String regelSporing){
            beregningsresultatFPMal.regelSporing = regelSporing;
            return this;
        }

        public BeregningsresultatEntitet build() {
            verifyStateForBuild();
            return beregningsresultatFPMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(beregningsresultatFPMal.beregningsresultatPerioder, "beregningsresultatPerioder");
            Objects.requireNonNull(beregningsresultatFPMal.regelInput, "regelInput");
            Objects.requireNonNull(beregningsresultatFPMal.regelSporing, "regelSporing");
        }
    }
}
