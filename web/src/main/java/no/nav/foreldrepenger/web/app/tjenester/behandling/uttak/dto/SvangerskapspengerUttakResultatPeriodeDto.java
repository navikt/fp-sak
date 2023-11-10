package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;

public class SvangerskapspengerUttakResultatPeriodeDto {
    private Utbetalingsgrad utbetalingsgrad;
    private PeriodeResultatType periodeResultatType;
    private PeriodeIkkeOppfyltÅrsak periodeIkkeOppfyltÅrsak;
    private LocalDate fom;
    private LocalDate tom;

    private SvangerskapspengerUttakResultatPeriodeDto(Builder builder) {
        utbetalingsgrad = builder.utbetalingsgrad;
        periodeResultatType = builder.periodeResultatType;
        periodeIkkeOppfyltÅrsak = builder.periodeIkkeOppfyltÅrsak;
        fom = builder.fom;
        tom = builder.tom;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Utbetalingsgrad getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public PeriodeResultatType getPeriodeResultatType() {
        return periodeResultatType;
    }

    public PeriodeIkkeOppfyltÅrsak getPeriodeIkkeOppfyltÅrsak() {
        return periodeIkkeOppfyltÅrsak;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public static final class Builder {
        private Utbetalingsgrad utbetalingsgrad;
        private PeriodeResultatType periodeResultatType;
        private PeriodeIkkeOppfyltÅrsak periodeIkkeOppfyltÅrsak;
        private LocalDate fom;
        private LocalDate tom;

        private Builder() {
        }

        public Builder medUtbetalingsgrad(Utbetalingsgrad utbetalingsgrad) {
            this.utbetalingsgrad = utbetalingsgrad;
            return this;
        }

        public Builder medPeriodeResultatType(PeriodeResultatType periodeResultatType) {
            this.periodeResultatType = periodeResultatType;
            return this;
        }

        public Builder medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak periodeIkkeOppfyltÅrsak) {
            this.periodeIkkeOppfyltÅrsak = periodeIkkeOppfyltÅrsak;
            return this;
        }

        public Builder medfom(LocalDate fom) {
            this.fom = fom;
            return this;
        }

        public Builder medTom(LocalDate tom) {
            this.tom = tom;
            return this;
        }

        public SvangerskapspengerUttakResultatPeriodeDto build() {
            return new SvangerskapspengerUttakResultatPeriodeDto(this);
        }
    }
}
