package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import no.nav.folketrygdloven.kalkulator.modell.behandling.KoblingReferanse;
import no.nav.folketrygdloven.kalkulator.modell.behandling.Skjæringstidspunkt;
import no.nav.folketrygdloven.kalkulus.kodeverk.FagsakYtelseType;
import no.nav.folketrygdloven.kalkulus.typer.AktørId;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;

public class MapBehandlingRef {

    private MapBehandlingRef() {
    }

    public static KoblingReferanse mapRef(BehandlingReferanse behandlingReferanse) {
        return KoblingReferanse.fra(FagsakYtelseType.fraKode(behandlingReferanse.fagsakYtelseType().getKode()),
            new AktørId(behandlingReferanse.aktørId().getId()),
            behandlingReferanse.behandlingId(),
            behandlingReferanse.behandlingUuid(),
            behandlingReferanse.getOriginalBehandlingId(),
            mapSkjæringstidspunkt(behandlingReferanse.getSkjæringstidspunkt()));
    }

    private static Skjæringstidspunkt mapSkjæringstidspunkt(no.nav.foreldrepenger.behandling.Skjæringstidspunkt skjæringstidspunkt) {
        return Skjæringstidspunkt.builder()
            .medSkjæringstidspunktOpptjening(skjæringstidspunkt.getSkjæringstidspunktOpptjening())
            .medSkjæringstidspunktBeregning(skjæringstidspunkt.getSkjæringstidspunktBeregningForKopieringTilKalkulus())
            .medFørsteUttaksdato(skjæringstidspunkt.getFørsteUttaksdatoGrunnbeløp())
            .build();
    }
}
