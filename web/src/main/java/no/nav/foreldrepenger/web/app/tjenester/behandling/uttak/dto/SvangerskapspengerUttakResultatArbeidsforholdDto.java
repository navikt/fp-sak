package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.ArbeidsforholdIkkeOppfyltÅrsak;

public class SvangerskapspengerUttakResultatArbeidsforholdDto {
    private ArbeidsforholdIkkeOppfyltÅrsak arbeidsforholdIkkeOppfyltÅrsak;
    private ArbeidsgiverDto arbeidsgiver;
    private UttakArbeidType arbeidType;

    private List<SvangerskapspengerUttakResultatPeriodeDto> perioder;

    private SvangerskapspengerUttakResultatArbeidsforholdDto(Builder builder) {
        arbeidsforholdIkkeOppfyltÅrsak = builder.arbeidsforholdIkkeOppfyltÅrsak;
        arbeidsgiver = builder.arbeidsgiver;
        arbeidType = builder.arbeidType;
        perioder = builder.perioder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ArbeidsforholdIkkeOppfyltÅrsak getArbeidsforholdIkkeOppfyltÅrsak() {
        return arbeidsforholdIkkeOppfyltÅrsak;
    }

    public ArbeidsgiverDto getArbeidsgiver() {
        return arbeidsgiver;
    }

    public UttakArbeidType getArbeidType() {
        return arbeidType;
    }

    public List<SvangerskapspengerUttakResultatPeriodeDto> getPerioder() {
        return perioder;
    }

    public static final class Builder {
        private ArbeidsforholdIkkeOppfyltÅrsak arbeidsforholdIkkeOppfyltÅrsak;
        private ArbeidsgiverDto arbeidsgiver;
        private UttakArbeidType arbeidType;
        private List<SvangerskapspengerUttakResultatPeriodeDto> perioder;

        private Builder() {
        }

        public Builder medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak arbeidsforholdIkkeOppfyltÅrsak) {
            this.arbeidsforholdIkkeOppfyltÅrsak = arbeidsforholdIkkeOppfyltÅrsak;
            return this;
        }

        public Builder medArbeidsgiver(ArbeidsgiverDto arbeidsgiver) {
            this.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medArbeidType(UttakArbeidType uttakArbeidType) {
            this.arbeidType = uttakArbeidType;
            return this;
        }

        public Builder medPerioder(List<SvangerskapspengerUttakResultatPeriodeDto> perioder) {
            this.perioder = perioder;
            return this;
        }

        public SvangerskapspengerUttakResultatArbeidsforholdDto build() {
            return new SvangerskapspengerUttakResultatArbeidsforholdDto(this);
        }
    }
}

