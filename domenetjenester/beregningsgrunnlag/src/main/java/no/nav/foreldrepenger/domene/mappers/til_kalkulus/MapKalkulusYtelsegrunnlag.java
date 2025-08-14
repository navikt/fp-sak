package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;

public interface MapKalkulusYtelsegrunnlag {
    YtelsespesifiktGrunnlagDto mapYtelsegrunnlag(BehandlingReferanse referanse, Skjæringstidspunkt stp);
}
