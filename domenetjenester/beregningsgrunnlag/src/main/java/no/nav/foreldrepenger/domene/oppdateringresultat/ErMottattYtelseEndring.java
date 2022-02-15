package no.nav.foreldrepenger.domene.oppdateringresultat;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;


public class ErMottattYtelseEndring {

    private AktivitetStatus aktivitetStatus;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private ToggleEndring erMottattYtelseEndring;

    public ErMottattYtelseEndring(AktivitetStatus aktivitetStatus,
                                  Arbeidsgiver arbeidsgiver,
                                  InternArbeidsforholdRef arbeidsforholdRef,
                                  ToggleEndring erMottattYtelseEndring) {
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.erMottattYtelseEndring = erMottattYtelseEndring;
    }


    public ErMottattYtelseEndring(AktivitetStatus aktivitetStatus,
                                  ToggleEndring erMottattYtelseEndring) {
        this.aktivitetStatus = aktivitetStatus;
        this.erMottattYtelseEndring = erMottattYtelseEndring;
    }

    public static ErMottattYtelseEndring lagErMottattYtelseEndringForFrilans(ToggleEndring toggleEndring) {
        return new ErMottattYtelseEndring(AktivitetStatus.FRILANSER, toggleEndring);
    }

    public static ErMottattYtelseEndring lagErMottattYtelseEndringForArbeid(ToggleEndring toggleEndring, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        return new ErMottattYtelseEndring(
            AktivitetStatus.ARBEIDSTAKER, arbeidsgiver, arbeidsforholdRef, toggleEndring);
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public ToggleEndring getErMottattYtelseEndring() {
        return erMottattYtelseEndring;
    }
}
