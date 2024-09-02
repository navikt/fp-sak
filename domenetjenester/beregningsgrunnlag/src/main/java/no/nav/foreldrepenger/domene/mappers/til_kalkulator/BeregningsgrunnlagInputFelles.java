package no.nav.foreldrepenger.domene.mappers.til_kalkulator;

import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.iay.KravperioderPrArbeidsforholdDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.KalkulusKonfigInjecter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public abstract class BeregningsgrunnlagInputFelles {

    protected BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private KalkulusKonfigInjecter kalkulusKonfigInjecter;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    @Inject
    public BeregningsgrunnlagInputFelles(BehandlingRepository behandlingRepository,
                                         InntektArbeidYtelseTjeneste iayTjeneste,
                                         SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                         OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                                         KalkulusKonfigInjecter kalkulusKonfigInjecter,
                                         InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.skjæringstidspunktTjeneste = Objects.requireNonNull(skjæringstidspunktTjeneste, "skjæringstidspunktTjeneste");
        this.opptjeningForBeregningTjeneste = Objects.requireNonNull(opptjeningForBeregningTjeneste, "opptjeningForBeregningTjeneste");
        this.kalkulusKonfigInjecter = kalkulusKonfigInjecter;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    protected BeregningsgrunnlagInputFelles() {
        // for CDI proxy
    }

    public abstract YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref, Skjæringstidspunkt stp);

    /** Returnerer input hvis data er på tilgjengelig for det, ellers Exception. */
    public BeregningsgrunnlagInput lagInput(BehandlingReferanse referanse) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.behandlingId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(referanse.behandlingId());
        return lagInput(referanse, skjæringstidspunkt, iayGrunnlag);
    }

    /** Returnerer input hvis data er på tilgjengelig for det, ellers Exception. */
    private BeregningsgrunnlagInput lagInput(BehandlingReferanse ref, Skjæringstidspunkt stp, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(ref, stp, iayGrunnlag);
        if (opptjeningAktiviteter.isEmpty()) {
            throw new IllegalStateException(String.format("No value present: Fant ikke forventet OpptjeningAktiviteter for behandling: %s med saksnummer: %s", ref.behandlingId(), ref.saksnummer()));
        }
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, stp.getUtledetSkjæringstidspunkt(), iayGrunnlag, true);
        var iayGrunnlagDto = IAYMapperTilKalkulus.mapGrunnlag(iayGrunnlag, inntektsmeldinger, ref.aktørId());

        var kravperioder = mapKravperioder(ref, stp, iayGrunnlag);
        var ytelseGrunnlag = getYtelsespesifiktGrunnlag(ref, stp);
        var beregningsgrunnlagInput = new BeregningsgrunnlagInput(
                MapBehandlingRef.mapRef(ref, stp),
                iayGrunnlagDto,
                OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(opptjeningAktiviteter.orElseThrow(), iayGrunnlag, ref, stp),
                kravperioder,
                ytelseGrunnlag);
        kalkulusKonfigInjecter.leggTilFeatureToggles(beregningsgrunnlagInput);
        return beregningsgrunnlagInput;
    }

    private List<KravperioderPrArbeidsforholdDto> mapKravperioder(BehandlingReferanse ref, Skjæringstidspunkt stp, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var alleInntektsmeldingerForFagsak = inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsak(ref.saksnummer());
        return KravperioderMapper.map(ref, stp, alleInntektsmeldingerForFagsak, iayGrunnlag);
    }
}
