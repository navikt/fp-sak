package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.svp;

import no.nav.folketrygdloven.kalkulator.input.SvangerskapspengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagGUIInputFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.TilretteleggingMapperTilKalkulus;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.BeregnTilrettleggingsperioderTjeneste;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Objects;

@ApplicationScoped
@FagsakYtelseTypeRef("SVP")
public class BeregningsgrunnlagGUIInputTjeneste extends BeregningsgrunnlagGUIInputFelles {

    private BeregnTilrettleggingsperioderTjeneste tilrettleggingsperioderTjeneste;

    protected BeregningsgrunnlagGUIInputTjeneste() {
        // CDI proxy
    }

    @Inject
    public BeregningsgrunnlagGUIInputTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                              InntektArbeidYtelseTjeneste iayTjeneste,
                                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                              BeregnTilrettleggingsperioderTjeneste tilrettleggingsperioderTjeneste,
                                              InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                              OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste) {
        super(behandlingRepositoryProvider.getBehandlingRepository(), iayTjeneste, skjæringstidspunktTjeneste,
                inntektsmeldingTjeneste, opptjeningForBeregningTjeneste);
        this.tilrettleggingsperioderTjeneste = Objects.requireNonNull(tilrettleggingsperioderTjeneste, "tilrettleggingsperioderTjeneste");
    }

    @Override
    public YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        var tilretteleggingMedUtbelingsgrad = tilrettleggingsperioderTjeneste.beregnPerioder(ref);

        return new SvangerskapspengerGrunnlag(
                TilretteleggingMapperTilKalkulus.mapTilretteleggingerMedUtbetalingsgrad(tilretteleggingMedUtbelingsgrad));
    }

}
