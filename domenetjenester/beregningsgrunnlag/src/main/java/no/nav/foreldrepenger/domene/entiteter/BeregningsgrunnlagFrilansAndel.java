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

@Entity(name = "BeregningsgrunnlagFrilansAndel")
@Table(name = "BG_FRILANS_ANDEL")
public class BeregningsgrunnlagFrilansAndel extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_FRILANS_ANDEL")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @OneToOne(optional = false)
    @JsonBackReference
    @JoinColumn(name = "BG_PR_STATUS_ANDEL_ID", nullable = false, updatable = false)
    private BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "MOTTAR_YTELSE")
    private Boolean mottarYtelse;

    @Convert(converter = BooleanToStringConverter.class)
    @Column(name = "NYOPPSTARTET")
    private Boolean nyoppstartet;

    public BeregningsgrunnlagFrilansAndel(BeregningsgrunnlagFrilansAndel beregningsgrunnlagFrilansAndel) {
        this.mottarYtelse = beregningsgrunnlagFrilansAndel.mottarYtelse;
        this.nyoppstartet = beregningsgrunnlagFrilansAndel.nyoppstartet;
    }

    private BeregningsgrunnlagFrilansAndel() { }

    public BeregningsgrunnlagPrStatusOgAndel getBeregningsgrunnlagPrStatusOgAndel() {
        return beregningsgrunnlagPrStatusOgAndel;
    }

    public Boolean getMottarYtelse() {
        return mottarYtelse;
    }

    public Boolean getNyoppstartet() {
        return nyoppstartet;
    }


    void setBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
        this.beregningsgrunnlagPrStatusOgAndel = beregningsgrunnlagPrStatusOgAndel;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BeregningsgrunnlagFrilansAndel other)) {
            return false;
        }
        return Objects.equals(this.getMottarYtelse(), other.getMottarYtelse())
                && Objects.equals(this.getNyoppstartet(), other.getNyoppstartet());
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


    public static BeregningsgrunnlagFrilansAndel.Builder builder() {
        return new BeregningsgrunnlagFrilansAndel.Builder();
    }

    public static BeregningsgrunnlagFrilansAndel.Builder builder(BeregningsgrunnlagFrilansAndel eksisterendeBGFrilansAndel) {
        return new BeregningsgrunnlagFrilansAndel.Builder(eksisterendeBGFrilansAndel);
    }

    public static class Builder {
        private BeregningsgrunnlagFrilansAndel beregningsgrunnlagFrilansAndelMal;

        public Builder() {
            beregningsgrunnlagFrilansAndelMal = new BeregningsgrunnlagFrilansAndel();
        }

        public Builder(BeregningsgrunnlagFrilansAndel eksisterendeBGFrilansAndelMal) {
            beregningsgrunnlagFrilansAndelMal = eksisterendeBGFrilansAndelMal;
        }

        BeregningsgrunnlagFrilansAndel.Builder medMottarYtelse(Boolean mottarYtelse) {
            beregningsgrunnlagFrilansAndelMal.mottarYtelse = mottarYtelse;
            return this;
        }

        public BeregningsgrunnlagFrilansAndel.Builder medNyoppstartet(Boolean nyoppstartet) {
            beregningsgrunnlagFrilansAndelMal.nyoppstartet = nyoppstartet;
            return this;
        }

        public BeregningsgrunnlagFrilansAndel build(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
            beregningsgrunnlagPrStatusOgAndel.setBeregningsgrunnlagFrilansAndel(beregningsgrunnlagFrilansAndelMal);
            verifyStateForBuild(beregningsgrunnlagPrStatusOgAndel);
            return beregningsgrunnlagFrilansAndelMal;
        }

        public void verifyStateForBuild(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel) {
            if (!beregningsgrunnlagPrStatusOgAndel.getAktivitetStatus().erFrilanser()) {
                throw new IllegalArgumentException("Andel med frilansfelt må ha aktivitetstatus frilans");
            }
        }
    }
}
