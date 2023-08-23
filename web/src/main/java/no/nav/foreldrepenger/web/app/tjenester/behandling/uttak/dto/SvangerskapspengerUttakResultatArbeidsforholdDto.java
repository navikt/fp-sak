package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.ArbeidsforholdIkkeOppfyltÅrsak;

import java.util.List;

public class SvangerskapspengerUttakResultatArbeidsforholdDto {
    private ArbeidsforholdIkkeOppfyltÅrsak arbeidsforholdIkkeOppfyltÅrsak;
    private String arbeidsgiverReferanse;
    private UttakArbeidType arbeidType;

    private List<SvangerskapspengerUttakResultatPeriodeDto> perioder;

    public static Builder builder() {
        return new Builder();
    }

    public ArbeidsforholdIkkeOppfyltÅrsak getArbeidsforholdIkkeOppfyltÅrsak() {
        return arbeidsforholdIkkeOppfyltÅrsak;
    }

    public String getArbeidsgiverReferanse() {
        return arbeidsgiverReferanse;
    }

    public UttakArbeidType getArbeidType() {
        return arbeidType;
    }

    public List<SvangerskapspengerUttakResultatPeriodeDto> getPerioder() {
        return perioder;
    }

    public static final class Builder {
        private SvangerskapspengerUttakResultatArbeidsforholdDto kladd;

        private Builder() {
            this.kladd = new SvangerskapspengerUttakResultatArbeidsforholdDto();
        }

        public Builder medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak arbeidsforholdIkkeOppfyltÅrsak) {
            this.kladd.arbeidsforholdIkkeOppfyltÅrsak = arbeidsforholdIkkeOppfyltÅrsak;
            return this;
        }

        public Builder medArbeidsgiver(String arbeidsgiverReferanse) {
            this.kladd.arbeidsgiverReferanse = arbeidsgiverReferanse;
            return this;
        }

        public Builder medArbeidType(UttakArbeidType uttakArbeidType) {
            this.kladd.arbeidType = uttakArbeidType;
            return this;
        }

        public Builder medPerioder(List<SvangerskapspengerUttakResultatPeriodeDto> perioder) {
            this.kladd.perioder = perioder;
            return this;
        }

        public SvangerskapspengerUttakResultatArbeidsforholdDto build() {
            return kladd;
        }
    }
}

