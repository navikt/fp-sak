package no.nav.foreldrepenger.domene.entiteter;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;

public class BeregningAktivitetNøkkel {
    private OpptjeningAktivitetType opptjeningAktivitetType;
    private LocalDate fom;
    private String arbeidsgiverIdentifikator;
    private String arbeidsforholdRef;

    private BeregningAktivitetNøkkel() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BeregningAktivitetNøkkel that)) {
            return false;
        }
        return Objects.equals(opptjeningAktivitetType, that.opptjeningAktivitetType) && Objects.equals(fom, that.fom) && Objects.equals(
            arbeidsgiverIdentifikator, that.arbeidsgiverIdentifikator) && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opptjeningAktivitetType, fom, arbeidsgiverIdentifikator, arbeidsforholdRef);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BeregningAktivitetNøkkel kladd;

        private Builder() {
            kladd = new BeregningAktivitetNøkkel();
        }

        public Builder medOpptjeningAktivitetType(OpptjeningAktivitetType opptjeningAktivitetType) {
            kladd.opptjeningAktivitetType = opptjeningAktivitetType;
            return this;
        }

        public Builder medFom(LocalDate fom) {
            kladd.fom = fom;
            return this;
        }

        public Builder medArbeidsgiverIdentifikator(String arbeidsgiverIdentifikator) {
            kladd.arbeidsgiverIdentifikator = arbeidsgiverIdentifikator;
            return this;
        }

        public Builder medArbeidsforholdRef(String arbeidsforholdRef) {
            kladd.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public BeregningAktivitetNøkkel build() {
            verifyStateForBuild();
            return kladd;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(kladd.opptjeningAktivitetType);
            Objects.requireNonNull(kladd.fom);
        }
    }
}
