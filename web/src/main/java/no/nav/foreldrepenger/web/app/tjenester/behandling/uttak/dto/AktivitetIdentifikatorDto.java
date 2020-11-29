package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;

public class AktivitetIdentifikatorDto {

    private final UttakArbeidType uttakArbeidType;
    private String arbeidsgiverReferanse;
    private final String arbeidsforholdId;

    public AktivitetIdentifikatorDto(UttakArbeidType uttakArbeidType, String arbeidsgiverReferanse, String arbeidsforholdId) {
        this.uttakArbeidType = uttakArbeidType;
        this.arbeidsgiverReferanse = arbeidsgiverReferanse;
        this.arbeidsforholdId = arbeidsforholdId;
    }

    public UttakArbeidType getUttakArbeidType() {
        return uttakArbeidType;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AktivitetIdentifikatorDto that = (AktivitetIdentifikatorDto) o;
        return Objects.equals(uttakArbeidType, that.uttakArbeidType) &&
            Objects.equals(arbeidsgiverReferanse, that.arbeidsgiverReferanse) &&
            Objects.equals(arbeidsforholdId, that.arbeidsforholdId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(uttakArbeidType, arbeidsgiverReferanse, arbeidsforholdId);
    }
}
