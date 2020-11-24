package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;

public class ArbeidsforholdDto {

    private final ArbeidsgiverDto arbeidsgiver;
    private String arbeidsgiverReferanse;
    private final UttakArbeidType arbeidType;

    private ArbeidsforholdDto(ArbeidsgiverDto arbeidsgiver, String arbeidsgiverReferanse, UttakArbeidType uttakArbeidType) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
        this.arbeidType = uttakArbeidType;
    }

    public static ArbeidsforholdDto ordinært(ArbeidsgiverDto arbeidsgiver, String arbeidsgiverReferanse) {
        return new ArbeidsforholdDto(arbeidsgiver, arbeidsgiverReferanse, UttakArbeidType.ORDINÆRT_ARBEID);
    }

    public static ArbeidsforholdDto frilans() {
        return new ArbeidsforholdDto(null, null, UttakArbeidType.FRILANS);
    }

    public static ArbeidsforholdDto selvstendigNæringsdrivende() {
        return new ArbeidsforholdDto(null, null, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    public ArbeidsgiverDto getArbeidsgiver() {
        return arbeidsgiver;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
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
            Objects.equals(arbeidsgiverReferanse, that.arbeidsgiverReferanse) &&
            Objects.equals(arbeidType, that.arbeidType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, arbeidsgiverReferanse, arbeidType);
    }
}
