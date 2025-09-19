package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import jakarta.validation.constraints.NotNull;

public record AktivitetSaldoDto(@NotNull AktivitetIdentifikatorDto aktivitetIdentifikator, @NotNull int saldo) {
}
