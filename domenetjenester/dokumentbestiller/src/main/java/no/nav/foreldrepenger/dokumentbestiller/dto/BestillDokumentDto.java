package no.nav.foreldrepenger.dokumentbestiller.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public record BestillDokumentDto(@Valid UUID behandlingUuid,
                                 @ValidKodeverk @NotNull DokumentMalType brevmalkode,
                                 @Size(max = 10000) @Pattern(regexp = InputValideringRegex.FRITEKST) String fritekst,
                                 @ValidKodeverk RevurderingVarslingÅrsak arsakskode) {
}

