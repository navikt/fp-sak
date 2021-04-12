package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;

public class ErEndringIBeregning {
    private ErEndringIBeregning() {
    }

    public static boolean vurder(Optional<BeregningsgrunnlagEntitet> revurderingsGrunnlag, Optional<BeregningsgrunnlagEntitet> originaltGrunnlag) {
        if (!revurderingsGrunnlag.isPresent() && !originaltGrunnlag.isPresent()) {
            return false;
        } else if (!revurderingsGrunnlag.isPresent() || !originaltGrunnlag.isPresent()) {
            return true;
        }

        List<BeregningsgrunnlagPeriode> originalePerioder = originaltGrunnlag.get().getBeregningsgrunnlagPerioder();
        List<BeregningsgrunnlagPeriode> revurderingsPerioder = revurderingsGrunnlag.get().getBeregningsgrunnlagPerioder();

        Set<LocalDate> allePeriodeDatoer = finnAllePeriodersStartdatoer(revurderingsPerioder, originalePerioder);

        for (LocalDate dato : allePeriodeDatoer) {
            Long dagsatsRevurderingsgrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, revurderingsPerioder);
            Long dagsatsOriginaltGrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, originalePerioder);
            if (!dagsatsRevurderingsgrunnlag.equals(dagsatsOriginaltGrunnlag)) {
                return true;
            }
        }
        return false;
    }

    public static boolean vurderUgunst(Optional<BeregningsgrunnlagEntitet> revurderingsGrunnlag,
                                       Optional<BeregningsgrunnlagEntitet> originaltGrunnlag,
                                       LocalDate sisteDagMedUttak) {
        if (revurderingsGrunnlag.isEmpty()) {
            return originaltGrunnlag.isPresent();
        }

        List<BeregningsgrunnlagPeriode> originalePerioder = originaltGrunnlag.map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder)
                .orElse(Collections.emptyList());
        List<BeregningsgrunnlagPeriode> revurderingsPerioder = revurderingsGrunnlag.map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder)
                .orElse(Collections.emptyList());

        // Sjekker kun ugunst i perioden frem til siste uttaksdato
        Set<LocalDate> allePeriodeDatoer = finnAllePeriodersStartdatoer(revurderingsPerioder, originalePerioder)
            .stream()
            .filter(dato -> !dato.isAfter(sisteDagMedUttak))
            .collect(Collectors.toSet());

        for (LocalDate dato : allePeriodeDatoer) {
            Long dagsatsRevurderingsgrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, revurderingsPerioder);
            Long dagsatsOriginaltGrunnlag = finnGjeldendeDagsatsForDenneDatoen(dato, originalePerioder);
            if ((dagsatsOriginaltGrunnlag != null)
                    && ((dagsatsRevurderingsgrunnlag == null) || (dagsatsRevurderingsgrunnlag < dagsatsOriginaltGrunnlag))) {
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
        Optional<BeregningsgrunnlagPeriode> førsteKronologiskePeriode = perioder.stream()
                .min(Comparator.comparing(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom));
        if (førsteKronologiskePeriode.filter(periode -> dato.isBefore(periode.getBeregningsgrunnlagPeriodeFom())).isPresent()) {
            return førsteKronologiskePeriode.get().getDagsats();
        }
        for (BeregningsgrunnlagPeriode periode : perioder) {
            if (periode.getPeriode().inkluderer(dato)) {
                return periode.getDagsats();
            }
        }
        throw new IllegalStateException("Finner ikke dagsats for denne perioden");
    }

}
