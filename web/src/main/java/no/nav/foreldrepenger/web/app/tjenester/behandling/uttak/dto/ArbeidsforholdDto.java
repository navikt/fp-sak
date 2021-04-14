package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;

public class ArbeidsforholdDto {

    private String arbeidsgiverReferanse;
    private final UttakArbeidType arbeidType;

    private ArbeidsforholdDto(String arbeidsgiverReferanse, UttakArbeidType uttakArbeidType) {
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
        this.arbeidType = uttakArbeidType;
    }

    public static ArbeidsforholdDto ordinært(String arbeidsgiverReferanse) {
        return new ArbeidsforholdDto(arbeidsgiverReferanse, UttakArbeidType.ORDINÆRT_ARBEID);
    }

    public static ArbeidsforholdDto frilans() {
        return new ArbeidsforholdDto(null, UttakArbeidType.FRILANS);
    }

    public static ArbeidsforholdDto selvstendigNæringsdrivende() {
        return new ArbeidsforholdDto(null, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE);
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
        var that = (ArbeidsforholdDto) o;
        return Objects.equals(arbeidsgiverReferanse, that.arbeidsgiverReferanse) &&
            Objects.equals(arbeidType, that.arbeidType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiverReferanse, arbeidType);
    }
}
