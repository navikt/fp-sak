package no.nav.foreldrepenger.ytelse.beregning.adapter;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

public final class ArbeidsforholdMapper {

    private ArbeidsforholdMapper() {}

    public static Arbeidsforhold mapArbeidsforholdFraUttakAktivitet(Optional<Arbeidsgiver> arbeidsgiver, Optional<InternArbeidsforholdRef> arbeidsforholdRef, UttakArbeidType uttakArbeidType) {
        if (UttakArbeidType.FRILANS.equals(uttakArbeidType)) {
            return Arbeidsforhold.frilansArbeidsforhold();
        }
        return lagArbeidsforhold(arbeidsgiver, arbeidsforholdRef);
    }

    static Arbeidsforhold mapArbeidsforholdFraBeregningsgrunnlag(BeregningsgrunnlagPrStatusOgAndel andel) {
        if (AktivitetStatus.FRILANSER.equals(AktivitetStatusMapper.fraBGTilVL(andel.getAktivitetStatus()))) {
            return Arbeidsforhold.frilansArbeidsforhold();
        }
        Optional<Arbeidsgiver> arbeidsgiver = andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver);
        Optional<InternArbeidsforholdRef> arbeidsforholdRef = andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef);
        return lagArbeidsforhold(arbeidsgiver, arbeidsforholdRef);
    }

    private static Arbeidsforhold lagArbeidsforhold(Optional<Arbeidsgiver> arbeidsgiverOpt, Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        if (arbeidsgiverOpt.isPresent()) {
            Arbeidsgiver arbeidsgiver = arbeidsgiverOpt.get();
            if (arbeidsgiver.getErVirksomhet()) {
                return lagArbeidsforholdHosVirksomhet(arbeidsgiver, arbeidsforholdRef);
            }
            if (arbeidsgiver.erAktørId()) {
                return lagArbeidsforholdHosPrivatperson(arbeidsgiver, arbeidsforholdRef);
            } else {
                throw new IllegalStateException("Utviklerfeil: Arbeidsgiver er verken virksomhet eller aktørId");
            }
        }
        return null;
    }

    private static Arbeidsforhold lagArbeidsforholdHosVirksomhet(Arbeidsgiver arbgiver, Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        return arbeidsforholdRef.isPresent()
            ? Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbgiver.getIdentifikator(), arbeidsforholdRef.get().getReferanse())
            : Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbgiver.getIdentifikator());
    }

    private static Arbeidsforhold lagArbeidsforholdHosPrivatperson(Arbeidsgiver arbgiver, Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        return arbeidsforholdRef.isPresent()
            ? Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbgiver.getIdentifikator(), arbeidsforholdRef.get().getReferanse())
            : Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbgiver.getIdentifikator());
    }

}
