package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;

public class MapAkivitetStatusFraBehandlingslagerTilBeregningsgrunnlag {

    public static AktivitetStatus mapAktivitetStatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus aktivitetStatus) {
        return AktivitetStatus.fraKode(aktivitetStatus.getKode());
    }


}
