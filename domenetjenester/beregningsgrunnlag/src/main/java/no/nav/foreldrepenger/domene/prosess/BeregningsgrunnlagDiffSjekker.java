package no.nav.foreldrepenger.domene.prosess;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.kodeverk.SammenligningsgrunnlagType;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.Sammenligningsgrunnlag;
import no.nav.foreldrepenger.domene.entiteter.SammenligningsgrunnlagPrStatus;

class BeregningsgrunnlagDiffSjekker {

    private BeregningsgrunnlagDiffSjekker() {
        // Skjul
    }

    static boolean harSignifikantDiffIAktiviteter(BeregningAktivitetAggregatEntitet aktivt,
                                                  BeregningAktivitetAggregatEntitet forrige) {
        if (aktivt.getBeregningAktiviteter().size() != forrige.getBeregningAktiviteter().size()) {
            return true;
        }
        if (!aktivt.getBeregningAktiviteter().containsAll(forrige.getBeregningAktiviteter())) {
            return true;
        }
        return !aktivt.getSkjæringstidspunktOpptjening().equals(forrige.getSkjæringstidspunktOpptjening());
    }

    static boolean harSignifikantDiffIBeregningsgrunnlag(BeregningsgrunnlagEntitet aktivt,
                                                         BeregningsgrunnlagEntitet forrige) {
        if (!erLike(aktivt.getGrunnbeløp() == null ? null : aktivt.getGrunnbeløp().getVerdi(),
            forrige.getGrunnbeløp() == null ? null : forrige.getGrunnbeløp().getVerdi())) {
            return true;
        }
        if (!hentStatuser(aktivt).equals(hentStatuser(forrige))) {
            return true;
        }
        if (!aktivt.getSkjæringstidspunkt().equals(forrige.getSkjæringstidspunkt())) {
            return true;
        }
        if (aktivt.getFaktaOmBeregningTilfeller().size() != forrige.getFaktaOmBeregningTilfeller().size()
            || !aktivt.getFaktaOmBeregningTilfeller().containsAll(forrige.getFaktaOmBeregningTilfeller())) {
            return true;
        }
        if (aktivt.getRegelInputBrukersStatus() != null && !aktivt.getRegelInputBrukersStatus()
            .equals(forrige.getRegelInputBrukersStatus())) {
            return true;
        }
        if (aktivt.getRegelInputSkjæringstidspunkt() != null && !aktivt.getRegelInputSkjæringstidspunkt()
            .equals(forrige.getRegelInputSkjæringstidspunkt())) {
            return true;
        }
        if (aktivt.getRegelinputPeriodisering() != null && !aktivt.getRegelinputPeriodisering()
            .equals(forrige.getRegelinputPeriodisering())) {
            return true;
        }
        if (harSammenligningsgrunnlagDiff(aktivt.getSammenligningsgrunnlag(), forrige.getSammenligningsgrunnlag())) {
            return true;
        }
        if (harSammenligningsgrunnlagPrStatusDiff(aktivt.getSammenligningsgrunnlagPrStatusListe(),
            forrige.getSammenligningsgrunnlagPrStatusListe())) {
            return true;
        }

        var aktivePerioder = aktivt.getBeregningsgrunnlagPerioder();
        var forrigePerioder = forrige.getBeregningsgrunnlagPerioder();
        return harPeriodeDiff(aktivePerioder, forrigePerioder);
    }

    private static boolean harSammenligningsgrunnlagDiff(Optional<Sammenligningsgrunnlag> aktivtOpt,
                                                         Optional<Sammenligningsgrunnlag> forrigeOpt) {
        if (aktivtOpt.isEmpty() || forrigeOpt.isEmpty()) {
            return aktivtOpt.isEmpty() != forrigeOpt.isEmpty();
        }
        var aktivt = aktivtOpt.get();
        var forrige = forrigeOpt.get();
        if (!erLike(aktivt.getAvvikPromille(), forrige.getAvvikPromille())) {
            return true;
        }
        if (!erLike(aktivt.getRapportertPrÅr(), forrige.getRapportertPrÅr())) {
            return true;
        }
        if (!Objects.equals(aktivt.getSammenligningsperiodeFom(), forrige.getSammenligningsperiodeFom())) {
            return true;
        }
        return !Objects.equals(aktivt.getSammenligningsperiodeTom(), forrige.getSammenligningsperiodeTom());
    }

