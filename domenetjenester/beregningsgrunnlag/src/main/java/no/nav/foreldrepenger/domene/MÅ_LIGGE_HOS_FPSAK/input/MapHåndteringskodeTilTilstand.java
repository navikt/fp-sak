package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input;

import java.util.HashMap;
import java.util.Map;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;

public class MapHåndteringskodeTilTilstand {

    private static Map<String, BeregningsgrunnlagTilstand> map = new HashMap<>();

    static {
        map.put(AksjonspunktKodeDefinisjon.AVKLAR_AKTIVITETER_KODE, BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        map.put(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE, BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        map.put(AksjonspunktKodeDefinisjon.VURDER_FAKTA_FOR_ATFL_SN_KODE, BeregningsgrunnlagTilstand.KOFAKBER_UT);
        map.put(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE, BeregningsgrunnlagTilstand.KOFAKBER_UT);
        map.put(AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
        map.put(AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE_KODE, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
        map.put(AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
        map.put(AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
        map.put(AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
        map.put(AksjonspunktKodeDefinisjon.VURDER_REFUSJON_BERGRUNN, BeregningsgrunnlagTilstand.VURDERT_REFUSJON_UT);
        map.put(AksjonspunktKodeDefinisjon.FORDEL_BEREGNINGSGRUNNLAG_KODE, BeregningsgrunnlagTilstand.FASTSATT_INN);
    }

    public static BeregningsgrunnlagTilstand map(String kode) {
        if (map.containsKey(kode)) {
            return map.get(kode);
        }
        throw new IllegalStateException("Finner ikke tilstand for kode " + kode);
    }

}
