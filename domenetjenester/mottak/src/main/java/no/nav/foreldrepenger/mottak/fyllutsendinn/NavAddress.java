package no.nav.foreldrepenger.mottak.fyllutsendinn;

/**
 * Prefilled address component (navAddress).
 * Fields are pre-populated from the national registry (Folkeregisteret).
 */
public record NavAddress(String adresse, String postnummer, String poststed, String land) {}
