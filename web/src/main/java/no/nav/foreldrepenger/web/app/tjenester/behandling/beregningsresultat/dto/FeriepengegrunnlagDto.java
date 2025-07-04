package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FeriepengegrunnlagDto(@NotNull @Valid @Size(max = 100) List<FeriepengegrunnlagAndelDto> andeler) {
}
