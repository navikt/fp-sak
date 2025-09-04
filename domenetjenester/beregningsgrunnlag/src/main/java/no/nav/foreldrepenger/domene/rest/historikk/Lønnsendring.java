package no.nav.foreldrepenger.domene.rest.historikk;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.validering.ValidKodeverk;


public class Lønnsendring {
    private Integer gammelArbeidsinntekt;
    private Integer nyArbeidsinntekt;
    private Integer nyArbeidsinntektPrÅr;
    @ValidKodeverk
    private Inntektskategori gammelInntektskategori;
    @ValidKodeverk
    private Inntektskategori nyInntektskategori;
    private Integer gammelRefusjonPrÅr;
    private Integer nyRefusjonPrÅr;
    private Integer nyTotalRefusjonPrÅr;
    private boolean nyAndel;
    private Integer gammelArbeidsinntektPrÅr;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    @ValidKodeverk
    private AktivitetStatus aktivitetStatus;

    private Lønnsendring() {
    }

    public void setNyAndel(boolean nyAndel) {
        this.nyAndel = nyAndel;
    }

    public boolean isNyAndel() {
        return nyAndel;
    }

    public Integer getGammelArbeidsinntekt() {
        return gammelArbeidsinntekt;
    }

    public Integer getNyArbeidsinntekt() {
        return nyArbeidsinntekt;
    }

    public Integer getNyArbeidsinntektPrÅr() {
        return nyArbeidsinntektPrÅr;
    }

    public Integer getGammelArbeidsinntektPrÅr() {
        return gammelArbeidsinntektPrÅr;
    }

    public Inntektskategori getGammelInntektskategori() {
        return gammelInntektskategori;
    }

    public Inntektskategori getNyInntektskategori() {
        return nyInntektskategori;
    }

    public Integer getGammelRefusjonPrÅr() {
        return gammelRefusjonPrÅr;
    }

    public Integer getNyRefusjonPrÅr() {
        return nyRefusjonPrÅr;
    }

    public Integer getNyTotalRefusjonPrÅr() {
        return nyTotalRefusjonPrÅr;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public Optional<InternArbeidsforholdRef> getArbeidsforholdRef() {
        return Optional.ofNullable(arbeidsforholdRef);
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public static class Builder {
        private Lønnsendring lønnsendringMal;

        public Builder() {
            lønnsendringMal = new Lønnsendring();
        }

        Builder(Lønnsendring lønnsendring) {
            lønnsendringMal = lønnsendring;
        }

        public static Builder ny(){
            return new Builder();
        }

        public Builder medNyAndel(boolean nyAndel) {
            lønnsendringMal.nyAndel = nyAndel;
            return this;
        }

        public Builder medGammelArbeidsinntekt(Integer gammelArbeidsinntekt) {
            lønnsendringMal.gammelArbeidsinntekt = gammelArbeidsinntekt;
            return this;
        }


        public Builder medGammelArbeidsinntektPrÅr(Integer gammelArbeidsinntektPrÅr) {
            lønnsendringMal.gammelArbeidsinntektPrÅr = gammelArbeidsinntektPrÅr;
            return this;
        }


        public Builder medNyArbeidsinntekt(Integer nyArbeidsinntekt) {
            lønnsendringMal.nyArbeidsinntekt = nyArbeidsinntekt;
            return this;
        }

        public Builder medNyArbeidsinntektPrÅr(Integer nyArbeidsinntektPrÅr) {
            lønnsendringMal.nyArbeidsinntektPrÅr = nyArbeidsinntektPrÅr;
            return this;
        }

        public Builder medNyInntektskategori(Inntektskategori nyInntektskategori) {
            lønnsendringMal.nyInntektskategori = nyInntektskategori;
            return this;
        }

        public Builder medGammelInntektskategori(Inntektskategori inntektskategori) {
            lønnsendringMal.gammelInntektskategori = inntektskategori;
            return this;
        }

        public Builder medGammelRefusjonPrÅr(Integer refusjon) {
            lønnsendringMal.gammelRefusjonPrÅr = refusjon;
            return this;
        }

        public Builder medNyRefusjonPrÅr(Integer refusjon) {
            lønnsendringMal.nyRefusjonPrÅr = refusjon;
            return this;
        }

        public Builder medNyTotalRefusjonPrÅr(Integer refusjon) {
            lønnsendringMal.nyTotalRefusjonPrÅr = refusjon;
            return this;
        }

        public Builder medArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
            lønnsendringMal.arbeidsforholdRef = arbeidsforholdRef;
            return this;
        }

        public Builder medArbeidsgiver(Arbeidsgiver arbeidsgiver) {
            lønnsendringMal.arbeidsgiver = arbeidsgiver;
            return this;
        }


        public Builder medAktivitetStatus(AktivitetStatus aktivitetStatus) {
            lønnsendringMal.aktivitetStatus = aktivitetStatus;
            return this;
        }

        public Lønnsendring build() {
            return lønnsendringMal;
        }

    }

}
