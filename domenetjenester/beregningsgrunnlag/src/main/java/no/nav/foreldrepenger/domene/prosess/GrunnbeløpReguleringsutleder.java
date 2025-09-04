package no.nav.foreldrepenger.domene.prosess;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Set;

import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;

public class GrunnbeløpReguleringsutleder {
    private static final Set<AktivitetStatus> SN_REGULERING = Set.of(
        AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, AktivitetStatus.KOMBINERT_AT_SN,
        AktivitetStatus.KOMBINERT_FL_SN, AktivitetStatus.KOMBINERT_AT_FL_SN);
    private static final Set<AktivitetStatus> YTELSER_REGULERING = Set.of(AktivitetStatus.ARBEIDSAVKLARINGSPENGER, AktivitetStatus.DAGPENGER);

    private GrunnbeløpReguleringsutleder() {
        // Skjuler default konstruktør
    }
    public static boolean kanPåvirkesAvGrunnbeløpRegulering(Beregningsgrunnlag bg) {
        return harGrunnlagSomBleAvkortet(bg)
            || erMilitærMedMinstekrav(bg)
            || erBeregnetSomNæringsdrivende(bg)
            || harYtelserSomReguleres(bg);
    }

    private static boolean erMilitærMedMinstekrav(Beregningsgrunnlag bg) {
        var erMs = bg.getAktivitetStatuser().stream().anyMatch(as -> AktivitetStatus.MILITÆR_ELLER_SIVIL.equals(as.getAktivitetStatus()));
        if (!erMs) {
            return false;
        }
        var minsteBruttoMilitørHarKravPå = bg.getGrunnbeløp().multipliser(3);
        var minsteBrutto = bg.getBeregningsgrunnlagPerioder().stream().map(BeregningsgrunnlagPeriode::getBruttoPrÅr).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        return minsteBrutto.compareTo(minsteBruttoMilitørHarKravPå.getVerdi()) <= 0;

    }

    private static boolean harGrunnlagSomBleAvkortet(Beregningsgrunnlag bg) {
        BigDecimal størsteBrutto = bg.getBeregningsgrunnlagPerioder().stream()
            .map(BeregningsgrunnlagPeriode::getBruttoPrÅr)
            .max(Comparator.naturalOrder())
            .orElse(BigDecimal.ZERO);
        BigDecimal antallGØvreGrenseverdi = BigDecimal.valueOf(6);
        BigDecimal grenseverdi = antallGØvreGrenseverdi.multiply(bg.getGrunnbeløp().getVerdi());
        return størsteBrutto.compareTo(grenseverdi) > 0;

    }

    private static boolean erBeregnetSomNæringsdrivende(Beregningsgrunnlag bg) {
        return bg.getAktivitetStatuser().stream()
            .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus)
            .anyMatch(SN_REGULERING::contains);
    }

    private static boolean harYtelserSomReguleres(Beregningsgrunnlag bg) {
        return bg.getAktivitetStatuser().stream()
            .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus)
            .anyMatch(YTELSER_REGULERING::contains);
    }
}
