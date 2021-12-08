package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.util.Date;
import java.util.List;

public record UforehistorikkDto(Date reaktiviseringFomDato, Date reaktiviseringTomDato, List<UforeperiodeDto> uforeperiodeListe) {
    public List<UforeperiodeDto> uforeperioder() {
        return uforeperiodeListe == null ? List.of() : uforeperiodeListe;
    }
}