    private static boolean harSammenligningsgrunnlagPrStatusDiff(List<SammenligningsgrunnlagPrStatus> aktivt,
                                                                 List<SammenligningsgrunnlagPrStatus> forrige) {
        if (aktivt.isEmpty() != forrige.isEmpty()) {
            return true;
        }
        if (!inneholderLikeSammenligningstyper(aktivt, forrige)) {
            return true;
        }
        for (var aktivSgPrStatus : aktivt) {
            var forrigeSgPrStatus = forrige.stream()
                .filter(s -> aktivSgPrStatus.getSammenligningsgrunnlagType().equals(s.getSammenligningsgrunnlagType()))
                .findFirst()
                .get();
            if (!erLike(aktivSgPrStatus.getAvvikPromille(), forrigeSgPrStatus.getAvvikPromille())) {
                return true;
            }
            if (!erLike(aktivSgPrStatus.getRapportertPrÅr(), forrigeSgPrStatus.getRapportertPrÅr())) {
                return true;
            }
            if (!Objects.equals(aktivSgPrStatus.getSammenligningsperiodeFom(),
                forrigeSgPrStatus.getSammenligningsperiodeFom())) {
                return true;
            }
            if (!Objects.equals(aktivSgPrStatus.getSammenligningsperiodeTom(),
                forrigeSgPrStatus.getSammenligningsperiodeTom())) {
                return true;
            }
        }
        return false;
    }

