package no.nav.foreldrepenger.domene.input;

import java.util.EnumMap;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;

public final class MapHåndteringskodeTilTilstand {

    private static final Map<AksjonspunktDefinisjon, BeregningsgrunnlagTilstand> MAP = new EnumMap<>(AksjonspunktDefinisjon.class);

    static {
        MAP.put(AksjonspunktDefinisjon.AVKLAR_AKTIVITETER, BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        MAP.put(AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER, BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        MAP.put(AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN, BeregningsgrunnlagTilstand.KOFAKBER_UT);
        MAP.put(AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.KOFAKBER_UT);
        MAP.put(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
        MAP.put(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET, BeregningsgrunnlagTilstand.FORESLÅTT_2_UT);
        MAP.put(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
        MAP.put(AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE, BeregningsgrunnlagTilstand.FORESLÅTT_2_UT);
        MAP.put(AksjonspunktDefinisjon.VURDER_REFUSJON_BERGRUNN, BeregningsgrunnlagTilstand.VURDERT_REFUSJON_UT);
        MAP.put(AksjonspunktDefinisjon.FORDEL_BEREGNINGSGRUNNLAG, BeregningsgrunnlagTilstand.FASTSATT_INN);
    }

    private MapHåndteringskodeTilTilstand() {

    }

    public static BeregningsgrunnlagTilstand map(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        if (MAP.containsKey(aksjonspunktDefinisjon)) {
            return MAP.get(aksjonspunktDefinisjon);
        }
        throw new IllegalStateException("Finner ikke tilstand for kode " + aksjonspunktDefinisjon.getKode());
    }

}
