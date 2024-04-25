package no.nav.foreldrepenger.domene.mappers.input;

import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;

public interface MapKalkulusYtelsegrunnlag {
    YtelsespesifiktGrunnlagDto mapYtelsegrunnlag(BehandlingReferanse referanse);
}
