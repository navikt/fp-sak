package no.nav.foreldrepenger.dokumentbestiller.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public record GenererHtmlDokumentDto(@Valid @NotNull UUID behandlingUuid,
                                     @ValidKodeverk DokumentMalType dokumentMal,
                                     @ValidKodeverk RevurderingVarslingÅrsak arsakskode,
                                     boolean automatiskVedtaksbrev) {
}

