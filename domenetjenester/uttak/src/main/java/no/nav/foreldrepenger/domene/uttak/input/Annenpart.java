package no.nav.foreldrepenger.domene.uttak.input;

import java.time.LocalDateTime;
import java.util.Objects;

public record Annenpart(Long gjeldendeVedtakBehandlingId, LocalDateTime søknadOpprettetTidspunkt) {

    public Annenpart {
        Objects.requireNonNull(gjeldendeVedtakBehandlingId,
            "Uttak bryr seg bare om annenpart hvis det foreligger et vedtak");
    }
}
