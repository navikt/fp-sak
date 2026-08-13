package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

/**
 * Årsak til at et arbeidsforhold ikke kreves inntektsmelding for, uten at det utløser aksjonspunkt.
 * Brukes kun til visning i frontend (se InaktiveArbeidsforholdUtleder), og skal ikke blandes med AksjonspunktÅrsak.
 */
public enum InaktivArbeidsforholdÅrsak {
    INAKTIVT_ARBEIDSFORHOLD,
    PERMISJON
}
