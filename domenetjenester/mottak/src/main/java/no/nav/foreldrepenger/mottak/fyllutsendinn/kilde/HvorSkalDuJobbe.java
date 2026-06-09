package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

public record HvorSkalDuJobbe(
    Boolean hosArbeidsgiver,
    Boolean frilanser,
    Boolean selvstendigNaeringsdrivende
) {
}
