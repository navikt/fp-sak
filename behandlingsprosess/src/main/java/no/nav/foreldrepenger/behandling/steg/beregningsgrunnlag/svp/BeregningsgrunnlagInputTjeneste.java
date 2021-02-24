package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.svp;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.SvangerskapspengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.KalkulusKonfigInjecter;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.TilretteleggingMapperTilKalkulus;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
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
            OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
            BeregnTilrettleggingsperioderTjeneste tilrettleggingsperioderTjeneste, InntektsmeldingTjeneste inntektsmeldingTjeneste,
            KalkulusKonfigInjecter kalkulusKonfigInjecter) {
        super(behandlingRepositoryProvider.getBehandlingRepository(), iayTjeneste, skjæringstidspunktTjeneste,
                opptjeningForBeregningTjeneste, inntektsmeldingTjeneste, kalkulusKonfigInjecter);
        this.tilrettleggingsperioderTjeneste = Objects.requireNonNull(tilrettleggingsperioderTjeneste, "tilrettleggingsperioderTjeneste");
    }

    @Override
    public YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        var tilretteleggingMedUtbelingsgrad = tilrettleggingsperioderTjeneste.beregnPerioder(ref);

        return new SvangerskapspengerGrunnlag(
                TilretteleggingMapperTilKalkulus.mapTilretteleggingerMedUtbetalingsgrad(tilretteleggingMedUtbelingsgrad));
    }

}
