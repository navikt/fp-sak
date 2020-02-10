package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;

public class ArbeidsforholdDto {

    private final ArbeidsgiverDto arbeidsgiver;

    private final UttakArbeidType arbeidType;

    private ArbeidsforholdDto(ArbeidsgiverDto arbeidsgiver, UttakArbeidType uttakArbeidType) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidType = uttakArbeidType;
    }

    public static ArbeidsforholdDto ordinært(ArbeidsgiverDto arbeidsgiver) {
        return new ArbeidsforholdDto(arbeidsgiver, UttakArbeidType.ORDINÆRT_ARBEID);
    }

    public static ArbeidsforholdDto frilans() {
        return new ArbeidsforholdDto(null, UttakArbeidType.FRILANS);
    }

    public static ArbeidsforholdDto selvstendigNæringsdrivende() {
        return new ArbeidsforholdDto(null, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    public ArbeidsgiverDto getArbeidsgiver() {
        return arbeidsgiver;
    }

    public UttakArbeidType getArbeidType() {
        return arbeidType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArbeidsforholdDto that = (ArbeidsforholdDto) o;
        return Objects.equals(arbeidsgiver, that.arbeidsgiver) &&
            Objects.equals(arbeidType, that.arbeidType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, arbeidType);
    }
}
