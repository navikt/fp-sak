package no.nav.foreldrepenger.domene.modell;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;


public class FaktaArbeidsforhold {

    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private FaktaVurdering erTidsbegrenset;
    private FaktaVurdering harMottattYtelse;
    private FaktaVurdering harLønnsendringIBeregningsperioden;

    public FaktaArbeidsforhold(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRefDto) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRefDto;
    }

    public FaktaArbeidsforhold(FaktaArbeidsforhold original) {
        this.arbeidsgiver = Arbeidsgiver.fra(original.getArbeidsgiver());
        this.arbeidsforholdRef = original.getArbeidsforholdRef();
        this.erTidsbegrenset = original.getErTidsbegrenset();
        this.harMottattYtelse = original.getHarMottattYtelse();
        this.harLønnsendringIBeregningsperioden = original.getHarLønnsendringIBeregningsperioden();
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef == null ? InternArbeidsforholdRef.nullRef() : arbeidsforholdRef;
    }

    public boolean gjelderFor(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        return Objects.equals(this.getArbeidsgiver(), arbeidsgiver) && this.getArbeidsforholdRef().gjelderFor(arbeidsforholdRef);
    }

    public Boolean getErTidsbegrensetVurdering() {
        return finnVurdering(erTidsbegrenset);
    }

    public Boolean getHarMottattYtelseVurdering() {
        return finnVurdering(harMottattYtelse);
    }

    public Boolean getHarLønnsendringIBeregningsperiodenVurdering() {
        return finnVurdering(harLønnsendringIBeregningsperioden);
    }

    private Boolean finnVurdering(FaktaVurdering vurdering) {
        return vurdering == null ? null : vurdering.getVurdering();
    }

    public FaktaVurdering getErTidsbegrenset() {
        return erTidsbegrenset;
    }

    public FaktaVurdering getHarMottattYtelse() {
        return harMottattYtelse;
    }

    public FaktaVurdering getHarLønnsendringIBeregningsperioden() {
        return harLønnsendringIBeregningsperioden;
    }

    @Override
    public String toString() {
        return "FaktaArbeidsforholdDto{" + "arbeidsgiver=" + arbeidsgiver + ", arbeidsforholdRef=" + arbeidsforholdRef + ", erTidsbegrenset="
            + erTidsbegrenset + ", harMottattYtelse=" + harMottattYtelse + '}';
    }

    public static Builder builder(FaktaArbeidsforhold kopi) {
        return new Builder(kopi);
    }

    public static Builder builder(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRefDto) {
        return new Builder(arbeidsgiver, arbeidsforholdRefDto);
    }

    public static class Builder {
        private FaktaArbeidsforhold mal;

        public Builder(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
            mal = new FaktaArbeidsforhold(arbeidsgiver, arbeidsforholdRef);
        }

        private Builder(FaktaArbeidsforhold faktaArbeidsforholdDto) {
            mal = new FaktaArbeidsforhold(faktaArbeidsforholdDto);
        }

        static Builder oppdater(FaktaArbeidsforhold faktaArbeidsforholdDto) {
            return new Builder(faktaArbeidsforholdDto);
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            mal.arbeidsgiver = arbeidsgiver;
            return this;
        }

        public Builder medArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
            mal.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medHarMottattYtelse(FaktaVurdering harMottattYtelse) {
            mal.harMottattYtelse = harMottattYtelse;
            return this;
        }

        public Builder medErTidsbegrenset(FaktaVurdering erTidsbegrenset) {
            mal.erTidsbegrenset = erTidsbegrenset;
            return this;
        }

        public Builder medHarLønnsendringIBeregningsperioden(FaktaVurdering harLønnsendringIBeregningsperioden) {
            mal.harLønnsendringIBeregningsperioden = harLønnsendringIBeregningsperioden;
            return this;
        }

        public FaktaArbeidsforhold build() {
            verifyStateForBuild();
            return mal;
        }

        private void verifyStateForBuild() {
            Objects.requireNonNull(mal.arbeidsgiver, "arbeidsgiver");
            if (manglerFakta()) {
                throw new IllegalStateException("Må ha satt minst et faktafelt.");
            }
        }

        // Brukes av fp-sak og må vere public
        public boolean manglerFakta() {
            return mal.erTidsbegrenset == null && mal.harLønnsendringIBeregningsperioden == null && mal.harMottattYtelse == null;
        }
    }
}
