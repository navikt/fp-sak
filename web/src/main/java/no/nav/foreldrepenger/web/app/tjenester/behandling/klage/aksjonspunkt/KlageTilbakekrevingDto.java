package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.vedtak.util.InputValideringRegex;

import java.time.LocalDate;
import java.util.UUID;

public record KlageTilbakekrevingDto(@NotNull UUID tilbakekrevingUuid, LocalDate tilbakekrevingVedtakDato, @Size(min = 2, max = 2) @Pattern(regexp = InputValideringRegex.KODEVERK) String tilbakekrevingBehandlingType) {
}
