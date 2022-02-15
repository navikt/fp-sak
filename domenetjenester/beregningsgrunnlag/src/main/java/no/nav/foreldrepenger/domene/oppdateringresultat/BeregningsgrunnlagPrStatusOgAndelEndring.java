package no.nav.foreldrepenger.domene.oppdateringresultat;


import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class BeregningsgrunnlagPrStatusOgAndelEndring {

    private BeløpEndring beløpEndring;
    private InntektskategoriEndring inntektskategoriEndring;
    private AktivitetStatus aktivitetStatus;
    private OpptjeningAktivitetType arbeidsforholdType;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private RefusjonEndring refusjonEndring;

    public BeregningsgrunnlagPrStatusOgAndelEndring(AktivitetStatus aktivitetStatus) {
        this.aktivitetStatus = aktivitetStatus;
    }

    public BeregningsgrunnlagPrStatusOgAndelEndring(Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    private BeregningsgrunnlagPrStatusOgAndelEndring(OpptjeningAktivitetType arbeidsforholdType) {
        this.arbeidsforholdType = arbeidsforholdType;
    }

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

    public void setBeløpEndring(BeløpEndring beløpEndring) {
        this.beløpEndring = beløpEndring;
    }

    public void setInntektskategoriEndring(InntektskategoriEndring inntektskategoriEndring) {
        this.inntektskategoriEndring = inntektskategoriEndring;
    }

    public void setAktivitetStatus(AktivitetStatus aktivitetStatus) {
        this.aktivitetStatus = aktivitetStatus;
    }

    public void setArbeidsforholdType(OpptjeningAktivitetType arbeidsforholdType) {
        this.arbeidsforholdType = arbeidsforholdType;
    }

    public void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    public void setArbeidsforholdRef(InternArbeidsforholdRef arbeidsforholdRef) {
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    public void setRefusjonEndring(RefusjonEndring refusjonEndring) {
        this.refusjonEndring = refusjonEndring;
    }

    public static BeregningsgrunnlagPrStatusOgAndelEndring opprettForArbeidstakerUtenArbeidsgiver(OpptjeningAktivitetType arbeidsforholdType) {
        return new BeregningsgrunnlagPrStatusOgAndelEndring(arbeidsforholdType);
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
