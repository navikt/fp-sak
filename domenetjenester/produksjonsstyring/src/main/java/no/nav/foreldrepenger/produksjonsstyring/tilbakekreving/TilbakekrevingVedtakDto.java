package no.nav.foreldrepenger.produksjonsstyring.tilbakekreving;

import java.time.LocalDate;

public record TilbakekrevingVedtakDto(Long behandlingId,
                                      LocalDate tilbakekrevingVedtakDato,
                                      String tilbakekrevingBehandlingType) {
}
