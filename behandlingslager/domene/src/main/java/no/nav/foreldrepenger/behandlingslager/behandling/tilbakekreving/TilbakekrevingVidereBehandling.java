package no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum TilbakekrevingVidereBehandling implements Kodeverdi {

    UDEFINIERT(STANDARDKODE_UDEFINERT, "Udefinert."),
    OPPRETT_TILBAKEKREVING("TILBAKEKR_OPPRETT", "Feilutbetaling med tilbakekreving"),
    IGNORER_TILBAKEKREVING("TILBAKEKR_IGNORER", "Feilutbetaling, avvent samordning"),
    INNTREKK("TILBAKEKR_INNTREKK", "Feilutbetalingen er trukket inn i annen utbetaling"),
    TILBAKEKR_OPPDATER("TILBAKEKR_OPPDATER", "Endringer vil oppdatere eksisterende feilutbetalte perioder og beløp."),
    ;

    private static final Map<String, TilbakekrevingVidereBehandling> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;

    @JsonValue
    private final String kode;

    TilbakekrevingVidereBehandling(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<TilbakekrevingVidereBehandling, String> {
        @Override
        public String convertToDatabaseColumn(TilbakekrevingVidereBehandling attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public TilbakekrevingVidereBehandling convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static TilbakekrevingVidereBehandling fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent TilbakekrevingVidereBehandling: " + kode);
            }
            return ad;
        }
    }
}
