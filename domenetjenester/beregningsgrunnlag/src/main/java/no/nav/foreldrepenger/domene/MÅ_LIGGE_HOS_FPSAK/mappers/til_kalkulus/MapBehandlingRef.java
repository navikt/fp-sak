package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus;

import no.nav.folketrygdloven.kalkulator.modell.behandling.KoblingReferanse;
import no.nav.folketrygdloven.kalkulator.modell.behandling.Skjæringstidspunkt;
import no.nav.folketrygdloven.kalkulus.typer.AktørId;
import no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType;

//TODO(OJR) skal fjernes
public class MapBehandlingRef {

    public static KoblingReferanse mapRef(no.nav.foreldrepenger.behandling.BehandlingReferanse behandlingReferanse) {
        return KoblingReferanse.fra(FagsakYtelseType.fraKode(behandlingReferanse.getFagsakYtelseType().getKode()),
            new AktørId(behandlingReferanse.getAktørId().getId()),
            behandlingReferanse.getBehandlingId(),
            behandlingReferanse.getBehandlingUuid(),
            behandlingReferanse.getOriginalBehandlingId(),
            mapSkjæringstidspunkt(behandlingReferanse.getSkjæringstidspunkt()));
    }

    private static Skjæringstidspunkt mapSkjæringstidspunkt(no.nav.foreldrepenger.behandling.Skjæringstidspunkt skjæringstidspunkt) {
        return Skjæringstidspunkt.builder()
            .medSkjæringstidspunktOpptjening(skjæringstidspunkt.getSkjæringstidspunktOpptjening())
            .medSkjæringstidspunktBeregning(skjæringstidspunkt.getSkjæringstidspunktBeregningForKopieringTilKalkulus())
            .medFørsteUttaksdato(skjæringstidspunkt.getFørsteUttaksdato())
            .build();
    }
}
