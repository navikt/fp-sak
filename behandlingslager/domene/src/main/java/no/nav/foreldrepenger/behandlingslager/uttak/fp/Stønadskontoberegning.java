package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;

@Entity
@Table(name = "STOENADSKONTOBEREGNING")
public class Stønadskontoberegning extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_STOENADSKONTOBEREGNING")
    private Long id;

    @Lob
    @ChangeTracked
    @Column(name = "regel_input", nullable = false)
    private String regelInput;

    @Lob
    @Column(name = "regel_evaluering", nullable = false)
    private String regelEvaluering;

    @Column(name = "regel_versjon")
    private String regelVersjon;

    @ChangeTracked
    @OneToMany(mappedBy = "stønadskontoberegning")
    private Set<Stønadskonto> stønadskontoer = new HashSet<>();


    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public String getRegelInput() {
        return regelInput;
    }

    public String getRegelEvaluering() {
        return regelEvaluering;
    }

    public String getRegelVersjon() {
        return regelVersjon;
    }

    public Set<Stønadskonto> getStønadskontoer() {
        return Collections.unmodifiableSet(stønadskontoer);
    }

    public void leggTilStønadskonto(Stønadskonto stønadskonto) {
        stønadskontoer.add(stønadskonto);
    }

    public Map<StønadskontoType, Integer> getStønadskontoutregning() {
        return stønadskontoer.stream()
            .filter(k -> k.getMaxDager() > 0)
            .collect(Collectors.toMap(Stønadskonto::getStønadskontoType, Stønadskonto::getMaxDager));
    }

    @Override
    public String toString() {
        return "Stønadskontoberegning{" +
            "stønadskontoer=" + stønadskontoer +
            '}';
    }

    public static class Builder {
        private Stønadskontoberegning kladd;

        public Builder() {
            kladd = new Stønadskontoberegning();
        }

        public Builder medRegelInput(String regelInput) {
            kladd.regelInput = regelInput;
            return this;
        }

        public Builder medRegelEvaluering(String regelEvaluering) {
            kladd.regelEvaluering = regelEvaluering;
            return this;
        }

        public Builder medRegelVersjon(String regelVersjon) {
            kladd.regelVersjon = regelVersjon;
            return this;
        }

        public Builder medStønadskonto(Stønadskonto stønadskonto) {
            Objects.requireNonNull(stønadskonto);
            stønadskonto.setStønadskontoberegning(kladd);
            kladd.stønadskontoer.add(stønadskonto);
            return this;
        }

        public Stønadskontoberegning build() {
            return kladd;
        }
    }
}
