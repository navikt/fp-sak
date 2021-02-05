package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.svp;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.SvangerskapspengerGrunnlag;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagGUIInputFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.TilretteleggingMapperTilKalkulus;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.BeregnTilrettleggingsperioderTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

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
            AndelGraderingTjeneste andelGraderingTjeneste,
            BeregnTilrettleggingsperioderTjeneste tilrettleggingsperioderTjeneste,
            InntektsmeldingTjeneste inntektsmeldingTjeneste,
            ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        super(behandlingRepositoryProvider.getBehandlingRepository(), iayTjeneste, skjæringstidspunktTjeneste, andelGraderingTjeneste,
                inntektsmeldingTjeneste, arbeidsgiverTjeneste);
        this.tilrettleggingsperioderTjeneste = Objects.requireNonNull(tilrettleggingsperioderTjeneste, "tilrettleggingsperioderTjeneste");
    }

    @Override
    public YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        var tilretteleggingMedUtbelingsgrad = tilrettleggingsperioderTjeneste.beregnPerioder(ref);

        return new SvangerskapspengerGrunnlag(
                TilretteleggingMapperTilKalkulus.mapTilretteleggingerMedUtbetalingsgrad(tilretteleggingMedUtbelingsgrad));
    }

}
