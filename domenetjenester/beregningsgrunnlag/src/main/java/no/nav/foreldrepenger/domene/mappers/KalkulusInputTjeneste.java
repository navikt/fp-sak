package no.nav.foreldrepenger.domene.mappers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapIAYTilKalkulusInput;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapKalkulusYtelsegrunnlag;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapKravperioder;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapOpptjeningTilKalkulusInput;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class KalkulusInputTjeneste {
    private Instance<MapKalkulusYtelsegrunnlag> ytelsegrunnlagMappere;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;


    public KalkulusInputTjeneste() {
        // CDI
    }

    @Inject
    public KalkulusInputTjeneste(@Any Instance<MapKalkulusYtelsegrunnlag> ytelsegrunnlagMappere,
                                 InntektArbeidYtelseTjeneste iayTjeneste,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                 OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                                 InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.ytelsegrunnlagMappere = ytelsegrunnlagMappere;
        this.iayTjeneste = iayTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    public KalkulatorInputDto lagKalkulusInput(BehandlingReferanse ref) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(ref.behandlingId());
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.behandlingId());

        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(ref, skjæringstidspunkt, iayGrunnlag);
        if (opptjeningAktiviteter.isEmpty()) {
            throw new IllegalStateException(String.format("No value present: Fant ikke forventet OpptjeningAktiviteter for behandling: %s med saksnummer: %s", ref.behandlingId(), ref.saksnummer()));
        }
        var alleInntektsmeldingerForSak = inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsak(ref.saksnummer());
        var inntektsmeldingerForBehandling = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, skjæringstidspunkt.getUtledetSkjæringstidspunkt(), iayGrunnlag, true);
        var kravperioderDto = MapKravperioder.map(ref, skjæringstidspunkt.getSkjæringstidspunktOpptjening(), alleInntektsmeldingerForSak, iayGrunnlag);
        var iayDto = MapIAYTilKalkulusInput.mapIAY(iayGrunnlag, inntektsmeldingerForBehandling, ref);
        var opptjeningDto = MapOpptjeningTilKalkulusInput.mapOpptjening(opptjeningAktiviteter.get(), iayGrunnlag, ref, skjæringstidspunkt);
        var kalkulatorInputDto = new KalkulatorInputDto(iayDto, opptjeningDto, skjæringstidspunkt.getSkjæringstidspunktOpptjening());
        kalkulatorInputDto.medRefusjonsperioderPrInntektsmelding(kravperioderDto);
        var ytelseMapper = FagsakYtelseTypeRef.Lookup.find(ytelsegrunnlagMappere, ref.fagsakYtelseType()).orElseThrow();
        YtelsespesifiktGrunnlagDto ytelsegrunnlag = ytelseMapper.mapYtelsegrunnlag(ref, skjæringstidspunkt);
        kalkulatorInputDto.medYtelsespesifiktGrunnlag(ytelsegrunnlag);
        return kalkulatorInputDto;
    }
}
