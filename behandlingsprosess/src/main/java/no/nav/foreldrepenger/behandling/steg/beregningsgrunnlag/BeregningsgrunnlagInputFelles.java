package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.MapBehandlingRef;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.OpptjeningMapperTilKalkulus;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.RefusjonskravDato;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;


public abstract class BeregningsgrunnlagInputFelles {

    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private AndelGraderingTjeneste andelGraderingTjeneste;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;


    @Inject
    public BeregningsgrunnlagInputFelles(BehandlingRepository behandlingRepository,
                                         InntektArbeidYtelseTjeneste iayTjeneste,
                                         SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                         AndelGraderingTjeneste andelGraderingTjeneste,
                                         OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste, InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.skjæringstidspunktTjeneste = Objects.requireNonNull(skjæringstidspunktTjeneste, "skjæringstidspunktTjeneste");
        this.andelGraderingTjeneste = Objects.requireNonNull(andelGraderingTjeneste, "andelGrderingTjeneste");
        this.opptjeningForBeregningTjeneste = Objects.requireNonNull(opptjeningForBeregningTjeneste, "opptjeningForBeregningTjeneste");
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    protected BeregningsgrunnlagInputFelles() {
        // for CDI proxy
    }

    public BeregningsgrunnlagInput lagInput(Behandling behandling) {
        var behandlingId = behandling.getId();
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);

        return lagInput(ref, iayGrunnlag).orElseThrow();
    }

    public BeregningsgrunnlagInput lagInput(BehandlingReferanse referanse) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(referanse.getBehandlingId());
        return lagInput(referanse.medSkjæringstidspunkt(skjæringstidspunkt), iayGrunnlag).orElseThrow();
    }

    private Optional<BeregningsgrunnlagInput> lagInput(Behandling behandling, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return lagInput(ref, iayGrunnlag);
    }

    /** Returnerer input hvis data er på tilgjengelig for det, ellers Optional.empty(). */
    private Optional<BeregningsgrunnlagInput> lagInput(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var aktivitetGradering = andelGraderingTjeneste.utled(ref);
        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(ref, iayGrunnlag);
        if (opptjeningAktiviteter.isEmpty()) {
            return Optional.empty();
        }
        List<RefusjonskravDato> refusjonskravDatoer = inntektsmeldingTjeneste.hentAlleRefusjonskravDatoerForFagsak(ref.getSaksnummer());

        var ytelseGrunnlag = getYtelsespesifiktGrunnlag(ref);
        return Optional.of(new BeregningsgrunnlagInput(
            MapBehandlingRef.mapRef(ref),
            IAYMapperTilKalkulus.mapGrunnlag(iayGrunnlag),
            OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(opptjeningAktiviteter.orElseThrow()),
            aktivitetGradering,
            IAYMapperTilKalkulus.mapRefusjonskravDatoer(refusjonskravDatoer),
            ytelseGrunnlag));
    }

    /** Returnerer input hvis data er på tilgjengelig for det, ellers Optional.empty(). */
    public BeregningsgrunnlagInput lagInput(Long behandlingId) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return lagInput(behandling, iayGrunnlag).orElseThrow();
    }

    public abstract YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref);
}
