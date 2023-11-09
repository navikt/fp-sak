package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;
import no.nav.fpsak.tidsserie.LocalDateInterval;

/**
 * Input for vurdering av opptjeningsvilkår.
 * Består av sett av AktivitetPeriodeInput for ulike aktivitetet og eventuelt ulike arbeidsgivere.
 */
@RuleDocumentationGrunnlag
public record Opptjeningsgrunnlag(LocalDate behandlingsDato, /** Behandlingstidspunkt - normalt dagens dato. */
                                  @JsonProperty("forsteDatoOpptjening") LocalDate førsteDatoOpptjening, /** Første dato med opptjening som teller med. (fra-og-med) */
                                  LocalDate sisteDatoForOpptjening, /** Skjæringstidspunkt (siste punkt med opptjening). (til-og-med) */
                                  List<AktivitetPeriode> aktivitetPerioder, /** Input med aktiviteter som skal inngå i vurderingen. */
                                  List<InntektPeriode> inntektPerioder) implements VilkårGrunnlag {

    public LocalDateInterval getOpptjeningPeriode() {
        return new LocalDateInterval(førsteDatoOpptjening, sisteDatoForOpptjening);
    }
}
