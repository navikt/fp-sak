package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import static no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.IAYMapperTilKalkulus.mapArbeidsforholdRef;
import static no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.IAYMapperTilKalkulus.mapArbeidsgiver;

import java.time.YearMonth;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.BesteberegningMånedGrunnlag;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.BesteberegningVurderingGrunnlag;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Inntekt;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagGUIInputFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BesteberegningInntektEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BesteberegningMånedsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BesteberegninggrunnlagEntitet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class BeregningsgrunnlagGUIInputTjeneste extends BeregningsgrunnlagGUIInputFelles {

    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private HentOgLagreBeregningsgrunnlagTjeneste hentOgLagreBeregningsgrunnlagTjeneste;

    protected BeregningsgrunnlagGUIInputTjeneste() {
        // CDI proxy
    }

    @Inject
    public BeregningsgrunnlagGUIInputTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                              InntektArbeidYtelseTjeneste iayTjeneste,
                                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                              AndelGraderingTjeneste andelGraderingTjeneste,
                                              BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste,
                                              InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                              HentOgLagreBeregningsgrunnlagTjeneste hentOgLagreBeregningsgrunnlagTjeneste) {
        super(behandlingRepositoryProvider.getBehandlingRepository(), iayTjeneste, skjæringstidspunktTjeneste, andelGraderingTjeneste,
                inntektsmeldingTjeneste);
        this.fagsakRelasjonRepository = Objects.requireNonNull(behandlingRepositoryProvider.getFagsakRelasjonRepository(),
                "fagsakRelasjonRepository");
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.hentOgLagreBeregningsgrunnlagTjeneste = hentOgLagreBeregningsgrunnlagTjeneste;
    }

    @Override
    public YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        var saksnummer = ref.getSaksnummer();
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonHvisEksisterer(saksnummer);
        var dekningsgrad = fagsakRelasjon.map(FagsakRelasjon::getGjeldendeDekningsgrad)
                .orElseThrow(() -> new IllegalStateException("Mangler FagsakRelasjon#dekningsgrad for behandling: " + ref));
        boolean kvalifisererTilBesteberegning = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref);
        var besteberegninggrunnlag = hentOgLagreBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(ref.getBehandlingId())
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .flatMap(BeregningsgrunnlagEntitet::getBesteberegninggrunnlag)
            .map(BeregningsgrunnlagGUIInputTjeneste::mapTilVurderinsgrunnlag);
        return besteberegninggrunnlag.map(bbGrunnlag -> new ForeldrepengerGrunnlag(dekningsgrad.getVerdi(), bbGrunnlag))
            .orElse(new ForeldrepengerGrunnlag(dekningsgrad.getVerdi(), kvalifisererTilBesteberegning));
    }

    private static BesteberegningVurderingGrunnlag mapTilVurderinsgrunnlag(BesteberegninggrunnlagEntitet besteberegninggrunnlagEntitet) {
        return new BesteberegningVurderingGrunnlag(besteberegninggrunnlagEntitet.getSeksBesteMåneder().stream()
            .map(BeregningsgrunnlagGUIInputTjeneste::mapTilMånedsgrunnlag).collect(Collectors.toList()));
    }

    private static BesteberegningMånedGrunnlag mapTilMånedsgrunnlag(BesteberegningMånedsgrunnlagEntitet månedsgrunnlagEntitet) {
        return new BesteberegningMånedGrunnlag(månedsgrunnlagEntitet.getInntekter().stream()
            .map(BeregningsgrunnlagGUIInputTjeneste::mapTilInntekt).collect(Collectors.toList()), YearMonth.from(månedsgrunnlagEntitet.getPeriode().getFomDato()));
    }

    private static Inntekt mapTilInntekt(BesteberegningInntektEntitet besteberegningInntektEntitet) {
        if (besteberegningInntektEntitet.getArbeidsgiver() != null) {
            return new Inntekt(mapArbeidsgiver(besteberegningInntektEntitet.getArbeidsgiver()),
                mapArbeidsforholdRef(besteberegningInntektEntitet.getArbeidsforholdRef()),
                besteberegningInntektEntitet.getInntekt());
        }
        return new Inntekt(OpptjeningAktivitetType.fraKode(besteberegningInntektEntitet.getOpptjeningAktivitetType().getKode()), besteberegningInntektEntitet.getInntekt());
    }
}
