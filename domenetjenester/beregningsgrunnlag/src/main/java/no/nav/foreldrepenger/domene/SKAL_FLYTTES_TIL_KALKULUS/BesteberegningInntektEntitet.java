package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
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
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

@Entity(name = "BesteberegningInntektEntitet")
@Table(name = "BG_BESTEBEREGNING_INNTEKT")
public class BesteberegningInntektEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BESTEBEREGNING_INNTEKT")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Convert(converter = OpptjeningAktivitetType.KodeverdiConverter.class)
    @Column(name = "OPPTJENING_AKTIVITET_TYPE", nullable = false)
    private OpptjeningAktivitetType opptjeningAktivitetType;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Embedded
    private InternArbeidsforholdRef arbeidsforholdRef;

    @Column(name = "inntekt")
    private BigDecimal inntekt;

    @JsonBackReference
    @OneToOne
    @JoinColumn(name = "besteberegning_maaned_id", updatable = false, unique = true)
    private BesteberegningMånedsgrunnlagEntitet besteberegningMåned;


    public BesteberegningInntektEntitet(BesteberegningInntektEntitet besteberegningInntektEntitet) {
        this.arbeidsforholdRef = besteberegningInntektEntitet.arbeidsforholdRef;
        this.arbeidsgiver = besteberegningInntektEntitet.arbeidsgiver;
        this.opptjeningAktivitetType = besteberegningInntektEntitet.opptjeningAktivitetType;
        this.inntekt = besteberegningInntektEntitet.inntekt;
    }

    public BesteberegningInntektEntitet() {
    }

    public Long getId() {
        return id;
    }

    public long getVersjon() {
        return versjon;
    }

    public OpptjeningAktivitetType getOpptjeningAktivitetType() {
        return opptjeningAktivitetType;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
    }

    public BigDecimal getInntekt() {
        return inntekt;
    }

    void setBesteberegningMåned(BesteberegningMånedsgrunnlagEntitet besteberegningMåned) {
        this.besteberegningMåned = besteberegningMåned;
    }

    public static Builder ny() {
        return new Builder();
    }

    public static class Builder {
        private final BesteberegningInntektEntitet kladd;

        public Builder() {
            kladd = new BesteberegningInntektEntitet();
        }

        public Builder(BesteberegningInntektEntitet besteberegningInntektEntitet, boolean erOppdatering) {
            if (Objects.nonNull(besteberegningInntektEntitet.getId())) {
                throw new IllegalArgumentException("Kan ikke bygge på et lagret grunnlag");
            }
            if (erOppdatering) {
                kladd = besteberegningInntektEntitet;
            } else {
                kladd = new BesteberegningInntektEntitet(besteberegningInntektEntitet);
            }
        }


        public Builder medOpptjeningAktivitetType(OpptjeningAktivitetType opptjeningAktivitetType) {
            kladd.opptjeningAktivitetType = opptjeningAktivitetType;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            kladd.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
            kladd.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medInntekt(BigDecimal inntekt) {
            kladd.inntekt = inntekt;
            return this;
        }


        public BesteberegningInntektEntitet build() {
            Objects.requireNonNull(kladd.opptjeningAktivitetType);
            Objects.requireNonNull(kladd.inntekt);
            return kladd;
        }
    }

}
