package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;

public record AktivitetIdentifikatorDto(@NotNull UttakArbeidType uttakArbeidType, @NotNull String arbeidsgiverReferanse, String arbeidsforholdId) {

}
