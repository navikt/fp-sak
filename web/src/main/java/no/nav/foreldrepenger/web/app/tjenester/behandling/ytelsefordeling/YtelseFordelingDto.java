package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.familiehendelse.rest.PeriodeDto;

public class YtelseFordelingDto {
    private List<PeriodeDto> ikkeOmsorgPerioder;
    private List<PeriodeDto> aleneOmsorgPerioder;
    private RettighetDto rettighetAleneomsorg;
    private AnnenforelderHarRettDto annenforelderHarRettDto;
    private RettigheterAnnenforelderDto rettigheterAnnenforelder;
    private LocalDate endringsdato;
    private int gjeldendeDekningsgrad;
    private LocalDate førsteUttaksdato;

    private YtelseFordelingDto() {
    }

    public List<PeriodeDto> getIkkeOmsorgPerioder() {
        return ikkeOmsorgPerioder;
    }

    public List<PeriodeDto> getAleneOmsorgPerioder() {
        return aleneOmsorgPerioder;
    }

    public RettighetDto getRettighetAleneomsorg() {
        return rettighetAleneomsorg;
    }

    public LocalDate getEndringsdato() {
        return endringsdato;
    }

    public AnnenforelderHarRettDto getAnnenforelderHarRettDto() {
        return annenforelderHarRettDto;
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

    public static class Builder {

        private final YtelseFordelingDto kladd = new YtelseFordelingDto();

        public Builder medIkkeOmsorgPerioder(List<PeriodeDto> ikkeOmsorgPerioder) {
            kladd.ikkeOmsorgPerioder = ikkeOmsorgPerioder;
            return this;
        }

        public Builder medAleneOmsorgPerioder(List<PeriodeDto> aleneOmsorgPerioder) {
            kladd.aleneOmsorgPerioder = aleneOmsorgPerioder;
            return this;
        }

        public Builder medRettighetAleneomsorg(RettighetDto rettighetAleneomsorg) {
            kladd.rettighetAleneomsorg = rettighetAleneomsorg;
            return this;
        }

        public Builder medEndringsdato(LocalDate endringsDato) {
            kladd.endringsdato = endringsDato;
            return this;
        }

        public Builder medAnnenforelderHarRett(AnnenforelderHarRettDto annenforelderHarRettDto) {
            kladd.annenforelderHarRettDto = annenforelderHarRettDto;
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

        public YtelseFordelingDto build() {
            return kladd;
        }
    }
}
