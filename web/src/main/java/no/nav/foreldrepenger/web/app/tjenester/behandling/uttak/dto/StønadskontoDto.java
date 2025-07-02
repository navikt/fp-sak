package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;

public record StønadskontoDto(SaldoerDto.SaldoVisningStønadskontoType stonadskontotype, int maxDager, int saldo,
                              List<AktivitetSaldoDto> aktivitetSaldoDtoList, boolean gyldigForbruk, KontoUtvidelser kontoUtvidelser,
                              KontoReduksjoner kontoReduksjoner) {

    public record KontoUtvidelser(int prematurdager, int flerbarnsdager) {}
    public record KontoReduksjoner(int annenForelderEøsUttak) {}

}
