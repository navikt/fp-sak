package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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

    public Set<Stønadskonto> getStønadskontoer() {
        return Collections.unmodifiableSet(stønadskontoer);
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
