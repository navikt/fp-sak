package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.domene.entiteter.Inntektskategori;

public class MapInntektskategoriFraBehandlingslagerTilBeregningsgrunnlag {

    public static Inntektskategori mapInntektskategori(no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori inntektskategori) {
        return Inntektskategori.fraKode(inntektskategori.getKode());
    }


}
