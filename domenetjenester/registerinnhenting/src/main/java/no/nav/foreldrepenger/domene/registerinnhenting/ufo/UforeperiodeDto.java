package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.util.Date;

public record UforeperiodeDto(Integer uforegrad,
                              Date uforetidspunkt,
                              Date virk,
                              UforeTypeCtiDto uforetype,
                              Date uforetidspunktTom,
                              Date ufgFom,
                              Date ufgTom) {
}
