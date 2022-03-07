package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;

public record StønadskontoDto(SaldoerDto.SaldoVisningStønadskontoType stonadskontotype, int maxDager, int saldo,
                              List<AktivitetSaldoDto> aktivitetSaldoDtoList, boolean gyldigForbruk, KontoUtvidelser kontoUtvidelser) {

}
