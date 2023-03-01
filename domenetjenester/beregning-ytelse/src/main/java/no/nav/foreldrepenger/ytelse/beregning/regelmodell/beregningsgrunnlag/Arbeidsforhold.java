package no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag;

public record Arbeidsforhold(String identifikator,
                             ReferanseType referanseType,
                             String arbeidsforholdId,
                             boolean frilanser) {

    public static Arbeidsforhold frilansArbeidsforhold() {
        return new Arbeidsforhold(null, null, null, true);
    }

    public static Arbeidsforhold nyttArbeidsforholdHosVirksomhet(String orgnr) {
        return new Arbeidsforhold(orgnr, ReferanseType.ORG_NR, null, false);
    }

    public static Arbeidsforhold nyttArbeidsforholdHosVirksomhet(String orgnr, String arbeidsforholdId) {
        return new Arbeidsforhold(orgnr, ReferanseType.ORG_NR, arbeidsforholdId, false);
    }

    public static Arbeidsforhold nyttArbeidsforholdHosPrivatperson(String aktørId) {
        return new Arbeidsforhold(aktørId, ReferanseType.AKTØR_ID, null, false);
    }

    public static Arbeidsforhold nyttArbeidsforholdHosPrivatperson(String aktørId, String arbeidsforholdId) {
        return new Arbeidsforhold(aktørId, ReferanseType.AKTØR_ID, arbeidsforholdId, false);
    }

}
