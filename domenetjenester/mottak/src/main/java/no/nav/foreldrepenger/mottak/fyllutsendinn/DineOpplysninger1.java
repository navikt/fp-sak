package no.nav.foreldrepenger.mottak.fyllutsendinn;

import jakarta.validation.Valid;

/**
 * Shared "dineOpplysninger1" container – present in all forms.
 * Contains personal information pre-filled from national registry.
 */
public record DineOpplysninger1(
    String fornavn,
    String etternavn,
    @Valid Identitet identitet,
    @Valid NavAddress adresse,
    @Valid AddressValidity adresseVarighet
) {}
