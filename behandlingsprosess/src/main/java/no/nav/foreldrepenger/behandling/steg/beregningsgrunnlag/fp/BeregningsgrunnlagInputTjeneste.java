package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import no.nav.folketrygdloven.kalkulator.input.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.KalkulusKonfigInjecter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Objects;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class BeregningsgrunnlagInputTjeneste extends BeregningsgrunnlagInputFelles {

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private BeregningUttakTjeneste beregningUttakTjeneste;

    protected BeregningsgrunnlagInputTjeneste() {
        // CDI proxy
    }

    @Inject
    public BeregningsgrunnlagInputTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
            InntektArbeidYtelseTjeneste iayTjeneste,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
            BeregningUttakTjeneste beregningUttakTjeneste,
            OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
            BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste, InntektsmeldingTjeneste inntektsmeldingTjeneste,
            KalkulusKonfigInjecter kalkulusKonfigInjecter) {
        super(behandlingRepositoryProvider.getBehandlingRepository(), iayTjeneste, skjæringstidspunktTjeneste,
                opptjeningForBeregningTjeneste, inntektsmeldingTjeneste, kalkulusKonfigInjecter);
        this.fagsakRelasjonRepository = Objects.requireNonNull(behandlingRepositoryProvider.getFagsakRelasjonRepository(),
                "fagsakRelasjonRepository");
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.beregningUttakTjeneste = Objects.requireNonNull(beregningUttakTjeneste, "andelGrderingTjeneste");
    }

    @Override
    public YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        var aktivitetGradering = beregningUttakTjeneste.finnAktivitetGraderinger(ref);
        var saksnummer = ref.getSaksnummer();
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonHvisEksisterer(saksnummer);
        var dekningsgrad = fagsakRelasjon.map(FagsakRelasjon::getGjeldendeDekningsgrad)
                .orElseThrow(() -> new IllegalStateException("Mangler FagsakRelasjon#dekningsgrad for behandling: " + ref));
        var kvalifisererTilBesteberegning = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref);
        var fpGrunnlag = new ForeldrepengerGrunnlag(dekningsgrad.getVerdi(), kvalifisererTilBesteberegning, aktivitetGradering);
        beregningUttakTjeneste.finnSisteTilnærmedeUttaksdato(ref).ifPresent(fpGrunnlag::setSisteSøkteUttaksdag);
        if (besteberegningFødendeKvinneTjeneste.kvalifisererTilAutomatiskBesteberegning(ref)) {
            fpGrunnlag.setBesteberegningYtelsegrunnlag(besteberegningFødendeKvinneTjeneste.lagBesteberegningYtelseinput(ref));
        }
        return fpGrunnlag;
    }
}
