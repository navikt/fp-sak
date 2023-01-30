package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.familiehendelse.rest.PeriodeDto;

public class YtelseFordelingDto {
    private List<PeriodeDto> ikkeOmsorgPerioder;
    private Boolean overstyrtOmsorg;
    private Boolean bekreftetAleneomsorg;
    private RettigheterAnnenforelderDto rettigheterAnnenforelder;
    private LocalDate endringsdato;
    private int gjeldendeDekningsgrad;
    private LocalDate førsteUttaksdato;
    private boolean ønskerJustertVedFødsel;

    private YtelseFordelingDto() {
    }

    public Boolean getOverstyrtOmsorg() {
        return overstyrtOmsorg;
    }

    public List<PeriodeDto> getIkkeOmsorgPerioder() {
        return ikkeOmsorgPerioder;
    }

    public Boolean getBekreftetAleneomsorg() {
        return bekreftetAleneomsorg;
    }

    public LocalDate getEndringsdato() {
        return endringsdato;
    }

    public RettigheterAnnenforelderDto getRettigheterAnnenforelder() {
        return rettigheterAnnenforelder;
    }

    public int getGjeldendeDekningsgrad() {
        return gjeldendeDekningsgrad;
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


        public Builder medIkkeOmsorgPerioder(List<PeriodeDto> ikkeOmsorgPerioder) {
            kladd.ikkeOmsorgPerioder = ikkeOmsorgPerioder;
            return this;
        }

        public Builder medBekreftetAleneomsorg(Boolean bekreftetAleneomsorg) {
            kladd.bekreftetAleneomsorg = bekreftetAleneomsorg;
            return this;
        }

        public Builder medEndringsdato(LocalDate endringsDato) {
            kladd.endringsdato = endringsDato;
            return this;
        }

        public Builder medRettigheterAnnenforelder(RettigheterAnnenforelderDto rettighetAnnenforelder) {
            kladd.rettigheterAnnenforelder = rettighetAnnenforelder;
            return this;
        }

        public Builder medGjeldendeDekningsgrad(int gjeldendeDekningsgrad){
            kladd.gjeldendeDekningsgrad = gjeldendeDekningsgrad;
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
