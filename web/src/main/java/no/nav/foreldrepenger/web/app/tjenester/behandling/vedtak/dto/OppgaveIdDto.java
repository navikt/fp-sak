package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record OppgaveIdDto(@NotNull @Digits(integer = 1000, fraction = 0) String id) { }
