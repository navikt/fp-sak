package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import java.util.stream.Collectors;

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
    private int inntektRapporteringFristDagIMåneden;

    private Unleash unleash;

    public KalkulusKonfigInjecter() {
        // CDI
    }

    @Inject
    public KalkulusKonfigInjecter(@KonfigVerdi(value = INNTEKT_RAPPORTERING_FRIST_DATO, defaultVerdi = "5") int inntektRapporteringFristDagIMåneden, Unleash unleash) {
        this.inntektRapporteringFristDagIMåneden = inntektRapporteringFristDagIMåneden;
        this.unleash = unleash;
    }

    void leggTilKonfigverdier(BeregningsgrunnlagInput input) {
        leggTilToggles(input);
        input.leggTilKonfigverdi(INNTEKT_RAPPORTERING_FRIST_DATO, inntektRapporteringFristDagIMåneden);
    }

    private void leggTilToggles(BeregningsgrunnlagInput input) {
        input.setToggles(unleash.getFeatureToggleNames().stream().collect(Collectors.toMap(feature -> feature, (f) -> unleash.isEnabled(f))));
    }

}
