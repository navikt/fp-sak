package no.nav.foreldrepenger.domene.modell;

import java.math.BigDecimal;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class BesteberegningInntekt {

    private OpptjeningAktivitetType opptjeningAktivitetType;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private BigDecimal inntekt;

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

    public static BesteberegningInntekt.Builder ny() {
        return new Builder();
    }

    public static class Builder {
        private final BesteberegningInntekt kladd;

        public Builder() {
            kladd = new BesteberegningInntekt();
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


        public BesteberegningInntekt build() {
            Objects.requireNonNull(kladd.opptjeningAktivitetType);
            Objects.requireNonNull(kladd.inntekt);
            return kladd;
        }
    }

}
