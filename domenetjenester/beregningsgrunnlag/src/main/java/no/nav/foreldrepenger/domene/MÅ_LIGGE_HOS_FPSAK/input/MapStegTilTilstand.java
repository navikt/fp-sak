package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;

public class MapStegTilTilstand {

    private static Map<BehandlingStegType, BeregningsgrunnlagTilstand> mapStegTilstand = new HashMap<>();
    private static Map<BehandlingStegType, BeregningsgrunnlagTilstand> mapStegUtTilstand = new HashMap<>();

    static {
        mapStegTilstand.put(BehandlingStegType.KONTROLLER_FAKTA_BEREGNING, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        mapStegTilstand.put(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.FORESLÅTT);
        mapStegTilstand.put(BehandlingStegType.VURDER_REF_BERGRUNN, BeregningsgrunnlagTilstand.VURDERT_REFUSJON);
        mapStegTilstand.put(BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
        mapStegTilstand.put(BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.FASTSATT);

        mapStegUtTilstand.put(BehandlingStegType.KONTROLLER_FAKTA_BEREGNING, BeregningsgrunnlagTilstand.KOFAKBER_UT);
        mapStegUtTilstand.put(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
        mapStegUtTilstand.put(BehandlingStegType.VURDER_REF_BERGRUNN, BeregningsgrunnlagTilstand.VURDERT_REFUSJON_UT);
        mapStegUtTilstand.put(BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.FASTSATT_INN);
    }

    public static BeregningsgrunnlagTilstand mapTilStegTilstand(BehandlingStegType kode) {
        if (mapStegTilstand.containsKey(kode)) {
            return mapStegTilstand.get(kode);
        }
        throw new IllegalStateException("Finner ikke tilstand for steg " + kode.getKode());
    }

    public static Optional<BeregningsgrunnlagTilstand> mapTilStegUtTilstand(BehandlingStegType kode) {
        return Optional.ofNullable(mapStegUtTilstand.get(kode));
    }


    public static no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningsgrunnlagTilstand mapTilKalkulatorStegTilstand(BehandlingStegType kode) {
        if (mapStegTilstand.containsKey(kode)) {
            return mapTilKalkulatorTilstand(mapStegTilstand.get(kode));
        }
        throw new IllegalStateException("Finner ikke tilstand for steg " + kode.getKode());
    }

    public static Optional<no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningsgrunnlagTilstand> mapTilKalkulatorStegUtTilstand(BehandlingStegType kode) {
        return Optional.ofNullable(mapTilKalkulatorTilstand(mapStegUtTilstand.get(kode)));
    }

    private static no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningsgrunnlagTilstand mapTilKalkulatorTilstand(BeregningsgrunnlagTilstand tilstand) {
        return tilstand == null ? null : no.nav.folketrygdloven.kalkulus.felles.kodeverk.domene.BeregningsgrunnlagTilstand.fraKode(tilstand.getKode());
    }


}
