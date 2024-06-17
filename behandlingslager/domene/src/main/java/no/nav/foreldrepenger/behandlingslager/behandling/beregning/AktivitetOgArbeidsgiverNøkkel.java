package no.nav.foreldrepenger.behandlingslager.behandling.beregning;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

/**
 * Nøkkel som brukes av VurderBehovForÅHindreTilbaketrekkV2 for å unikt identifisere andeler uten å skille på ulike andeler hos samme arbeidsgiver
 */
public class AktivitetOgArbeidsgiverNøkkel {
    private final Arbeidsgiver arbeidsgiver;
    private final AktivitetStatus aktivitetStatus;

    public AktivitetOgArbeidsgiverNøkkel(BeregningsresultatAndel andel) {
        Objects.requireNonNull(andel.getAktivitetStatus(), "aktivitetStatus");
        this.arbeidsgiver = andel.getArbeidsgiver().orElse(null);
        this.aktivitetStatus = andel.getAktivitetStatus();
    }

    @Override
    public String toString() {
        return "BeregningsresultatAktivitetsnøkkelV2{" + "arbeidsgiver=" + arbeidsgiver + ", aktivitetStatus=" + aktivitetStatus + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AktivitetOgArbeidsgiverNøkkel that)) {
            return false;
        }

        return Objects.equals(arbeidsgiver, that.arbeidsgiver) && Objects.equals(aktivitetStatus, that.aktivitetStatus);
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidsgiver, aktivitetStatus);
    }

    public boolean erArbeidstaker() {
        return aktivitetStatus.erArbeidstaker();
    }
}
