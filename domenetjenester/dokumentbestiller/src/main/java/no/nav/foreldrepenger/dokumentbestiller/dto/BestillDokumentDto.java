package no.nav.foreldrepenger.dokumentbestiller.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public record BestillDokumentDto(@Valid UUID behandlingUuid,
                                 @ValidKodeverk @NotNull DokumentMalType brevmalkode,
                                 @Valid FritekstDto fritekst,
                                 @ValidKodeverk RevurderingVarslingÅrsak arsakskode) {
}

