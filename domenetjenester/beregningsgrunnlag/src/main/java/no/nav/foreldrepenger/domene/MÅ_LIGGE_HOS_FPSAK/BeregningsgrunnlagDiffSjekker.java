package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.SammenligningsgrunnlagType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.SammenligningsgrunnlagPrStatus;
import no.nav.vedtak.util.Tuple;

class BeregningsgrunnlagDiffSjekker {

    private BeregningsgrunnlagDiffSjekker() {
        // Skjul
    }

    static boolean harSignifikantDiffIAktiviteter(BeregningAktivitetAggregatEntitet aktivt, BeregningAktivitetAggregatEntitet forrige) {
        if (aktivt.getBeregningAktiviteter().size() != forrige.getBeregningAktiviteter().size()) {
            return true;
        }
        if (!aktivt.getBeregningAktiviteter().containsAll(forrige.getBeregningAktiviteter())) {
            return true;
        }
        if (!aktivt.getSkjæringstidspunktOpptjening().equals(forrige.getSkjæringstidspunktOpptjening())) {
            return true;
        }
        return false;
    }

    static boolean harSignifikantDiffIBeregningsgrunnlag(BeregningsgrunnlagEntitet aktivt, BeregningsgrunnlagEntitet forrige) {
        if (!erLike(aktivt.getGrunnbeløp() == null ? null : aktivt.getGrunnbeløp().getVerdi(), forrige.getGrunnbeløp() == null ? null : forrige.getGrunnbeløp().getVerdi())) {
            return true;
        }
        if (!hentStatuser(aktivt).equals(hentStatuser(forrige))) {
            return true;
        }
        if (!aktivt.getSkjæringstidspunkt().equals(forrige.getSkjæringstidspunkt())) {
            return true;
        }
        if (aktivt.getFaktaOmBeregningTilfeller().size() != forrige.getFaktaOmBeregningTilfeller().size() || !aktivt.getFaktaOmBeregningTilfeller().containsAll(forrige.getFaktaOmBeregningTilfeller())) {
            return true;
        }
        if (aktivt.getRegelInputBrukersStatus() != null && !aktivt.getRegelInputBrukersStatus().equals(forrige.getRegelInputBrukersStatus())) {
            return true;
        }
        if (aktivt.getRegelInputSkjæringstidspunkt() != null && !aktivt.getRegelInputSkjæringstidspunkt().equals(forrige.getRegelInputSkjæringstidspunkt())) {
            return true;
        }
        if (aktivt.getRegelinputPeriodisering() != null && !aktivt.getRegelinputPeriodisering().equals(forrige.getRegelinputPeriodisering())) {
            return true;
        }
        if (harSammenligningsgrunnlagDiff(aktivt.getSammenligningsgrunnlag(), forrige.getSammenligningsgrunnlag())) {
            return true;
        }
        if (harSammenligningsgrunnlagPrStatusDiff(aktivt.getSammenligningsgrunnlagPrStatusListe(), forrige.getSammenligningsgrunnlagPrStatusListe())) {
            return true;
        }

        List<BeregningsgrunnlagPeriode> aktivePerioder = aktivt.getBeregningsgrunnlagPerioder();
        List<BeregningsgrunnlagPeriode> forrigePerioder = forrige.getBeregningsgrunnlagPerioder();
        return harPeriodeDiff(aktivePerioder, forrigePerioder);
    }

    private static boolean harSammenligningsgrunnlagDiff(Sammenligningsgrunnlag aktivt, Sammenligningsgrunnlag forrige) {
        if (aktivt == null || forrige == null) {
            return !Objects.equals(aktivt, forrige);
        }
        if (!erLike(aktivt.getAvvikPromille(), forrige.getAvvikPromille())) {
            return true;
        }
        if (!erLike(aktivt.getRapportertPrÅr(), forrige.getRapportertPrÅr())) {
            return true;
        }
        if (!Objects.equals(aktivt.getSammenligningsperiodeFom(), forrige.getSammenligningsperiodeFom())) {
            return true;
        }
        if (!Objects.equals(aktivt.getSammenligningsperiodeTom(), forrige.getSammenligningsperiodeTom())) {
            return true;
        }
        return false;
    }

