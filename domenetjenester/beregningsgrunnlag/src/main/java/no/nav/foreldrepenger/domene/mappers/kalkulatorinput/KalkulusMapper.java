package no.nav.foreldrepenger.domene.mappers.kalkulatorinput;

import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;

public class KalkulusMapper {

    private KalkulusMapper() {}

    public static KalkulatorInputDto mapKalkulatorInput(BehandlingReferanse behandlingReferanse,
                                                        InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                        OpptjeningAktiviteter opptjeningAktiviteter) {
        var skjæringstidspunktOpptjening = behandlingReferanse.getSkjæringstidspunkt().getSkjæringstidspunktOpptjening();
        var mappetIayGrunnlag = IAYTilKalkulatorInputMapper.mapTilDto(iayGrunnlag, behandlingReferanse.getAktørId(), skjæringstidspunktOpptjening);
        return new KalkulatorInputDto(
            mappetIayGrunnlag,
            IAYTilKalkulatorInputMapper.mapTilDto(opptjeningAktiviteter),
            skjæringstidspunktOpptjening);
    }

}
