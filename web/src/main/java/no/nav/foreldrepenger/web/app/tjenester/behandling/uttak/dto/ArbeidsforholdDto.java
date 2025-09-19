package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import jakarta.validation.constraints.Pattern;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public record ArbeidsforholdDto(@Pattern(regexp = InputValideringRegex.ARBEIDSGIVER) String arbeidsgiverReferanse,
                                @ValidKodeverk UttakArbeidType arbeidType) {

    public static ArbeidsforholdDto ordinært(String arbeidsgiverReferanse) {
        return new ArbeidsforholdDto(arbeidsgiverReferanse, UttakArbeidType.ORDINÆRT_ARBEID);
    }

    public static ArbeidsforholdDto frilans() {
        return new ArbeidsforholdDto(null, UttakArbeidType.FRILANS);
    }

    public static ArbeidsforholdDto selvstendigNæringsdrivende() {
        return new ArbeidsforholdDto(null, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    @Override
    public String toString() {
        return arbeidType == null ? null : arbeidType.name();
    }
}
