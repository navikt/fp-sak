package no.nav.foreldrepenger.domene.entiteter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagPeriodeRegelType;

@Entity(name = "BeregningsgrunnlagPeriodeRegelSporing")
@Table(name = "BG_PERIODE_REGEL_SPORING")
public class BeregningsgrunnlagPeriodeRegelSporing extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_PERIODE_REGEL_SPORING")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @JsonBackReference
    @ManyToOne(optional = false)
    @JoinColumn(name = "bg_periode_id", nullable = false, updatable = false)
    private BeregningsgrunnlagPeriode beregningsgrunnlagPeriode;

    @Lob
    @Column(name = "regel_evaluering")
    private String regelEvaluering;

    @Lob
    @Column(name = "regel_input")
    private String regelInput;

    @Convert(converter = BeregningsgrunnlagPeriodeRegelType.KodeverdiConverter.class)
    @Column(name = "regel_type", nullable = false)
    private BeregningsgrunnlagPeriodeRegelType regelType;

    public BeregningsgrunnlagPeriodeRegelSporing(BeregningsgrunnlagPeriodeRegelSporing beregningsgrunnlagPeriodeRegelSporing) {
        this.regelEvaluering = beregningsgrunnlagPeriodeRegelSporing.getRegelEvaluering();
        this.regelInput = beregningsgrunnlagPeriodeRegelSporing.getRegelInput();
        this.regelType = beregningsgrunnlagPeriodeRegelSporing.getRegelType();
    }

    BeregningsgrunnlagPeriodeRegelSporing() {
        // hibernate
    }

    public Long getId() {
        return id;
    }

    public BeregningsgrunnlagPeriodeRegelType getRegelType() {
        return regelType;
    }

    public String getRegelEvaluering() {
        return regelEvaluering;
    }

    public String getRegelInput() {
        return regelInput;
    }

    void setBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        this.beregningsgrunnlagPeriode = beregningsgrunnlagPeriode;
    }

    static Builder ny() {
        return new Builder();
    }

    public static class Builder {
        private BeregningsgrunnlagPeriodeRegelSporing beregningsgrunnlagPeriodeRegelSporingMal;

        public Builder() {
            beregningsgrunnlagPeriodeRegelSporingMal = new BeregningsgrunnlagPeriodeRegelSporing();
        }

        Builder medRegelInput(String regelInput) {
            beregningsgrunnlagPeriodeRegelSporingMal.regelInput = regelInput;
            return this;
        }

        Builder medRegelEvaluering(String regelEvaluering) {
            beregningsgrunnlagPeriodeRegelSporingMal.regelEvaluering = regelEvaluering;
            return this;
        }

        Builder medRegelType(BeregningsgrunnlagPeriodeRegelType regelType) {
            beregningsgrunnlagPeriodeRegelSporingMal.regelType = regelType;
            return this;
        }

        BeregningsgrunnlagPeriodeRegelSporing build(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
            beregningsgrunnlagPeriode.leggTilBeregningsgrunnlagPeriodeRegel(beregningsgrunnlagPeriodeRegelSporingMal);
            return beregningsgrunnlagPeriodeRegelSporingMal;
        }
    }

}
