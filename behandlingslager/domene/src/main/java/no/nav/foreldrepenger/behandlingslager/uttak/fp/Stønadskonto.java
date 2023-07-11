package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;

@Entity
@Table(name = "STOENADSKONTO")
public class Stønadskonto extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_STOENADSKONTO")
    private Long id;

    @ChangeTracked
    @Column(name = "max_dager", nullable = false)
    private Integer maxDager;

    @ChangeTracked
    @Convert(converter = StønadskontoType.KodeverdiConverter.class)
    @Column(name="stoenadskontotype", nullable = false)
    private StønadskontoType stønadskontoType;

    @ManyToOne
    @JoinColumn(name = "stoenadskontoberegning_id", nullable = false)
    private Stønadskontoberegning stønadskontoberegning;

    Stønadskonto() {
        // For hibernate
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(stønadskontoType);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Stønadskonto other)) {
            return false;
        }
        return Objects.equals(stønadskontoType, other.stønadskontoType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stønadskontoType);
    }

    @Override
    public String toString() {
        return "Stønadskonto{" +
            "maxDager=" + maxDager +
            ", stønadskontoType=" + stønadskontoType +
            '}';
    }

    public int getMaxDager() {
        return maxDager;
    }

    public StønadskontoType getStønadskontoType() {
        return stønadskontoType;
    }

    public Stønadskontoberegning getStønadskontoberegning() {
        return stønadskontoberegning;
    }

    void setStønadskontoberegning(Stønadskontoberegning stønadskontoberegning) {
        this.stønadskontoberegning = stønadskontoberegning;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Stønadskonto kladd = new Stønadskonto();

        public Builder medMaxDager(int maxDager) {
            kladd.maxDager = maxDager;
            return this;
        }

        public Builder medStønadskontoType(StønadskontoType stønadskontoType) {
            kladd.stønadskontoType = stønadskontoType;
            return this;
        }

        public Stønadskonto build() {
            return kladd;
        }
    }
}
