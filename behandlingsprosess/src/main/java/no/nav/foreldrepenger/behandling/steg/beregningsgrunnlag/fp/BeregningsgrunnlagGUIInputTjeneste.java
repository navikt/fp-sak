package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.fp;

import static no.nav.foreldrepenger.domene.mappers.til_kalkulus.IAYMapperTilKalkulus.mapArbeidsforholdRef;
import static no.nav.foreldrepenger.domene.mappers.til_kalkulus.IAYMapperTilKalkulus.mapArbeidsgiver;

import java.math.BigDecimal;
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
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagGUIInputFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BesteberegningInntektEntitet;
import no.nav.foreldrepenger.domene.entiteter.BesteberegningMånedsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BesteberegninggrunnlagEntitet;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class BeregningsgrunnlagGUIInputTjeneste extends BeregningsgrunnlagGUIInputFelles {

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private HentOgLagreBeregningsgrunnlagTjeneste hentOgLagreBeregningsgrunnlagTjeneste;
    private BeregningUttakTjeneste beregningUttakTjeneste;

    protected BeregningsgrunnlagGUIInputTjeneste() {
        // CDI proxy
    }

    @Inject
    public BeregningsgrunnlagGUIInputTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                              InntektArbeidYtelseTjeneste iayTjeneste,
                                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                              BeregningUttakTjeneste beregningUttakTjeneste,
                                              BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste,
                                              InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                              HentOgLagreBeregningsgrunnlagTjeneste hentOgLagreBeregningsgrunnlagTjeneste,
                                              FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        super(behandlingRepositoryProvider.getBehandlingRepository(), iayTjeneste, skjæringstidspunktTjeneste, inntektsmeldingTjeneste);
        this.fagsakRelasjonTjeneste = Objects.requireNonNull(fagsakRelasjonTjeneste, "fagsakRelasjonTjeneste");
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.hentOgLagreBeregningsgrunnlagTjeneste = hentOgLagreBeregningsgrunnlagTjeneste;
        this.beregningUttakTjeneste = Objects.requireNonNull(beregningUttakTjeneste, "andelGrderingTjeneste");
    }

    @Override
    public YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        var saksnummer = ref.saksnummer();
        var aktivitetGradering = beregningUttakTjeneste.finnAktivitetGraderinger(ref);
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonHvisEksisterer(saksnummer);
        var dekningsgrad = fagsakRelasjon.map(FagsakRelasjon::getGjeldendeDekningsgrad)
                .orElseThrow(() -> new IllegalStateException("Mangler FagsakRelasjon#dekningsgrad for behandling: " + ref));
        var kvalifisererTilBesteberegning = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref);
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
