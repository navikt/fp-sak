package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

public record UforeperiodeDto(Integer uforegrad,
                              String uforetidspunkt,
                              String virk,
                              UforeTypeCtiDto uforetype,
                              String uforetidspunktTom,
                              String ufgFom,
                              String ufgTom) {
}
