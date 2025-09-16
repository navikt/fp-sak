package no.nav.foreldrepenger.domene.aksjonspunkt;


import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class BeregningsgrunnlagPrStatusOgAndelEndring {

    private final BeløpEndring beløpEndring;
    private final InntektskategoriEndring inntektskategoriEndring;
    private final AktivitetStatus aktivitetStatus;
    private final OpptjeningAktivitetType arbeidsforholdType;
    private final Arbeidsgiver arbeidsgiver;
    private final InternArbeidsforholdRef arbeidsforholdRef;
    private final RefusjonEndring refusjonEndring;

    public BeregningsgrunnlagPrStatusOgAndelEndring(BeløpEndring beløpEndring,
                                                    InntektskategoriEndring inntektskategoriEndring,
                                                    RefusjonEndring refusjonEndring,
                                                    AktivitetStatus aktivitetStatus,
                                                    OpptjeningAktivitetType arbeidsforholdType,
                                                    Arbeidsgiver arbeidsgiver,
                                                    InternArbeidsforholdRef arbeidsforholdRef) {
        this.beløpEndring = beløpEndring;
        this.inntektskategoriEndring = inntektskategoriEndring;
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsforholdType = arbeidsforholdType;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.refusjonEndring = refusjonEndring;
    }

    public Optional<BeløpEndring> getInntektEndring() {
        return Optional.ofNullable(beløpEndring);
    }

    public Optional<InntektskategoriEndring> getInntektskategoriEndring() {
        return Optional.ofNullable(inntektskategoriEndring);
    }

    public Optional<RefusjonEndring> getRefusjonEndring() {
        return Optional.ofNullable(refusjonEndring);
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public OpptjeningAktivitetType getArbeidsforholdType() {
        return arbeidsforholdType;
    }

    public Optional<Arbeidsgiver> getArbeidsgiver() {
        return Optional.ofNullable(arbeidsgiver);
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }
}
