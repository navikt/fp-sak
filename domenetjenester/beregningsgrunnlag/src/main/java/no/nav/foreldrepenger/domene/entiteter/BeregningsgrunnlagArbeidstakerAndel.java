package no.nav.foreldrepenger.domene.entiteter;


import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

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
        if (!(obj instanceof BeregningsgrunnlagArbeidstakerAndel)) {
            return false;
        }
        var other = (BeregningsgrunnlagArbeidstakerAndel) obj;
        return Objects.equals(this.getMottarYtelse(), other.getMottarYtelse());
    }

    @Override
    public int hashCode() {
        return Objects.hash(mottarYtelse);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
                "id=" + id + ", " //$NON-NLS-2$
                + "mottarYtelse=" + mottarYtelse + ", " //$NON-NLS-1$ //$NON-NLS-2$
                + ">"; //$NON-NLS-1$
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

