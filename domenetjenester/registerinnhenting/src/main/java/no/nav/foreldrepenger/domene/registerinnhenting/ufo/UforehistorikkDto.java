package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.util.List;

public record UforehistorikkDto(String reaktiviseringFomDato, String reaktiviseringTomDato, List<UforeperiodeDto> uforeperiodeListe) {
}
