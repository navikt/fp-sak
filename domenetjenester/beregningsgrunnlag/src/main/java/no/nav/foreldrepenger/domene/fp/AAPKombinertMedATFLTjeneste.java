package no.nav.foreldrepenger.domene.fp;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;

@ApplicationScoped
public class AAPKombinertMedATFLTjeneste {

    private static final Set<OpptjeningAktivitetType> ATFL_TYPER = Set.of(OpptjeningAktivitetType.ARBEID, OpptjeningAktivitetType.FRILANS);

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private BeregningTjeneste beregningTjeneste;

    protected AAPKombinertMedATFLTjeneste() {
        // CDI
    }

    @Inject
    public AAPKombinertMedATFLTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                       OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                                       BeregningTjeneste beregningTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    public boolean harAAPKombinertMedATFL(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        if (!FagsakYtelseType.FORELDREPENGER.equals(ref.fagsakYtelseType())) {
            return false;
        }
        var stpDato = stp.getSkjæringstidspunktHvisUtledet().orElse(null);
        if (stpDato == null) {
            return false;
        }
        if (erBeregningsgrunnlagOverstyrt(ref)) {
            return false;
        }
        var iay = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.behandlingId());
        var opptjening = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(ref, stp, iay);
        var perioder = opptjening.map(OpptjeningAktiviteter::getOpptjeningPerioder).orElse(Collections.emptyList());

        return harAAPPåSkjæringstidspunkt(perioder, stpDato) && harATFLSisteTreMåneder(perioder, stpDato);
    }

    private boolean erBeregningsgrunnlagOverstyrt(BehandlingReferanse ref) {
        return beregningTjeneste.hent(ref)
            .flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .map(Beregningsgrunnlag::isOverstyrt)
            .orElse(false);
    }

    private boolean harAAPPåSkjæringstidspunkt(java.util.List<OpptjeningAktiviteter.OpptjeningPeriode> perioder, LocalDate stp) {
        return perioder.stream()
            .filter(p -> OpptjeningAktivitetType.ARBEIDSAVKLARING.equals(p.opptjeningAktivitetType()))
            .anyMatch(p -> !p.periode().getFom().isAfter(stp) && (p.periode().getTom() == null || !p.periode().getTom().isBefore(stp)));
    }

    private boolean harATFLSisteTreMåneder(java.util.List<OpptjeningAktiviteter.OpptjeningPeriode> perioder, LocalDate stp) {
        var treMånederFørStp = stp.minusMonths(3);
        return perioder.stream()
            .filter(p -> ATFL_TYPER.contains(p.opptjeningAktivitetType()))
            .anyMatch(p -> !p.periode().getFom().isAfter(stp) && (p.periode().getTom() == null || !p.periode().getTom().isBefore(treMånederFørStp)));
    }
}
