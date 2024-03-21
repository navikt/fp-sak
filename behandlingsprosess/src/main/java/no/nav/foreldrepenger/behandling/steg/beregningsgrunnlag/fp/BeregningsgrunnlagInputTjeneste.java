package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.folketrygdloven.kalkulus.kodeverk.Dekningsgrad;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.KalkulusKonfigInjecter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class BeregningsgrunnlagInputTjeneste extends BeregningsgrunnlagInputFelles {

    private DekningsgradTjeneste dekningsgradTjeneste;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private BeregningUttakTjeneste beregningUttakTjeneste;

    protected BeregningsgrunnlagInputTjeneste() {
        // CDI proxy
    }

    @Inject
    public BeregningsgrunnlagInputTjeneste(InntektArbeidYtelseTjeneste iayTjeneste,
                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                           BeregningUttakTjeneste beregningUttakTjeneste,
                                           OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                                           BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste,
                                           InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                           KalkulusKonfigInjecter kalkulusKonfigInjecter,
                                           DekningsgradTjeneste dekningsgradTjeneste,
                                           BehandlingRepository behandlingRepository) {
        super(behandlingRepository, iayTjeneste, skjæringstidspunktTjeneste, opptjeningForBeregningTjeneste,
            kalkulusKonfigInjecter, inntektsmeldingTjeneste);
        this.dekningsgradTjeneste = Objects.requireNonNull(dekningsgradTjeneste,
                "fagsakRelasjonTjeneste");
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.beregningUttakTjeneste = Objects.requireNonNull(beregningUttakTjeneste, "andelGrderingTjeneste");
    }

    @Override
    public YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        var aktivitetGradering = beregningUttakTjeneste.finnAktivitetGraderinger(ref);
        var dekningsgrad = dekningsgradTjeneste.finnGjeldendeDekningsgrad(ref);
        var kvalifisererTilBesteberegning = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref);
        var fpGrunnlag = new ForeldrepengerGrunnlag(mapTilDekningsgradKalkulator(dekningsgrad.getVerdi()), kvalifisererTilBesteberegning, aktivitetGradering);
        beregningUttakTjeneste.finnSisteTilnærmedeUttaksdato(ref).ifPresent(fpGrunnlag::setSisteSøkteUttaksdag);
        if (besteberegningFødendeKvinneTjeneste.kvalifisererTilAutomatiskBesteberegning(ref)) {
            fpGrunnlag.setBesteberegningYtelsegrunnlag(besteberegningFødendeKvinneTjeneste.lagBesteberegningYtelseinput(ref));
        }
        fpGrunnlag.setBehandlingstidspunkt(behandlingRepository.hentBehandling(ref.behandlingId()).getOpprettetDato().toLocalDate());
        return fpGrunnlag;
    }

    private Dekningsgrad mapTilDekningsgradKalkulator(int verdi) {
        // Kan ikke bruke switch siden fpsak ikke representerer som enum
        if (verdi == 80) {
            return Dekningsgrad.DEKNINGSGRAD_80;
        }
        if (verdi == 100) {
            return Dekningsgrad.DEKNINGSGRAD_100;
        }
        throw new IllegalStateException("Ugyldig dekningsgrad for foreldrepenger " + verdi);
    }

}
