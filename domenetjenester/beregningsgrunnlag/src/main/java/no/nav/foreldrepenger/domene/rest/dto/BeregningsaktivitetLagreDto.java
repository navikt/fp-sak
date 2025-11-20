package no.nav.foreldrepenger.domene.rest.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public class BeregningsaktivitetLagreDto {
    @NotNull
    @ValidKodeverk
    private OpptjeningAktivitetType opptjeningAktivitetType;

    @NotNull
    private LocalDate fom;

    private LocalDate tom;

    @Pattern(regexp = InputValideringRegex.ARBEIDSGIVER)
    private String oppdragsgiverOrg;

    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String arbeidsgiverIdentifikator;

    @Size(max = 100)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String arbeidsforholdRef;

    private boolean skalBrukes;

    BeregningsaktivitetLagreDto() {
        //for jackson
    }

    public OpptjeningAktivitetType getOpptjeningAktivitetType() {
        return opptjeningAktivitetType;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getOppdragsgiverOrg() {
        return oppdragsgiverOrg;
    }

    public String getArbeidsgiverIdentifikator() {
        return arbeidsgiverIdentifikator;
    }

    public String getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public boolean getSkalBrukes() {
        return skalBrukes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private BeregningsaktivitetLagreDto kladd;

        private Builder() {
            kladd = new BeregningsaktivitetLagreDto();
        }

        public Builder medOpptjeningAktivitetType(OpptjeningAktivitetType opptjeningAktivitetType) {
            kladd.opptjeningAktivitetType = opptjeningAktivitetType;
            return this;
        }

        public Builder medFom(LocalDate fom) {
            kladd.fom = fom;
            return this;
        }

        public Builder medTom(LocalDate tom) {
            kladd.tom = tom;
            return this;
        }

        public Builder medOppdragsgiverOrg(String oppdragsgiverOrg) {
            kladd.oppdragsgiverOrg = oppdragsgiverOrg;
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

        public Builder medSkalBrukes(boolean skalBrukes) {
            kladd.skalBrukes = skalBrukes;
            return this;
        }

        public BeregningsaktivitetLagreDto build() {
            return kladd;
        }
    }
}
