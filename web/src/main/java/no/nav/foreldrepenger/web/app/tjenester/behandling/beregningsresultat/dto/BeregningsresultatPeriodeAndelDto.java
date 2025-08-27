package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;

public class BeregningsresultatPeriodeAndelDto {
    @NotNull private String arbeidsgiverReferanse;
    @NotNull private final Integer refusjon;
    @NotNull private final Integer tilSoker;
    @NotNull private final UttakDto uttak;
    @NotNull private final BigDecimal utbetalingsgrad;
    @NotNull private final LocalDate sisteUtbetalingsdato;
    @NotNull private final AktivitetStatus aktivitetStatus;
    private final String arbeidsforholdId;
    @NotNull private final String eksternArbeidsforholdId;
    private final String aktørId;
    @NotNull private final OpptjeningAktivitetType arbeidsforholdType;
    private final BigDecimal stillingsprosent;

    private BeregningsresultatPeriodeAndelDto(Builder builder) {
        this.arbeidsgiverReferanse = builder.arbeidsgiverReferanse;
        this.refusjon = builder.refusjon;
        this.tilSoker = builder.tilSøker;
        this.uttak = builder.uttak;
        this.utbetalingsgrad = builder.utbetalingsgrad;
        this.sisteUtbetalingsdato = builder.sisteUtbetalingsdato;
        this.aktivitetStatus = builder.aktivitetStatus;
        this.arbeidsforholdId = builder.arbeidsforholdId;
        this.eksternArbeidsforholdId = builder.eksternArbeidsforholdId;
        this.aktørId = builder.aktørId;
        this.arbeidsforholdType = builder.arbeidsforholdType;
        this.stillingsprosent = builder.stillingsprosent;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public Integer getRefusjon() {
        return refusjon;
    }

    public Integer getTilSoker() {
        return tilSoker;
    }

    public UttakDto getUttak() {
        return uttak;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public LocalDate getSisteUtbetalingsdato() {
        return sisteUtbetalingsdato;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    public String getAktørId() {
        return aktørId;
    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public BigDecimal getStillingsprosent() {
        return stillingsprosent;
    }

    public static Builder build() {
        return new Builder();
    }

    public static class Builder {
        private String arbeidsgiverReferanse;
        private Integer refusjon;
        private Integer tilSøker;
        private BigDecimal utbetalingsgrad;
        private UttakDto uttak;
        private LocalDate sisteUtbetalingsdato;
        private AktivitetStatus aktivitetStatus;
        private String arbeidsforholdId;
        private String eksternArbeidsforholdId;
        private String aktørId;
        private OpptjeningAktivitetType arbeidsforholdType;
        private BigDecimal stillingsprosent;

        private Builder() {
        }

        public Builder medArbeidsgiverReferanse(String ref) {
            this.arbeidsgiverReferanse = ref;
            return this;
        }
        public Builder medRefusjon(Integer refusjon) {
            this.refusjon = refusjon;
            return this;
        }

        public Builder medTilSøker(Integer tilSøker) {
            this.tilSøker = tilSøker;
            return this;
        }

        public Builder medUtbetalingsgrad(BigDecimal utbetalingsgrad) {
            this.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medSisteUtbetalingsdato(LocalDate sisteUtbetalingsdato) {
            this.sisteUtbetalingsdato = sisteUtbetalingsdato;
            return this;
        }

        public Builder medAktivitetstatus(AktivitetStatus aktivitetStatus) {
            this.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public Builder medArbeidsforholdId(String arbeidsforholdId) {
            this.arbeidsforholdId = arbeidsforholdId;
            return this;
        }

        public Builder medEksternArbeidsforholdId(String eksternArbeidsforholdId) {
            this.eksternArbeidsforholdId = eksternArbeidsforholdId;
            return this;
        }

        public Builder medAktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder medArbeidsforholdType(OpptjeningAktivitetType arbeidsforholdType) {
            this.arbeidsforholdType = arbeidsforholdType;
            return this;
        }

        public Builder medUttak(UttakDto uttak) {
            this.uttak = uttak;
            return this;
        }

        public Builder medStillingsprosent(BigDecimal stillingsprosent) {
            this.stillingsprosent = stillingsprosent;
            return this;
        }

        public BeregningsresultatPeriodeAndelDto create() {
            return new BeregningsresultatPeriodeAndelDto(this);
        }
    }
}
