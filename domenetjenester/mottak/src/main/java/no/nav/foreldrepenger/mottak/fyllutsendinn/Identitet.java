package no.nav.foreldrepenger.mottak.fyllutsendinn;

/** Shared identity component: fødselsnummer or D-nummer. */
public record Identitet(JaNei harDuFodselsnummer, String identitetsnummer) {}

