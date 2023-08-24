package no.nav.foreldrepenger.ytelse.beregning.adapter;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;

import java.util.Optional;

public final class ArbeidsforholdMapper {

    private ArbeidsforholdMapper() {}

    public static Arbeidsforhold mapArbeidsforholdFraUttakAktivitet(Optional<Arbeidsgiver> arbeidsgiver,
                                                                    InternArbeidsforholdRef arbeidsforholdRef,
                                                                    UttakArbeidType uttakArbeidType) {
        if (UttakArbeidType.FRILANS.equals(uttakArbeidType)) {
            return Arbeidsforhold.frilansArbeidsforhold();
        }
        return lagArbeidsforhold(arbeidsgiver, arbeidsforholdRef);
    }

    static Arbeidsforhold mapArbeidsforholdFraBeregningsgrunnlag(BeregningsgrunnlagPrStatusOgAndel andel) {
        if (AktivitetStatus.FRILANSER.equals(AktivitetStatus.fraKode(andel.getAktivitetStatus().getKode()))) {
            return Arbeidsforhold.frilansArbeidsforhold();
        }
        var arbeidsgiver = andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver);
        var arbeidsforholdRef = andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef);
        return lagArbeidsforhold(arbeidsgiver, arbeidsforholdRef.orElse(InternArbeidsforholdRef.nullRef()));
    }

    private static Arbeidsforhold lagArbeidsforhold(Optional<Arbeidsgiver> arbeidsgiverOpt, InternArbeidsforholdRef arbeidsforholdRef) {
        if (arbeidsgiverOpt.isPresent()) {
            var arbeidsgiver = arbeidsgiverOpt.get();
            if (arbeidsgiver.getErVirksomhet()) {
                return lagArbeidsforholdHosVirksomhet(arbeidsgiver, arbeidsforholdRef);
            }
            if (arbeidsgiver.erAktørId()) {
                return lagArbeidsforholdHosPrivatperson(arbeidsgiver, arbeidsforholdRef);
            }
            throw new IllegalStateException("Utviklerfeil: Arbeidsgiver er verken virksomhet eller aktørId");
        }
        return null;
    }

    private static Arbeidsforhold lagArbeidsforholdHosVirksomhet(Arbeidsgiver arbgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        return Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(arbgiver.getIdentifikator(), arbeidsforholdRef.getReferanse());
    }

    private static Arbeidsforhold lagArbeidsforholdHosPrivatperson(Arbeidsgiver arbgiver, InternArbeidsforholdRef arbeidsforholdRef) {
        return Arbeidsforhold.nyttArbeidsforholdHosPrivatperson(arbgiver.getIdentifikator(), arbeidsforholdRef.getReferanse());
    }

}
