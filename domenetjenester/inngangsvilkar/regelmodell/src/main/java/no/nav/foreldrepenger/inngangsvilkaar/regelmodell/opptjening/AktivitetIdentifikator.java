package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

public interface AktivitetIdentifikator {
    String value();

    default String tilMaskertNummer(String identifikator) {
        if (identifikator == null) {
            return null;
        }
        int length = identifikator.length();
        if (length <= 4) {
            return "*".repeat(length);
        }
        return "*".repeat(length - 4) + identifikator.substring(length - 4);
    }
}
