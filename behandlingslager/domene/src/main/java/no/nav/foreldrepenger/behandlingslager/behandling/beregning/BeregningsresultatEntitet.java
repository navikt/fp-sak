package no.nav.foreldrepenger.behandlingslager.behandling.beregning;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

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

    /**
     * Er egentlig OneToOne, men må mappes slik da JPA/Hibernate ikke støtter OneToOne på annet enn shared PK.
     */
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "beregningsresultat", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private Set<BeregningsresultatFeriepenger> beregningsresultatFeriepenger = new HashSet<>(1);

    @Lob
    @Column(name = "regel_input", nullable = false)
    private String regelInput;

    @Lob
    @Column(name = "regel_sporing", nullable = false)
    private String regelSporing;

    @Column(name = "endringsdato")
    private LocalDate endringsdato;

    public Long getId() {
        return id;
    }

    public String getRegelInput() {
        return regelInput;
    }

    public String getRegelSporing() {
        return regelSporing;
    }

    public Optional<LocalDate> getEndringsdato(){
        return Optional.ofNullable(endringsdato);
    }

    public List<BeregningsresultatPeriode> getBeregningsresultatPerioder() {
        return Collections.unmodifiableList(beregningsresultatPerioder);
    }

    public void addBeregningsresultatPeriode(BeregningsresultatPeriode brPeriode){
        Objects.requireNonNull(brPeriode, "beregningsresultatPeriode");
        if (!beregningsresultatPerioder.contains(brPeriode)) { // NOSONAR Class defines List based fields but uses them like Sets: Ingening å tjene på å bytte til Set ettersom det er små lister
            beregningsresultatPerioder.add(brPeriode);
        }
    }

    public Optional<BeregningsresultatFeriepenger> getBeregningsresultatFeriepenger() {
        if (this.beregningsresultatFeriepenger.size() > 1) {
            throw new IllegalStateException("Utviklerfeil: Det finnes flere BeregningsresultatFeriepenger");
        }
        return beregningsresultatFeriepenger.isEmpty() ? Optional.empty() : Optional.of(beregningsresultatFeriepenger.iterator().next());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof BeregningsresultatEntitet)) {
            return false;
        }
        BeregningsresultatEntitet other = (BeregningsresultatEntitet) obj;
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

        public Builder medBeregningsresultatFeriepenger(BeregningsresultatFeriepenger beregningsresultatFeriepenger) {
            beregningsresultatFPMal.beregningsresultatFeriepenger.clear();
            beregningsresultatFPMal.beregningsresultatFeriepenger.add(beregningsresultatFeriepenger);
            return this;
        }

        public Builder medEndringsdato(LocalDate endringsdato){
            beregningsresultatFPMal.endringsdato = endringsdato;
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
