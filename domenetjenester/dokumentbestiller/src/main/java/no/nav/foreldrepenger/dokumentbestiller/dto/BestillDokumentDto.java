package no.nav.foreldrepenger.dokumentbestiller.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import no.nav.folketrygdloven.kalkulus.annoteringer.Fritekst;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public record BestillDokumentDto(@Valid UUID behandlingUuid,
                                 @ValidKodeverk @NotNull DokumentMalType brevmalkode,
                                 @Valid @Fritekst @Size(max = 20_000) String fritekst,
                                 @ValidKodeverk RevurderingVarslingÅrsak arsakskode) {
}

