package no.nav.foreldrepenger.domene.aksjonspunkt;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;


public class ErTidsbegrensetArbeidsforholdEndring {

    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private ToggleEndring erTidsbegrensetArbeidsforholdEndring;

    public ErTidsbegrensetArbeidsforholdEndring(Arbeidsgiver arbeidsgiver,
                                                InternArbeidsforholdRef arbeidsforholdRef,
                                                ToggleEndring erTidsbegrensetArbeidsforholdEndring) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.erTidsbegrensetArbeidsforholdEndring = erTidsbegrensetArbeidsforholdEndring;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public ToggleEndring getErTidsbegrensetArbeidsforholdEndring() {
        return erTidsbegrensetArbeidsforholdEndring;
    }
}
