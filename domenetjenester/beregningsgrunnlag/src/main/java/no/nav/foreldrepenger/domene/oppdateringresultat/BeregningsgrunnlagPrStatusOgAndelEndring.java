package no.nav.foreldrepenger.domene.oppdateringresultat;


import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class BeregningsgrunnlagPrStatusOgAndelEndring {

    private Long andelsnr;
    private BeløpEndring beløpEndring;
    private InntektskategoriEndring inntektskategoriEndring;
    private AktivitetStatus aktivitetStatus;
    private OpptjeningAktivitetType arbeidsforholdType;
    private Arbeidsgiver arbeidsgiver;
    private InternArbeidsforholdRef arbeidsforholdRef;
    private RefusjonEndring refusjonEndring;

    public BeregningsgrunnlagPrStatusOgAndelEndring(Long andelsnr, AktivitetStatus aktivitetStatus) {
        this.andelsnr = andelsnr;
        this.aktivitetStatus = aktivitetStatus;
    }

    public BeregningsgrunnlagPrStatusOgAndelEndring(Long andelsnr, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        this.andelsnr = andelsnr;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.aktivitetStatus = AktivitetStatus.ARBEIDSTAKER;
    }

    private BeregningsgrunnlagPrStatusOgAndelEndring(Long andelsnr, OpptjeningAktivitetType arbeidsforholdType) {
        this.andelsnr = andelsnr;
        this.arbeidsforholdType = arbeidsforholdType;
    }

    public BeregningsgrunnlagPrStatusOgAndelEndring(Long andelsnr,
                                                    BeløpEndring beløpEndring,
                                                    InntektskategoriEndring inntektskategoriEndring,
                                                    RefusjonEndring refusjonEndring,
                                                    AktivitetStatus aktivitetStatus,
                                                    OpptjeningAktivitetType arbeidsforholdType,
                                                    Arbeidsgiver arbeidsgiver,
                                                    InternArbeidsforholdRef arbeidsforholdRef) {
        this.andelsnr = andelsnr;
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

    public static BeregningsgrunnlagPrStatusOgAndelEndring opprettForArbeidstakerUtenArbeidsgiver(OpptjeningAktivitetType arbeidsforholdType,
                                                                                                  Long andelsnr) {
        return new BeregningsgrunnlagPrStatusOgAndelEndring(andelsnr, arbeidsforholdType);
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

    public Long getAndelsnr() {
        return andelsnr;
    }
}
