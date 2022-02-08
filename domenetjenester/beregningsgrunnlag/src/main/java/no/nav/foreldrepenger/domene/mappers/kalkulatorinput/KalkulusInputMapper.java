package no.nav.foreldrepenger.domene.mappers.kalkulatorinput;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;

@ApplicationScoped
public class KalkulusInputMapper {

    private Instance<YtelsesspesifiktGrunnlagMapper> ytelsesspesifiktGrunnlagMappers;

    public KalkulusInputMapper() {
    }

    @Inject
    public KalkulusInputMapper(@Any Instance<YtelsesspesifiktGrunnlagMapper> ytelsesspesifiktGrunnlagMappers) {
        this.ytelsesspesifiktGrunnlagMappers = ytelsesspesifiktGrunnlagMappers;
    }


    public KalkulatorInputDto mapKalkulatorInput(BehandlingReferanse behandlingReferanse,
                                                        InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                        OpptjeningAktiviteter opptjeningAktiviteter) {
        var skjæringstidspunktOpptjening = behandlingReferanse.getSkjæringstidspunkt().getSkjæringstidspunktOpptjening();
        var mappetIayGrunnlag = IAYTilKalkulatorInputMapper.mapTilDto(iayGrunnlag, behandlingReferanse.getAktørId(), skjæringstidspunktOpptjening);
        var kalkulatorInputDto = new KalkulatorInputDto(mappetIayGrunnlag, IAYTilKalkulatorInputMapper.mapTilDto(opptjeningAktiviteter),
            skjæringstidspunktOpptjening);
        var ytelsesspesifiktGrunnlag = YtelsesspesifiktGrunnlagMapper.finnTjeneste(behandlingReferanse.getFagsakYtelseType(),
            ytelsesspesifiktGrunnlagMappers).mapYtelsesspesifiktGrunnlag(behandlingReferanse);
        kalkulatorInputDto.medYtelsespesifiktGrunnlag(ytelsesspesifiktGrunnlag);
        return kalkulatorInputDto;
    }

}
