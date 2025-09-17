package no.nav.foreldrepenger.domene.aksjonspunkt;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
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
