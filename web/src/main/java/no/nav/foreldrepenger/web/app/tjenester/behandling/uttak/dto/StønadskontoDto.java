package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record StønadskontoDto(@NotNull SaldoerDto.SaldoVisningStønadskontoType stonadskontotype, @NotNull int maxDager, @NotNull int saldo,
                              @NotNull List<AktivitetSaldoDto> aktivitetSaldoDtoList, @NotNull boolean gyldigForbruk, KontoUtvidelser kontoUtvidelser,
                              KontoReduksjoner kontoReduksjoner) {

    public record KontoUtvidelser(@NotNull int prematurdager, @NotNull int flerbarnsdager) {}
    public record KontoReduksjoner(@NotNull BigDecimal annenForelderEøsUttak) {}

}
