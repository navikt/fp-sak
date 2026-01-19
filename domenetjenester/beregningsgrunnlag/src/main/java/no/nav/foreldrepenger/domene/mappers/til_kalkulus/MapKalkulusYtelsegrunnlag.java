package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.input.YtelsespesifiktGrunnlagDto;

public interface MapKalkulusYtelsegrunnlag {
    YtelsespesifiktGrunnlagDto mapYtelsegrunnlag(BehandlingReferanse referanse, Skjæringstidspunkt stp);
}
