package no.nav.foreldrepenger.domene.entiteter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType;

import java.util.Objects;

import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType.PERIODISERING;

@Entity(name = "BeregningsgrunnlagRegelSporing")
@Table(name = "BG_REGEL_SPORING")
public class BeregningsgrunnlagRegelSporing extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_REGEL_SPORING")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "bg_id", nullable = false, updatable = false)
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @Lob
    @Column(name = "regel_evaluering")
    private String regelEvaluering;

    @Lob
    @Column(name = "regel_input")
    private String regelInput;

    @Convert(converter= BeregningsgrunnlagRegelType.KodeverdiConverter.class)
    @Column(name="regel_type", nullable = false)
    private BeregningsgrunnlagRegelType regelType;

    public BeregningsgrunnlagRegelSporing(BeregningsgrunnlagRegelSporing beregningsgrunnlagRegelSporing) {
        this.regelEvaluering = beregningsgrunnlagRegelSporing.getRegelEvaluering();
        this.regelInput = beregningsgrunnlagRegelSporing.getRegelInput();
        this.regelType = beregningsgrunnlagRegelSporing.getRegelType();
    }

    protected BeregningsgrunnlagRegelSporing() {
    }

    public Long getId() {
        return id;
    }

    public BeregningsgrunnlagRegelType getRegelType() {
        return regelType;
    }

    public String getRegelEvaluering() {
        return regelEvaluering;
    }

    public String getRegelInput() {
        return regelInput;
    }

    void setBeregningsgrunnlag(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        this.beregningsgrunnlag = beregningsgrunnlag;
    }

    static Builder ny() {
        return new Builder();
    }

    static class Builder {
        private BeregningsgrunnlagRegelSporing beregningsgrunnlagRegelSporingMal;

        Builder() {
            beregningsgrunnlagRegelSporingMal = new BeregningsgrunnlagRegelSporing();
        }

        Builder medRegelInput(String regelInput) {
            beregningsgrunnlagRegelSporingMal.regelInput = regelInput;
            return this;
        }

        Builder medRegelEvaluering(String regelEvaluering) {
            beregningsgrunnlagRegelSporingMal.regelEvaluering = regelEvaluering;
            return this;
        }

        Builder medRegelType(BeregningsgrunnlagRegelType regelType) {
            beregningsgrunnlagRegelSporingMal.regelType = regelType;
            return this;
        }

        BeregningsgrunnlagRegelSporing build(BeregningsgrunnlagEntitet beregningsgrunnlag) {
            verifyStateForBuild();
            beregningsgrunnlagRegelSporingMal.beregningsgrunnlag = beregningsgrunnlag;
            beregningsgrunnlag.leggTilBeregningsgrunnlagRegel(beregningsgrunnlagRegelSporingMal);
            return beregningsgrunnlagRegelSporingMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(beregningsgrunnlagRegelSporingMal.regelType, "regelType");
            Objects.requireNonNull(beregningsgrunnlagRegelSporingMal.regelInput, "regelInput");
            // Periodisering har ingen logg for evaluering, men kun input
            if (!PERIODISERING.equals(beregningsgrunnlagRegelSporingMal.regelType)) {
                Objects.requireNonNull(beregningsgrunnlagRegelSporingMal.regelEvaluering, "regelEvaluering");
            }
        }

    }
}
