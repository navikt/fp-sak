package no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public record BeregningsgrunnlagPeriode(LocalDateInterval periode, List<BeregningsgrunnlagPrStatus> beregningsgrunnlagPrStatus) {

    private static final LocalDate MIN_DATO = LocalDate.of(2000, Month.JANUARY, 1);
    private static final LocalDate MAX_DATO = LocalDate.of(9999, Month.DECEMBER, 31);

    public BeregningsgrunnlagPeriode(LocalDate fom, LocalDate tom, List<BeregningsgrunnlagPrStatus> beregningsgrunnlagPrStatus) {
        this(new LocalDateInterval(fom != null ? fom : MIN_DATO, tom != null ? tom : MAX_DATO), beregningsgrunnlagPrStatus != null ? beregningsgrunnlagPrStatus : List.of());
    }

    public List<BeregningsgrunnlagPrStatus> getBeregningsgrunnlagPrStatus(AktivitetStatus aktivitetStatus) {
        return beregningsgrunnlagPrStatus().stream()
                .filter(af -> aktivitetStatus.equals(af.aktivitetStatus()))
                .toList();
    }

}
