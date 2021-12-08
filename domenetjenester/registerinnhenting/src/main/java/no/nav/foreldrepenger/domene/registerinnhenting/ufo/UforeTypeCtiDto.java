package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

public record UforeTypeCtiDto(String code,
                              String decode,
                              boolean valid,
                              String fromDate,
                              String toDate) {
}
