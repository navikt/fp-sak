package no.nav.foreldrepenger.domene.entiteter;


import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.vedtak.felles.jpa.converters.BooleanToStringConverter;

@Entity(name = "BeregningsgrunnlagArbeidstakerAndel")
@Table(name = "BG_ARBEIDSTAKER_ANDEL")
public class BeregningsgrunnlagArbeidstakerAndel extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_ARBEIDSTAKER_ANDEL")
    private Long id;

    @OneToOne(optional = false)
    @JsonBackReference
    @JoinColumn(name = "BG_PR_STATUS_ANDEL_ID", nullable = false, updatable = false)
    private BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "MOTTAR_YTELSE")
    private Boolean mottarYtelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public BeregningsgrunnlagArbeidstakerAndel(BeregningsgrunnlagArbeidstakerAndel beregningsgrunnlagArbeidstakerAndel) {
        this.mottarYtelse = beregningsgrunnlagArbeidstakerAndel.mottarYtelse;
    }

    public BeregningsgrunnlagArbeidstakerAndel() {
    }

    public static BeregningsgrunnlagArbeidstakerAndel.Builder builder() {
        return new BeregningsgrunnlagArbeidstakerAndel.Builder();
    }

    public static BeregningsgrunnlagArbeidstakerAndel.Builder builder(BeregningsgrunnlagArbeidstakerAndel eksisterendeBGArbeidstakerAndel) {
        return new BeregningsgrunnlagArbeidstakerAndel.Builder(eksisterendeBGArbeidstakerAndel);
    }

    public BeregningsgrunnlagPrStatusOgAndel getBeregningsgrunnlagPrStatusOgAndel() {
        return beregningsgrunnlagPrStatusOgAndel;
    }

    public Boolean getMottarYtelse() {
        return mottarYtelse;
    }

    void setBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
        this.beregningsgrunnlagPrStatusOgAndel = beregningsgrunnlagPrStatusOgAndel;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeregningsgrunnlagArbeidstakerAndel other)) {
            return false;
        }
        return Objects.equals(this.getMottarYtelse(), other.getMottarYtelse());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mottarYtelse);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
                "id=" + id + ", "
                + "mottarYtelse=" + mottarYtelse + ", "
                + ">";
    }

    public static class Builder {
        private BeregningsgrunnlagArbeidstakerAndel beregningsgrunnlagArbeidstakerAndelMal;

        public Builder() {
            beregningsgrunnlagArbeidstakerAndelMal = new BeregningsgrunnlagArbeidstakerAndel();
        }

        public Builder(BeregningsgrunnlagArbeidstakerAndel eksisterendeBGArbeidstakerAndelMal) {
            beregningsgrunnlagArbeidstakerAndelMal = eksisterendeBGArbeidstakerAndelMal;
        }

        public BeregningsgrunnlagArbeidstakerAndel.Builder medMottarYtelse(Boolean mottarYtelse) {
            beregningsgrunnlagArbeidstakerAndelMal.mottarYtelse = mottarYtelse;
            return this;
        }

        public BeregningsgrunnlagArbeidstakerAndel build(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
            beregningsgrunnlagPrStatusOgAndel.setBeregningsgrunnlagArbeidstakerAndel(beregningsgrunnlagArbeidstakerAndelMal);
            verifyStateForBuild(beregningsgrunnlagPrStatusOgAndel);
            return beregningsgrunnlagArbeidstakerAndelMal;
        }

        public void verifyStateForBuild(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
            if (!beregningsgrunnlagPrStatusOgAndel.getAktivitetStatus().erArbeidstaker()) {
                throw new IllegalArgumentException("Andel med arbeidstakerfelt må ha aktivitetstatus arbeidstaker");
            }
        }
    }
}

