package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.time.LocalDate;

public class YtelseFordelingDto {
    private Boolean overstyrtOmsorg;
    private Boolean bekreftetAleneomsorg;
    private RettigheterAnnenforelderDto rettigheterAnnenforelder;
    private LocalDate førsteUttaksdato;
    private boolean ønskerJustertVedFødsel;

    private YtelseFordelingDto() {
    }

    public Boolean getOverstyrtOmsorg() {
        return overstyrtOmsorg;
    }

    public Boolean getBekreftetAleneomsorg() {
        return bekreftetAleneomsorg;
    }

    public RettigheterAnnenforelderDto getRettigheterAnnenforelder() {
        return rettigheterAnnenforelder;
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


        public Builder medBekreftetAleneomsorg(Boolean bekreftetAleneomsorg) {
            kladd.bekreftetAleneomsorg = bekreftetAleneomsorg;
            return this;
        }

        public Builder medRettigheterAnnenforelder(RettigheterAnnenforelderDto rettighetAnnenforelder) {
            kladd.rettigheterAnnenforelder = rettighetAnnenforelder;
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
