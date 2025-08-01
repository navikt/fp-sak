package no.nav.foreldrepenger.domene.input;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;

public final class MapStegTilTilstand {

    private static final Map<BehandlingStegType, BeregningsgrunnlagTilstand> STEG_TILSTAND = new EnumMap<>(BehandlingStegType.class);
    private static final Map<BehandlingStegType, BeregningsgrunnlagTilstand> STEG_UT_TILSTAND = new EnumMap<>(BehandlingStegType.class);

    static {
        STEG_TILSTAND.put(BehandlingStegType.KONTROLLER_FAKTA_BEREGNING,
            BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        STEG_TILSTAND.put(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.FORESLÅTT);
        STEG_TILSTAND.put(BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.FORESLÅTT_2);
        STEG_TILSTAND.put(BehandlingStegType.FORESLÅ_BESTEBEREGNING, BeregningsgrunnlagTilstand.BESTEBEREGNET);
        STEG_TILSTAND.put(BehandlingStegType.VURDER_VILKAR_BERGRUNN, BeregningsgrunnlagTilstand.VURDERT_VILKÅR);
        STEG_TILSTAND.put(BehandlingStegType.VURDER_REF_BERGRUNN, BeregningsgrunnlagTilstand.VURDERT_REFUSJON);
        STEG_TILSTAND.put(BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG,
            BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
        STEG_TILSTAND.put(BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.FASTSATT);

        STEG_UT_TILSTAND.put(BehandlingStegType.KONTROLLER_FAKTA_BEREGNING, BeregningsgrunnlagTilstand.KOFAKBER_UT);
        STEG_UT_TILSTAND.put(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
        STEG_UT_TILSTAND.put(BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.FORESLÅTT_2_UT);
        STEG_UT_TILSTAND.put(BehandlingStegType.VURDER_REF_BERGRUNN, BeregningsgrunnlagTilstand.VURDERT_REFUSJON_UT);
        STEG_UT_TILSTAND.put(BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.FASTSATT_INN);
    }

    private MapStegTilTilstand() {
    }

    public static BeregningsgrunnlagTilstand mapTilStegTilstand(BehandlingStegType kode) {
        if (STEG_TILSTAND.containsKey(kode)) {
            return STEG_TILSTAND.get(kode);
        }
        throw new IllegalStateException("Finner ikke tilstand for steg " + kode.getKode());
    }

    public static Optional<BeregningsgrunnlagTilstand> mapTilStegUtTilstand(BehandlingStegType kode) {
        return Optional.ofNullable(STEG_UT_TILSTAND.get(kode));
    }

    public static no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand mapTilKalkulatorStegTilstand(
        BehandlingStegType kode) {
        if (STEG_TILSTAND.containsKey(kode)) {
            return mapTilKalkulatorTilstand(STEG_TILSTAND.get(kode));
        }
        throw new IllegalStateException("Finner ikke tilstand for steg " + kode.getKode());
    }

    public static Optional<no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand> mapTilKalkulatorStegUtTilstand(
        BehandlingStegType kode) {
        return Optional.ofNullable(mapTilKalkulatorTilstand(STEG_UT_TILSTAND.get(kode)));
    }

    private static no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand mapTilKalkulatorTilstand(
        BeregningsgrunnlagTilstand tilstand) {
        return tilstand == null ? null : no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand.fraKode(
            tilstand.getKode());
    }


}
