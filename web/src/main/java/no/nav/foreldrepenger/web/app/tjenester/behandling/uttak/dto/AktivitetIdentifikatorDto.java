package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;

public record AktivitetIdentifikatorDto(UttakArbeidType uttakArbeidType, String arbeidsgiverReferanse, String arbeidsforholdId) {

}
