package no.nav.foreldrepenger.domene.entiteter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;


@Entity(name = "BesteberegningEntitet")
@Table(name = "BG_BESTEBEREGNINGGRUNNLAG")
public class BesteberegninggrunnlagEntitet extends BaseEntitet {

    public static final int ANTALL_BESTEBEREGNING_MÅNEDER = 6;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_BESTEBEREGNINGGRUNNLAG")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @JsonBackReference
    @OneToOne(optional = false)
    @JoinColumn(name = "beregningsgrunnlag_id", nullable = false, updatable = false, unique = true)
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "besteberegninggrunnlag", cascade = CascadeType.PERSIST)
    private Set<BesteberegningMånedsgrunnlagEntitet> seksBesteMåneder = new HashSet<>();

    @Column(name = "avvik_belop")
    private BigDecimal avvik;

    public BesteberegninggrunnlagEntitet(BesteberegninggrunnlagEntitet besteberegninggrunnlagEntitet) {
        besteberegninggrunnlagEntitet.getSeksBesteMåneder().stream()
            .map(BesteberegningMånedsgrunnlagEntitet::new)
            .forEach(this::leggTilMånedsgrunnlag);
        this.avvik = besteberegninggrunnlagEntitet.getAvvik().orElse(null);
    }

    public BesteberegninggrunnlagEntitet() {

    }

    public Long getId() {
        return id;
    }

    public long getVersjon() {
        return versjon;
    }

    public Set<BesteberegningMånedsgrunnlagEntitet> getSeksBesteMåneder() {
        return seksBesteMåneder;
    }

    public BeregningsgrunnlagEntitet getBeregningsgrunnlag() {
        return beregningsgrunnlag;
    }

    void setBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        this.beregningsgrunnlag = beregningsgrunnlag;
    }

    // Det regnes kun ut et avvik for å kontrollere hvis tredje ledd har gitt beste beregning
    public Optional<BigDecimal> getAvvik() {
        return Optional.ofNullable(avvik);
    }

    private void leggTilMånedsgrunnlag(BesteberegningMånedsgrunnlagEntitet månedsgrunnlagEntitet) {
        if (seksBesteMåneder.size() >= ANTALL_BESTEBEREGNING_MÅNEDER) {
            throw new IllegalStateException("Kan ikke legge til mer en + " +
                ANTALL_BESTEBEREGNING_MÅNEDER + " måneder for bestebergning");
        }
        if (seksBesteMåneder.stream().anyMatch(m -> m.getPeriode().overlapper(månedsgrunnlagEntitet.getPeriode()))) {
            throw new IllegalStateException("Det finnes allerede et månedsgrunnlag for " + månedsgrunnlagEntitet.getPeriode());
        }
        månedsgrunnlagEntitet.setBesteberegninggrunnlag(this);
        this.seksBesteMåneder.add(månedsgrunnlagEntitet);
    }

    public static Builder ny() {
        return new Builder();
    }

    public static class Builder {
        private final BesteberegninggrunnlagEntitet kladd;

        public Builder() {
            kladd = new BesteberegninggrunnlagEntitet();
        }

        public Builder(BesteberegninggrunnlagEntitet besteberegninggrunnlagEntitet, boolean erOppdatering) {
            if (Objects.nonNull(besteberegninggrunnlagEntitet.getId())) {
                throw new IllegalArgumentException("Kan ikke bygge på et lagret grunnlag");
            }
            if (erOppdatering) {
                kladd = besteberegninggrunnlagEntitet;
            } else {
                kladd = new BesteberegninggrunnlagEntitet(besteberegninggrunnlagEntitet);
            }
        }

        public Builder leggTilMånedsgrunnlag(BesteberegningMånedsgrunnlagEntitet månedsgrunnlagEntitet) {
            kladd.leggTilMånedsgrunnlag(månedsgrunnlagEntitet);
            return this;
        }

        public Builder medAvvik(BigDecimal avvik) {
            kladd.avvik = avvik;
            return this;
        }

        public BesteberegninggrunnlagEntitet build() {
            return kladd;
        }

    }






}
