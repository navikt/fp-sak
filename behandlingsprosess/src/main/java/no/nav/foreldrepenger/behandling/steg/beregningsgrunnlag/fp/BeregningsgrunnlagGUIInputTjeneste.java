package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import static no.nav.foreldrepenger.domene.mappers.til_kalkulator.IAYMapperTilKalkulus.mapArbeidsforholdRef;
import static no.nav.foreldrepenger.domene.mappers.til_kalkulator.IAYMapperTilKalkulus.mapArbeidsgiver;

import java.time.YearMonth;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.typer.Beløp;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.BesteberegningMånedGrunnlag;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.BesteberegningVurderingGrunnlag;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Inntekt;
import no.nav.folketrygdloven.kalkulus.kodeverk.Dekningsgrad;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BesteberegningInntektEntitet;
import no.nav.foreldrepenger.domene.entiteter.BesteberegningMånedsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BesteberegninggrunnlagEntitet;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagGUIInputFelles;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class BeregningsgrunnlagGUIInputTjeneste extends BeregningsgrunnlagGUIInputFelles {

    private DekningsgradTjeneste dekningsgradTjeneste;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private HentOgLagreBeregningsgrunnlagTjeneste hentOgLagreBeregningsgrunnlagTjeneste;
    private BeregningUttakTjeneste beregningUttakTjeneste;

    protected BeregningsgrunnlagGUIInputTjeneste() {
        // CDI proxy
    }

    @Inject
    public BeregningsgrunnlagGUIInputTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                              BeregningUttakTjeneste beregningUttakTjeneste,
                                              BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste,
                                              InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                              HentOgLagreBeregningsgrunnlagTjeneste hentOgLagreBeregningsgrunnlagTjeneste,
                                              DekningsgradTjeneste dekningsgradTjeneste) {
        super(behandlingRepositoryProvider.getBehandlingRepository(), skjæringstidspunktTjeneste, inntektsmeldingTjeneste);
        this.dekningsgradTjeneste = Objects.requireNonNull(dekningsgradTjeneste, "fagsakRelasjonTjeneste");
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.hentOgLagreBeregningsgrunnlagTjeneste = hentOgLagreBeregningsgrunnlagTjeneste;
        this.beregningUttakTjeneste = Objects.requireNonNull(beregningUttakTjeneste, "andelGrderingTjeneste");
    }

    @Override
    public YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var aktivitetGradering = beregningUttakTjeneste.finnAktivitetGraderingerKalkulus(ref);
        var dekningsgrad = dekningsgradTjeneste.finnGjeldendeDekningsgrad(ref);
        var kvalifisererTilBesteberegning = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref, stp);
        var besteberegninggrunnlag = hentOgLagreBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagGrunnlagEntitet(ref.behandlingId())
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .flatMap(BeregningsgrunnlagEntitet::getBesteberegninggrunnlag)
            .map(BeregningsgrunnlagGUIInputTjeneste::mapTilVurderinsgrunnlag);
        var fpGr = besteberegninggrunnlag.map(bbGrunnlag -> new ForeldrepengerGrunnlag(mapTilDekningsgradKalkulator(dekningsgrad.getVerdi()), bbGrunnlag))
            .orElse(new ForeldrepengerGrunnlag(mapTilDekningsgradKalkulator(dekningsgrad.getVerdi()), kvalifisererTilBesteberegning, aktivitetGradering));
        fpGr.setAktivitetGradering(aktivitetGradering);
        return fpGr;
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

    private static BesteberegningVurderingGrunnlag mapTilVurderinsgrunnlag(BesteberegninggrunnlagEntitet besteberegninggrunnlagEntitet) {
        return new BesteberegningVurderingGrunnlag(besteberegninggrunnlagEntitet.getSeksBesteMåneder().stream()
            .map(BeregningsgrunnlagGUIInputTjeneste::mapTilMånedsgrunnlag).toList(), Beløp.fra(besteberegninggrunnlagEntitet.getAvvik().orElse(null)));
    }

    private static BesteberegningMånedGrunnlag mapTilMånedsgrunnlag(BesteberegningMånedsgrunnlagEntitet månedsgrunnlagEntitet) {
        return new BesteberegningMånedGrunnlag(månedsgrunnlagEntitet.getInntekter().stream()
            .map(BeregningsgrunnlagGUIInputTjeneste::mapTilInntekt).toList(), YearMonth.from(månedsgrunnlagEntitet.getPeriode().getFomDato()));
    }

    private static Inntekt mapTilInntekt(BesteberegningInntektEntitet besteberegningInntektEntitet) {
        if (besteberegningInntektEntitet.getArbeidsgiver() != null) {
            return new Inntekt(mapArbeidsgiver(besteberegningInntektEntitet.getArbeidsgiver()),
                mapArbeidsforholdRef(besteberegningInntektEntitet.getArbeidsforholdRef()),
                Beløp.fra(besteberegningInntektEntitet.getInntekt()));
        }
        return new Inntekt(OpptjeningAktivitetType.fraKode(besteberegningInntektEntitet.getOpptjeningAktivitetType().getKode()), Beløp.fra(besteberegningInntektEntitet.getInntekt()));
    }
}
