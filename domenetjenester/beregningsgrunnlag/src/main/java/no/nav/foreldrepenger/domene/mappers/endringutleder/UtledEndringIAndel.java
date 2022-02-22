package no.nav.foreldrepenger.domene.mappers.endringutleder;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.domene.modell.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeløpEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.InntektskategoriEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.RefusjonEndring;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class UtledEndringIAndel {

    private UtledEndringIAndel() {
        // skjul
    }

    public static Optional<BeregningsgrunnlagPrStatusOgAndelEndring> utled(BeregningsgrunnlagPrStatusOgAndel andel,
                                                                           Optional<BeregningsgrunnlagPrStatusOgAndel> andelFraSteg,
                                                                           Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeAndel) {
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring = initialiserAndelEndring(andel);
        andelEndring.setBeløpEndring(lagInntektEndring(andel, forrigeAndel));
        andelEndring.setInntektskategoriEndring(utledInntektskategoriEndring(andel, andelFraSteg, forrigeAndel));
        andelEndring.setRefusjonEndring(utledRefusjonsendring(andel, forrigeAndel));
        if (harEndringIAndel(andelEndring)) {
            return Optional.of(andelEndring);
        }
        return Optional.empty();
    }


    private static BeregningsgrunnlagPrStatusOgAndelEndring initialiserAndelEndring(BeregningsgrunnlagPrStatusOgAndel andel) {
        BeregningsgrunnlagPrStatusOgAndelEndring andelEndring;
        if (andel.getAktivitetStatus().erArbeidstaker()) {
            if (andel.getArbeidsgiver().isPresent()) {
                var arbeidsgiver = andel.getArbeidsgiver().get();
                andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(andel.getAndelsnr(), arbeidsgiver,
                    andel.getArbeidsforholdRef().orElse(InternArbeidsforholdRef.nullRef()));
            } else {
                andelEndring = BeregningsgrunnlagPrStatusOgAndelEndring.opprettForArbeidstakerUtenArbeidsgiver(andel.getArbeidsforholdType(),
                    andel.getAndelsnr());
            }
        } else {
            andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(andel.getAndelsnr(),
                AktivitetStatus.fraKode(andel.getAktivitetStatus().getKode()));
        }
        return andelEndring;
    }

    private static boolean harEndringIAndel(BeregningsgrunnlagPrStatusOgAndelEndring a) {
        return a.getInntektEndring().isPresent() || a.getInntektskategoriEndring().isPresent();
    }

    private static BeløpEndring lagInntektEndring(BeregningsgrunnlagPrStatusOgAndel andel, Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeAndel) {
        return andel.getBruttoPrÅr() != null ? new BeløpEndring(forrigeAndel.map(BeregningsgrunnlagPrStatusOgAndel::getBruttoPrÅr).orElse(null),
            andel.getBruttoPrÅr()) : null;
    }

    private static InntektskategoriEndring utledInntektskategoriEndring(BeregningsgrunnlagPrStatusOgAndel andel,
                                                                        Optional<BeregningsgrunnlagPrStatusOgAndel> andelFraSteg,
                                                                        Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeAndel) {
        return harEndringIInntektskategori(andel, andelFraSteg, forrigeAndel) ? initInntektskategoriEndring(andel, forrigeAndel) : null;
    }

    private static InntektskategoriEndring initInntektskategoriEndring(BeregningsgrunnlagPrStatusOgAndel andel,
                                                                       Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeAndel) {
        return new InntektskategoriEndring(finnInntektskategori(forrigeAndel), Inntektskategori.fraKode(andel.getInntektskategori().getKode()));
    }

    private static Boolean harEndringIInntektskategori(BeregningsgrunnlagPrStatusOgAndel andel,
                                                       Optional<BeregningsgrunnlagPrStatusOgAndel> andelFraSteg,
                                                       Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeAndel) {
        if (forrigeAndel.isEmpty()) {
            return andelFraSteg.map(a -> !Objects.equals(a.getInntektskategori(), andel.getInntektskategori())).orElse(true);
        }
        return forrigeAndel.map(a -> !a.getInntektskategori().equals(andel.getInntektskategori())).orElse(true);
    }

    private static Inntektskategori finnInntektskategori(Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeAndel) {
        return forrigeAndel.map(BeregningsgrunnlagPrStatusOgAndel::getInntektskategori).orElse(null);
    }

    private static RefusjonEndring utledRefusjonsendring(BeregningsgrunnlagPrStatusOgAndel andel,
                                                         Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeAndel) {
        return harEndringIRefusjon(andel, forrigeAndel) ? initRefusjonEndring(andel, forrigeAndel) : null;
    }

    private static RefusjonEndring initRefusjonEndring(BeregningsgrunnlagPrStatusOgAndel andel,
                                                       Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeAndel) {
        return new RefusjonEndring(finnRefusjon(forrigeAndel), initRefusjon(andel));
    }

    private static BigDecimal initRefusjon(BeregningsgrunnlagPrStatusOgAndel andel) {
        return andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(BigDecimal.ZERO);
    }

    private static boolean harEndringIRefusjon(BeregningsgrunnlagPrStatusOgAndel andel, Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeAndel) {
        Optional<BigDecimal> forrigeRefusjonskrav = forrigeAndel.flatMap(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
            .map(BGAndelArbeidsforhold::getRefusjonskravPrÅr);
        Optional<BigDecimal> nyttRefusjonskrav = andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr);

        if (forrigeRefusjonskrav.isEmpty() || nyttRefusjonskrav.isEmpty()) {
            // Hvis en mangler må begge mangle, ellers er det endring i refusjon
            return !(forrigeRefusjonskrav.isEmpty() && nyttRefusjonskrav.isEmpty());
        }

        return forrigeRefusjonskrav.get().compareTo(nyttRefusjonskrav.get()) != 0;
    }

    private static BigDecimal finnRefusjon(Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeAndel) {
        return forrigeAndel.flatMap(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
            .map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
            .orElse(null);
    }

}
