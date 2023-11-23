package no.nav.foreldrepenger.datavarehus.v2;

import jakarta.validation.constraints.NotNull;

record Inngangsvilkår(@NotNull String vilkaarType, @NotNull Utfall utfall, String avslagÅrsak) {
    // Denne trenges ikke
    private enum Utfall {
        OPPFYLT,
        IKKE_OPPFYLT,
        IKKE_VURDERT
    }
}
