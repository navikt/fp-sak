package no.nav.foreldrepenger.dokumentbestiller.dto;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.folketrygdloven.kalkulus.opptjening.v1.Fritekst;
import no.nav.foreldrepenger.behandlingslager.behandling.RevurderingVarslingÅrsak;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public record ForhåndsvisDokumentDto(@Valid @NotNull UUID behandlingUuid,
                                     @ValidKodeverk DokumentMalType dokumentMal,
                                     @ValidKodeverk RevurderingVarslingÅrsak arsakskode,
                                     boolean automatiskVedtaksbrev,
                                     @Size(max = 200) @Pattern(regexp = InputValideringRegex.FRITEKST) String tittel,
                                     @Valid @Fritekst @Size(max = 20_000) String fritekst) { // HTML eller rå tekst avhengig av maltype FRITEKSTBREV/FRITEKSTBREV_HTML
}

