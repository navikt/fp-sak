package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.svp;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.KLASSER_MED_AVHENGIGHETER.SvangerskapspengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFilter;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.TilretteleggingMapperTilKalkulus;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.BeregnTilrettleggingsperioderTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class BeregningsgrunnlagInputTjeneste extends BeregningsgrunnlagInputFelles {

    private BeregnTilrettleggingsperioderTjeneste tilrettleggingsperioderTjeneste;

    protected BeregningsgrunnlagInputTjeneste() {
        // CDI proxy
    }

    @Inject
    public BeregningsgrunnlagInputTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                           AndelGraderingTjeneste andelGraderingTjeneste,
                                           OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                                           BeregnTilrettleggingsperioderTjeneste tilrettleggingsperioderTjeneste, InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        super(behandlingRepositoryProvider.getBehandlingRepository(), iayTjeneste, skjæringstidspunktTjeneste, andelGraderingTjeneste, opptjeningForBeregningTjeneste, inntektsmeldingTjeneste);
        this.tilrettleggingsperioderTjeneste = Objects.requireNonNull(tilrettleggingsperioderTjeneste, "tilrettleggingsperioderTjeneste");
    }

    @Override
    public YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        var tilretteleggingMedUtbelingsgrad = tilrettleggingsperioderTjeneste.beregnPerioder(ref);

        return new SvangerskapspengerGrunnlag(
            TilretteleggingMapperTilKalkulus.mapTilretteleggingerMedUtbetalingsgrad(tilretteleggingMedUtbelingsgrad)
        );
    }

}