    private static boolean harSammenligningsgrunnlagPrStatusDiff(List<SammenligningsgrunnlagPrStatus> aktivt, List<SammenligningsgrunnlagPrStatus> forrige) {
        if(aktivt.isEmpty() != forrige.isEmpty()){
            return true;
        }
        if(!inneholderLikeSammenligningstyper(aktivt, forrige)){
            return true;
        }
        for(SammenligningsgrunnlagPrStatus aktivSgPrStatus : aktivt){
            SammenligningsgrunnlagPrStatus forrigeSgPrStatus = forrige.stream().filter(s -> aktivSgPrStatus.getSammenligningsgrunnlagType().equals(s.getSammenligningsgrunnlagType())).findFirst().get();
            if(!erLike(aktivSgPrStatus.getAvvikPromille(), forrigeSgPrStatus.getAvvikPromille())){
                return true;
            }
            if (!erLike(aktivSgPrStatus.getRapportertPrÅr(), forrigeSgPrStatus.getRapportertPrÅr())) {
                return true;
            }
            if (!Objects.equals(aktivSgPrStatus.getSammenligningsperiodeFom(), forrigeSgPrStatus.getSammenligningsperiodeFom())) {
                return true;
            }
            if (!Objects.equals(aktivSgPrStatus.getSammenligningsperiodeTom(), forrigeSgPrStatus.getSammenligningsperiodeTom())) {
                return true;
            }
        }
        return false;
    }

    private static boolean harPeriodeDiff(List<BeregningsgrunnlagPeriode> aktivePerioder, List<BeregningsgrunnlagPeriode> forrigePerioder) {
        if (aktivePerioder.size() != forrigePerioder.size()) {
            return true;
        }
        // begge listene er sorter på fom dato så det er mulig å benytte indeks her
        for (int i = 0; i < aktivePerioder.size(); i++) {
            BeregningsgrunnlagPeriode aktivPeriode = aktivePerioder.get(i);
            BeregningsgrunnlagPeriode forrigePeriode = forrigePerioder.get(i);
            if (!aktivPeriode.getBeregningsgrunnlagPeriodeFom().equals(forrigePeriode.getBeregningsgrunnlagPeriodeFom())) {
                return true;
            }
            if (!erLike(aktivPeriode.getBruttoPrÅr(), forrigePeriode.getBruttoPrÅr())) {
                return true;
            }
            if (aktivPeriode.getRegelInputVilkårvurdering() != null && !aktivPeriode.getRegelInputVilkårvurdering().equals(forrigePeriode.getRegelInputVilkårvurdering())) {
                return true;
            }
            if (aktivPeriode.getRegelInputForeslå() != null && !aktivPeriode.getRegelInputForeslå().equals(forrigePeriode.getRegelInputForeslå())) {
                return true;
            }
            Tuple<List<BeregningsgrunnlagPrStatusOgAndel>, List<BeregningsgrunnlagPrStatusOgAndel>> resultat = finnAndeler(aktivPeriode, forrigePeriode);
            if (resultat.getElement1().size() != resultat.getElement2().size()) {
                return true;
            }
            if (sjekkAndeler(resultat.getElement1(), resultat.getElement2())) {
                return true;
            }
        }
        return false;
    }

    private static boolean sjekkAndeler(List<BeregningsgrunnlagPrStatusOgAndel> aktiveAndeler, List<BeregningsgrunnlagPrStatusOgAndel> forrigeAndeler) {
        for (BeregningsgrunnlagPrStatusOgAndel aktivAndel : aktiveAndeler) {
            Optional<BeregningsgrunnlagPrStatusOgAndel> forrigeAndelOpt = forrigeAndeler
                .stream().filter(a -> a.getAndelsnr().equals(aktivAndel.getAndelsnr()))
                .findFirst();
            if (forrigeAndelOpt.isEmpty()) {
                return true;
            }
            BeregningsgrunnlagPrStatusOgAndel forrigeAndel = forrigeAndelOpt.get();
            if (harAndelDiff(aktivAndel, forrigeAndel)) {
                return true;
            }
        }
        return false;
    }

    private static boolean harAndelDiff(BeregningsgrunnlagPrStatusOgAndel aktivAndel, BeregningsgrunnlagPrStatusOgAndel forrigeAndel) {
        if (!aktivAndel.getAktivitetStatus().equals(forrigeAndel.getAktivitetStatus())) {
            return true;
        }
        if (hvisArbforManglerHosKunEn(aktivAndel, forrigeAndel)) {
            return true;
        }

        Optional<BGAndelArbeidsforhold> aktivArbeidsforhold = aktivAndel.getBgAndelArbeidsforhold();
        Optional<BGAndelArbeidsforhold> forrigeArbeidsforhold = forrigeAndel.getBgAndelArbeidsforhold();

        if (aktivArbeidsforhold.isPresent() && forrigeArbeidsforhold.isPresent()) {
            return aktivArbeidsforholdFørerTilDiff(aktivArbeidsforhold.get(), forrigeArbeidsforhold.get());
        }
        if (!aktivAndel.getInntektskategori().equals(forrigeAndel.getInntektskategori())) {
            return true;
        }
        if (!erLike(aktivAndel.getBruttoPrÅr(), forrigeAndel.getBruttoPrÅr())) {
            return true;
        }
        if (aktivAndel.getNyIArbeidslivet() != null && !aktivAndel.getNyIArbeidslivet().equals(forrigeAndel.getNyIArbeidslivet())) {
            return true;
        }
        if (aktivAndel.erNyoppstartet().isPresent() && !forrigeAndel.erNyoppstartet().map(nyoppstartet -> aktivAndel.erNyoppstartet().get().equals(nyoppstartet)).orElse(false)) {
            return true;
        }
        if (aktivAndel.mottarYtelse().isPresent() && !forrigeAndel.mottarYtelse().map(mottarYtelse -> aktivAndel.mottarYtelse().get().equals(mottarYtelse)).orElse(false)) {
            return true;
        }
        return false;
    }

