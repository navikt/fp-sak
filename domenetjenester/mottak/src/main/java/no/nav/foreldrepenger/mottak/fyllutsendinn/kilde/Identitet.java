package no.nav.foreldrepenger.mottak.fyllutsendinn.kilde;

/** Shared identity component: fødselsnummer or D-nummer. */
public record Identitet(JaNei harDuFodselsnummer, String identitetsnummer) {}
