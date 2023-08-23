package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import java.math.BigDecimal;

public class UttakResultatPeriodeAktivitetDto {

    private StønadskontoType stønadskontoType;
    private Trekkdager trekkdager;
    private BigDecimal prosentArbeid;
    private String arbeidsforholdId;
    private String eksternArbeidsforholdId;
    private String arbeidsgiverReferanse;
    private Utbetalingsgrad utbetalingsgrad;
    private UttakArbeidType uttakArbeidType;
    private boolean gradering;

    private UttakResultatPeriodeAktivitetDto() {
    }

    public StønadskontoType getStønadskontoType() {
        return stønadskontoType;
    }

    @JsonProperty("trekkdagerDesimaler")
    public BigDecimal getTrekkdager() {
        return trekkdager.decimalValue();
    }

    public BigDecimal getProsentArbeid() {
        return prosentArbeid;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    public String getEksternArbeidsforholdId() {
        return eksternArbeidsforholdId;
    }

    public Utbetalingsgrad getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public UttakArbeidType getUttakArbeidType() {
        return uttakArbeidType;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public boolean isGradering() {
        return gradering;
    }

    public static class Builder {

        private UttakResultatPeriodeAktivitetDto kladd = new UttakResultatPeriodeAktivitetDto();

        public Builder medStønadskontoType(StønadskontoType stønadskontoType) {
            kladd.stønadskontoType = stønadskontoType;
            return this;
        }

        public Builder medTrekkdager(Trekkdager trekkdager) {
            kladd.trekkdager = trekkdager;
            return this;
        }

        public Builder medProsentArbeid(BigDecimal prosentArbeid) {
            kladd.prosentArbeid = prosentArbeid;
            return this;
        }

        public Builder medUtbetalingsgrad(Utbetalingsgrad utbetalingsgrad) {
            kladd.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medArbeidsforhold(InternArbeidsforholdRef ref, String eksternArbeidsforholdId, String arbeidsgiverReferanse) {
            kladd.arbeidsforholdId = ref == null ? null : ref.getReferanse();
            kladd.eksternArbeidsforholdId = eksternArbeidsforholdId;
            kladd.arbeidsgiverReferanse = arbeidsgiverReferanse;
            return this;
        }

        public Builder medGradering(boolean gradering) {
            kladd.gradering = gradering;
            return this;
        }

        public Builder medUttakArbeidType(UttakArbeidType uttakArbeidType) {
            kladd.uttakArbeidType = uttakArbeidType;
            return this;
        }

        public UttakResultatPeriodeAktivitetDto build() {
            return kladd;
        }
    }
}
