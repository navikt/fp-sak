package no.nav.foreldrepenger.ytelse.beregning;

import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;

public class MapInntektskategoriFraBehandlingslagerTilBeregningsgrunnlag {

    public static Inntektskategori mapInntektskategori(no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori inntektskategori) {
        return Inntektskategori.fraKode(inntektskategori.getKode());
    }


}
