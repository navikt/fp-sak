package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.util.List;

public record UforehistorikkDto(List<UforeperiodeDto> uforeperiodeListe) {
    public List<UforeperiodeDto> uforeperioder() {
        return uforeperiodeListe == null ? List.of() : uforeperiodeListe;
    }
}
