package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;

public class MapAkivitetStatusFraBehandlingslagerTilBeregningsgrunnlag {

    public static AktivitetStatus mapAktivitetStatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus aktivitetStatus) {
        return AktivitetStatus.fraKode(aktivitetStatus.getKode());
    }


}