    private static boolean harPeriodeDiff(List<BeregningsgrunnlagPeriode> aktivePerioder,
                                          List<BeregningsgrunnlagPeriode> forrigePerioder) {
        if (aktivePerioder.size() != forrigePerioder.size()) {
            return true;
        }
        // begge listene er sorter på fom dato så det er mulig å benytte indeks her
        for (var i = 0; i < aktivePerioder.size(); i++) {
            var aktivPeriode = aktivePerioder.get(i);
            var forrigePeriode = forrigePerioder.get(i);
            if (!aktivPeriode.getBeregningsgrunnlagPeriodeFom()
                .equals(forrigePeriode.getBeregningsgrunnlagPeriodeFom())) {
                return true;
            }
            if (!erLike(aktivPeriode.getBruttoPrÅr(), forrigePeriode.getBruttoPrÅr())) {
                return true;
            }
            if (aktivPeriode.getRegelInputVilkårvurdering() != null && !aktivPeriode.getRegelInputVilkårvurdering()
                .equals(forrigePeriode.getRegelInputVilkårvurdering())) {
                return true;
            }
            if (aktivPeriode.getRegelInputForeslå() != null && !aktivPeriode.getRegelInputForeslå()
                .equals(forrigePeriode.getRegelInputForeslå())) {
                return true;
            }
            var aktiveAndeler = aktivPeriode.getBeregningsgrunnlagPrStatusOgAndelList();
            var forrigeAndeler = forrigePeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                .filter(a -> !a.erLagtTilAvSaksbehandler())
                .collect(Collectors.toList());
            if (aktiveAndeler.size() != forrigeAndeler.size()) {
                return true;
            }
            if (sjekkAndeler(aktiveAndeler, forrigeAndeler)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sjekkAndeler(List<BeregningsgrunnlagPrStatusOgAndel> aktiveAndeler,
                                        List<BeregningsgrunnlagPrStatusOgAndel> forrigeAndeler) {
        for (var aktivAndel : aktiveAndeler) {
            var forrigeAndelOpt = forrigeAndeler.stream()
                .filter(a -> a.getAndelsnr().equals(aktivAndel.getAndelsnr()))
                .findFirst();
            if (forrigeAndelOpt.isEmpty()) {
                return true;
            }
            var forrigeAndel = forrigeAndelOpt.get();
            if (harAndelDiff(aktivAndel, forrigeAndel)) {
                return true;
            }
        }
        return false;
    }

    private static boolean harAndelDiff(BeregningsgrunnlagPrStatusOgAndel aktivAndel,
                                        BeregningsgrunnlagPrStatusOgAndel forrigeAndel) {
        if (!aktivAndel.getAktivitetStatus().equals(forrigeAndel.getAktivitetStatus())) {
            return true;
        }
        if (hvisArbforManglerHosKunEn(aktivAndel, forrigeAndel)) {
            return true;
        }

        var aktivArbeidsforhold = aktivAndel.getBgAndelArbeidsforhold();
        var forrigeArbeidsforhold = forrigeAndel.getBgAndelArbeidsforhold();

        if (aktivArbeidsforhold.isPresent() && forrigeArbeidsforhold.isPresent()) {
            return aktivArbeidsforholdFørerTilDiff(aktivArbeidsforhold.get(), forrigeArbeidsforhold.get());
        }
        if (!aktivAndel.getGjeldendeInntektskategori().equals(forrigeAndel.getGjeldendeInntektskategori())) {
            return true;
        }
        if (!erLike(aktivAndel.getBruttoPrÅr(), forrigeAndel.getBruttoPrÅr())) {
            return true;
        }
        if (aktivAndel.getNyIArbeidslivet() != null && !aktivAndel.getNyIArbeidslivet()
            .equals(forrigeAndel.getNyIArbeidslivet())) {
            return true;
        }
        if (aktivAndel.erNyoppstartet().isPresent() && !forrigeAndel.erNyoppstartet()
            .map(nyoppstartet -> aktivAndel.erNyoppstartet().orElseThrow().equals(nyoppstartet))
            .orElse(false)) {
            return true;
        }
        return aktivAndel.mottarYtelse().isPresent() && !forrigeAndel.mottarYtelse()
            .map(mottarYtelse -> aktivAndel.mottarYtelse().orElseThrow().equals(mottarYtelse))
            .orElse(false);
    }

    private static boolean hvisArbforManglerHosKunEn(BeregningsgrunnlagPrStatusOgAndel aktivAndel,
                                                     BeregningsgrunnlagPrStatusOgAndel forrigeAndel) {
        return aktivAndel.getBgAndelArbeidsforhold().isPresent() != forrigeAndel.getBgAndelArbeidsforhold().isPresent();
    }

    private static boolean aktivArbeidsforholdFørerTilDiff(BGAndelArbeidsforhold aktivArbeidsforhold,
                                                           BGAndelArbeidsforhold forrigeArbeidsforhold) {
        if (!aktivArbeidsforhold.getArbeidsgiver().equals(forrigeArbeidsforhold.getArbeidsgiver())) {
            return true;
        }
        if (!erLike(aktivArbeidsforhold.getRefusjonskravPrÅr(), forrigeArbeidsforhold.getRefusjonskravPrÅr())) {
            return true;
        }
        if (!erLike(aktivArbeidsforhold.getSaksbehandletRefusjonPrÅr(),
            forrigeArbeidsforhold.getSaksbehandletRefusjonPrÅr())) {
            return true;
        }
        if (!erLike(aktivArbeidsforhold.getFordeltRefusjonPrÅr(), forrigeArbeidsforhold.getFordeltRefusjonPrÅr())) {
            return true;
        }
        if (aktivArbeidsforhold.erLønnsendringIBeregningsperioden() != null
            && !aktivArbeidsforhold.erLønnsendringIBeregningsperioden()
            .equals(forrigeArbeidsforhold.erLønnsendringIBeregningsperioden())) {
            return true;
        }
        if (!Objects.equals(aktivArbeidsforhold.getArbeidsperiodeFom(), forrigeArbeidsforhold.getArbeidsperiodeFom())) {
            return true;
        }
        if (!Objects.equals(aktivArbeidsforhold.getArbeidsperiodeTom(), forrigeArbeidsforhold.getArbeidsperiodeTom())) {
            return true;
        }
        return aktivArbeidsforhold.getErTidsbegrensetArbeidsforhold() != null
            && !aktivArbeidsforhold.getErTidsbegrensetArbeidsforhold()
            .equals(forrigeArbeidsforhold.getErTidsbegrensetArbeidsforhold());
    }

    private static boolean inneholderLikeSammenligningstyper(List<SammenligningsgrunnlagPrStatus> aktivt,
                                                             List<SammenligningsgrunnlagPrStatus> forrige) {
        var sammenligningsgrunnlagTyper = EnumSet.allOf(
            SammenligningsgrunnlagType.class);

        for (var sgType : sammenligningsgrunnlagTyper) {
            if (forrige.stream().anyMatch(s -> sgType.getKode().equals(s.getSammenligningsgrunnlagType().getKode()))
                != aktivt.stream()
                .anyMatch(s -> sgType.getKode().equals(s.getSammenligningsgrunnlagType().getKode()))) {
                return false;
            }
        }
        return true;
    }

    private static List<AktivitetStatus> hentStatuser(BeregningsgrunnlagEntitet aktivt) {
        return aktivt.getAktivitetStatuser()
            .stream()
            .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus)
            .collect(Collectors.toList());
    }

    private static boolean erLike(BigDecimal verdi1, BigDecimal verdi2) {
        return verdi1 == null && verdi2 == null || verdi1 != null && verdi2 != null && verdi1.compareTo(verdi2) == 0;
    }
}
