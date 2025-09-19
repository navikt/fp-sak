package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class YtelseFordelingDto {
    private Boolean overstyrtOmsorg;
    @NotNull private LocalDate førsteUttaksdato;
    private boolean ønskerJustertVedFødsel;

    private YtelseFordelingDto() {
    }

    public Boolean getOverstyrtOmsorg() {
        return overstyrtOmsorg;
    }

    public LocalDate getFørsteUttaksdato() {
        return førsteUttaksdato;
    }

    public boolean isØnskerJustertVedFødsel() {
        return ønskerJustertVedFødsel;
    }

    public static class Builder {

        private final YtelseFordelingDto kladd = new YtelseFordelingDto();

        public Builder medOverstyrtOmsorg(Boolean overstyrtOmsorg) {
            kladd.overstyrtOmsorg = overstyrtOmsorg;
            return this;
        }

        public Builder medFørsteUttaksdato(LocalDate førsteUttaksdato) {
            kladd.førsteUttaksdato = førsteUttaksdato;
            return this;
        }

        public Builder medØnskerJustertVedFødsel(boolean ønskerJustertVedFødsel) {
            kladd.ønskerJustertVedFødsel = ønskerJustertVedFødsel;
            return this;
        }

        public YtelseFordelingDto build() {
            return kladd;
        }
    }
}
