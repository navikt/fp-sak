package no.nav.foreldrepenger.behandling.revurdering.felles;

import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;

import java.time.LocalDate;
import java.util.*;

public class ErEndringIBeregning {
    private ErEndringIBeregning() {
    }

    public static boolean vurder(Optional<Beregningsgrunnlag> revurderingsGrunnlag, Optional<Beregningsgrunnlag> originaltGrunnlag) {
        if (revurderingsGrunnlag.isEmpty() && originaltGrunnlag.isEmpty()) {
            return false;
        }
        if (revurderingsGrunnlag.isEmpty() || originaltGrunnlag.isEmpty()) {
            return true;
        }

        var originalePerioder = originaltGrunnlag.get().getBeregningsgrunnlagPerioder();
        var revurderingsPerioder = revurderingsGrunnlag.get().getBeregningsgrunnlagPerioder();

        var allePeriodeDatoer = finnAllePeriodersStartdatoer(revurderingsPerioder, originalePerioder);

        for (var dato : allePeriodeDatoer) {
            var dagsatsRevurderingsgrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, revurderingsPerioder);
            var dagsatsOriginaltGrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, originalePerioder);
            if (!dagsatsRevurderingsgrunnlag.equals(dagsatsOriginaltGrunnlag)) {
                return true;
            }
        }
        return false;
    }

    public static boolean vurderUgunst(Optional<Beregningsgrunnlag> revurderingsGrunnlag,
            Optional<Beregningsgrunnlag> originaltGrunnlag) {
        if (revurderingsGrunnlag.isEmpty()) {
            return originaltGrunnlag.isPresent();
        }

        var originalePerioder = originaltGrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder)
                .orElse(Collections.emptyList());
        var revurderingsPerioder = revurderingsGrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder)
                .orElse(Collections.emptyList());

        var allePeriodeDatoer = finnAllePeriodersStartdatoer(revurderingsPerioder, originalePerioder);

        for (var dato : allePeriodeDatoer) {
            var dagsatsRevurderingsgrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, revurderingsPerioder);
            var dagsatsOriginaltGrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, originalePerioder);
            if (dagsatsOriginaltGrunnlag != null && (dagsatsRevurderingsgrunnlag == null || dagsatsRevurderingsgrunnlag < dagsatsOriginaltGrunnlag)) {
                return true;
            }
        }
        return false;
    }

    private static Set<LocalDate> finnAllePeriodersStartdatoer(List<BeregningsgrunnlagPeriode> revurderingsPerioder,
            List<BeregningsgrunnlagPeriode> originalePerioder) {
        Set<LocalDate> startDatoer = new HashSet<>();
        revurderingsPerioder.stream().map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom).forEach(startDatoer::add);
        originalePerioder.stream().map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom).forEach(startDatoer::add);
        return startDatoer;
    }

    private static Long finnGjeldendeDagsatsForDenneDatoen(LocalDate dato, List<BeregningsgrunnlagPeriode> perioder) {
        // Hvis dato er før starten på den første perioden bruker vi første periodes
        // dagsats
        var førsteKronologiskePeriode = perioder.stream()
                .min(Comparator.comparing(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom));
        if (førsteKronologiskePeriode.filter(periode -> dato.isBefore(periode.getBeregningsgrunnlagPeriodeFom())).isPresent()) {
            return førsteKronologiskePeriode.get().getDagsats();
        }
        for (var periode : perioder) {
            if (periode.getPeriode().inkluderer(dato)) {
                return periode.getDagsats();
            }
        }
        throw new IllegalStateException("Finner ikke dagsats for denne perioden");
    }

}
