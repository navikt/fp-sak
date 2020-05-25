package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.finn.unleash.Unleash;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.vedtak.konfig.KonfigVerdi;

/**
 * Midlertidig konfiginjecter for kalkulus
 */
@ApplicationScoped
public class KalkulusKonfigInjecter {

    private static final String INNTEKT_RAPPORTERING_FRIST_DATO = "inntekt.rapportering.frist.dato";
    private static List<String> TOGGLES = new ArrayList<>();
    private int inntektRapporteringFristDagIMåneden;

    private Unleash unleash;

    static {
        TOGGLES.add("fpsak.splitteSammenligningATFL");
    }

    public KalkulusKonfigInjecter() {
        // CDI
    }

    @Inject
    public KalkulusKonfigInjecter(@KonfigVerdi(value = INNTEKT_RAPPORTERING_FRIST_DATO, defaultVerdi = "5") int inntektRapporteringFristDagIMåneden, Unleash unleash) {
        this.inntektRapporteringFristDagIMåneden = inntektRapporteringFristDagIMåneden;
        this.unleash = unleash;
    }

    void leggTilKonfigverdier(BeregningsgrunnlagInput input) {
        input.leggTilKonfigverdi(INNTEKT_RAPPORTERING_FRIST_DATO, inntektRapporteringFristDagIMåneden);
    }

    public void leggTilFeatureToggles(BeregningsgrunnlagInput input) {
        Map<String, Boolean> toggleMap = new HashMap<>();
        TOGGLES.forEach(toggle -> toggleMap.put(toggle, unleash.isEnabled(toggle)));
        input.setToggles(toggleMap);
    }

}
