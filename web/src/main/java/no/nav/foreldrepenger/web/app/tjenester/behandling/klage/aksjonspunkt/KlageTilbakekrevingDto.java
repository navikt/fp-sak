package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import java.time.LocalDate;
import java.util.UUID;

public record KlageTilbakekrevingDto(UUID tilbakekrevingUuid, LocalDate tilbakekrevingVedtakDato, String tilbakekrevingBehandlingType) {
}
