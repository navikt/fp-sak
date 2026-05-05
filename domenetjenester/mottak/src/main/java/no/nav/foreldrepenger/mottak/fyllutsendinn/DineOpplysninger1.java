package no.nav.foreldrepenger.mottak.fyllutsendinn;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Shared "dineOpplysninger1" container – present in several forms.
 * Contains personal information pre-filled from national registry.
 */
public record DineOpplysninger1(
    @NotNull String fornavn,
    @NotNull String etternavn,
    @Valid @NotNull Identitet identitet,
    @Valid NavAddress adresse,
    @Valid AddressValidity adresseVarighet
) {}
