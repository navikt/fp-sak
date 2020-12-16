package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.util.env.Environment;

/**
 * Midlertidig konfiginjecter for kalkulus
 */
@ApplicationScoped
public class KalkulusKonfigInjecter {

    private static final String INNTEKT_RAPPORTERING_FRIST_DATO = "inntekt.rapportering.frist.dato";
    private static final Map<String, Boolean> TOGGLES = new HashMap<>();
    private int inntektRapporteringFristDagIMåneden;

    static {
        TOGGLES.put("fpsak.splitteSammenligningATFL", false);
        TOGGLES.put("automatisk-besteberegning", !Environment.current().isProd());
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
