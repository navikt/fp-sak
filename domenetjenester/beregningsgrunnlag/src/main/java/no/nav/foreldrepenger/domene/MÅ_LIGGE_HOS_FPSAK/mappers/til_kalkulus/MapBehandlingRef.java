package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus;

import no.nav.folketrygdloven.kalkulator.modell.behandling.BehandlingReferanse;
import no.nav.folketrygdloven.kalkulator.modell.behandling.BehandlingStatus;
import no.nav.folketrygdloven.kalkulator.modell.behandling.BehandlingType;
import no.nav.folketrygdloven.kalkulator.modell.behandling.FagsakYtelseType;
import no.nav.folketrygdloven.kalkulator.modell.behandling.RelasjonsRolleType;
import no.nav.folketrygdloven.kalkulator.modell.behandling.Skjæringstidspunkt;
import no.nav.folketrygdloven.kalkulator.modell.typer.AktørId;

//TODO(OJR) skal fjernes
public class MapBehandlingRef {

    public static BehandlingReferanse mapRef(no.nav.foreldrepenger.behandling.BehandlingReferanse behandlingReferanse) {
        return BehandlingReferanse.fra(FagsakYtelseType.fraKode(behandlingReferanse.getFagsakYtelseType().getKode()),
            BehandlingType.fraKode(behandlingReferanse.getBehandlingType().getKode()),
            RelasjonsRolleType.fraKode(behandlingReferanse.getRelasjonsRolleType().getKode()),
            new AktørId(behandlingReferanse.getAktørId().getId()),
            behandlingReferanse.getFagsakId(),
            behandlingReferanse.getBehandlingId(),
            behandlingReferanse.getBehandlingUuid(),
            behandlingReferanse.getOriginalBehandlingId(),
            BehandlingStatus.fraKode(behandlingReferanse.getBehandlingStatus().getKode()),
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
