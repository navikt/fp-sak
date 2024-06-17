package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import no.nav.vedtak.util.InputValideringRegex;

public record KlageTilbakekrevingDto(@NotNull UUID tilbakekrevingUuid, LocalDate tilbakekrevingVedtakDato,
                                     @Pattern(regexp = InputValideringRegex.KODEVERK) String tilbakekrevingBehandlingType) {
}
