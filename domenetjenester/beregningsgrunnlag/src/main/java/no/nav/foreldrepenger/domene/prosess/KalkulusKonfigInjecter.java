package no.nav.foreldrepenger.domene.prosess;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

/**
 * Midlertidig konfiginjecter for kalkulus
 */
@ApplicationScoped
public class KalkulusKonfigInjecter {

    private static final String INNTEKT_RAPPORTERING_FRIST_DATO = "inntekt.rapportering.frist.dato";
    private static final Map<String, Boolean> TOGGLES = new HashMap<>();
    private int inntektRapporteringFristDagIMåneden;

    static {
        // Kan fjernes når vi har fått gui for å vise lønnsendring
        TOGGLES.put("AUTOMATISK_BERGNE_LØNNENDRING", false);
    }

    public KalkulusKonfigInjecter() {
        // CDI
    }

    @Inject
    public KalkulusKonfigInjecter(@KonfigVerdi(value = INNTEKT_RAPPORTERING_FRIST_DATO, defaultVerdi = "5") int inntektRapporteringFristDagIMåneden) {
        this.inntektRapporteringFristDagIMåneden = inntektRapporteringFristDagIMåneden;
    }

    public void leggTilKonfigverdier(BeregningsgrunnlagInput input) {
        input.leggTilKonfigverdi(INNTEKT_RAPPORTERING_FRIST_DATO, inntektRapporteringFristDagIMåneden);
    }

    public void leggTilFeatureToggles(BeregningsgrunnlagInput input) {
        input.setToggles(TOGGLES);
    }

}
