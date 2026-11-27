package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record FordelingsgrunnlagDto(@NotNull List<@Valid RefusjonsandelDto> refusjonsandeler) {
}
