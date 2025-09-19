package no.nav.foreldrepenger.domene.opptjening.dto;

import jakarta.validation.constraints.NotNull;

public record FerdiglignetNæringDto(@NotNull String år, @NotNull Long beløp) {
}