    private static boolean hvisArbforManglerHosKunEn(BeregningsgrunnlagPrStatusOgAndel aktivAndel, BeregningsgrunnlagPrStatusOgAndel forrigeAndel) {
        return aktivAndel.getBgAndelArbeidsforhold().isPresent() != forrigeAndel.getBgAndelArbeidsforhold().isPresent();
    }

    private static boolean aktivArbeidsforholdFørerTilDiff(BGAndelArbeidsforhold aktivArbeidsforhold, BGAndelArbeidsforhold forrigeArbeidsforhold) {
        if (!aktivArbeidsforhold.getArbeidsgiver().equals(forrigeArbeidsforhold.getArbeidsgiver())) {
            return true;
        }
        if (!erLike(aktivArbeidsforhold.getRefusjonskravPrÅr(), forrigeArbeidsforhold.getRefusjonskravPrÅr())) {
            return true;
        }
        if (!erLike(aktivArbeidsforhold.getSaksbehandletRefusjonPrÅr(), forrigeArbeidsforhold.getSaksbehandletRefusjonPrÅr())) {
            return true;
        }
        if (!erLike(aktivArbeidsforhold.getFordeltRefusjonPrÅr(), forrigeArbeidsforhold.getFordeltRefusjonPrÅr())) {
            return true;
        }
        if (aktivArbeidsforhold.erLønnsendringIBeregningsperioden() != null && !aktivArbeidsforhold.erLønnsendringIBeregningsperioden().equals(forrigeArbeidsforhold.erLønnsendringIBeregningsperioden())) {
            return true;
        }
        if (aktivArbeidsforhold.getErTidsbegrensetArbeidsforhold() != null && !aktivArbeidsforhold.getErTidsbegrensetArbeidsforhold().equals(forrigeArbeidsforhold.getErTidsbegrensetArbeidsforhold())) {
            return true;
        }
        return false;
    }

    private static boolean inneholderLikeSammenligningstyper(List<SammenligningsgrunnlagPrStatus> aktivt, List<SammenligningsgrunnlagPrStatus> forrige){
        EnumSet<SammenligningsgrunnlagType> sammenligningsgrunnlagTyper = EnumSet.allOf(SammenligningsgrunnlagType.class);

        for(SammenligningsgrunnlagType sgType : sammenligningsgrunnlagTyper){
            if(forrige.stream().anyMatch(s -> sgType.getKode().equals(s.getSammenligningsgrunnlagType().getKode())) !=
                    aktivt.stream().anyMatch(s -> sgType.getKode().equals(s.getSammenligningsgrunnlagType().getKode()))){
                return false;
            }
        }
        return true;
    }

    private static List<AktivitetStatus> hentStatuser(BeregningsgrunnlagEntitet aktivt) {
        return aktivt.getAktivitetStatuser().stream().map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus).collect(Collectors.toList());
    }

    private static Tuple<List<BeregningsgrunnlagPrStatusOgAndel>, List<BeregningsgrunnlagPrStatusOgAndel>> finnAndeler(BeregningsgrunnlagPeriode aktivPeriode, BeregningsgrunnlagPeriode forrigePeriode) {
        List<BeregningsgrunnlagPrStatusOgAndel> aktiveAndeler = aktivPeriode.getBeregningsgrunnlagPrStatusOgAndelList();
        List<BeregningsgrunnlagPrStatusOgAndel> forrigeAndeler = forrigePeriode
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(a -> !a.getLagtTilAvSaksbehandler())
            .collect(Collectors.toList());
        return new Tuple<>(aktiveAndeler, forrigeAndeler);
    }

    private static boolean erLike(BigDecimal verdi1, BigDecimal verdi2) {
        return verdi1 == null && verdi2 == null || verdi1 != null && verdi2 != null && verdi1.compareTo(verdi2) == 0;
    }
}
