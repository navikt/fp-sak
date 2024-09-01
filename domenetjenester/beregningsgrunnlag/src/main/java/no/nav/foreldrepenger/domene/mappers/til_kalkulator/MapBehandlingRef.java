package no.nav.foreldrepenger.domene.mappers.til_kalkulator;

import no.nav.folketrygdloven.kalkulator.modell.behandling.KoblingReferanse;
import no.nav.folketrygdloven.kalkulator.modell.behandling.Skjæringstidspunkt;
import no.nav.folketrygdloven.kalkulus.typer.AktørId;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;

public class MapBehandlingRef {

    private MapBehandlingRef() {
    }

    public static KoblingReferanse mapRef(BehandlingReferanse behandlingReferanse, no.nav.foreldrepenger.behandling.Skjæringstidspunkt stp) {
        return KoblingReferanse.fra(KodeverkTilKalkulusMapper.mapFagsakytelsetype(behandlingReferanse.fagsakYtelseType()),
            new AktørId(behandlingReferanse.aktørId().getId()),
            behandlingReferanse.behandlingId(),
            behandlingReferanse.behandlingUuid(),
            behandlingReferanse.getOriginalBehandlingId(),
            mapSkjæringstidspunkt(stp));
    }

    private static Skjæringstidspunkt mapSkjæringstidspunkt(no.nav.foreldrepenger.behandling.Skjæringstidspunkt skjæringstidspunkt) {
        return Skjæringstidspunkt.builder()
            .medSkjæringstidspunktOpptjening(skjæringstidspunkt.getSkjæringstidspunktOpptjening())
            .medSkjæringstidspunktBeregning(skjæringstidspunkt.getSkjæringstidspunktBeregningForKopieringTilKalkulus())
            .medFørsteUttaksdato(skjæringstidspunkt.getFørsteUttaksdatoGrunnbeløp())
            .build();
    }
}
