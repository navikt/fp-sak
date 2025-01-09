package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@Deprecated(forRemoval = true)
public enum HistorikkResultatType implements Kodeverdi {
    AVVIS_KLAGE("AVVIS_KLAGE", "Klagen er avvist"),
    BEREGNET_AARSINNTEKT("BEREGNET_AARSINNTEKT", "Grunnlag for beregnet årsinntekt"),
    MEDHOLD_I_KLAGE("MEDHOLD_I_KLAGE", "Vedtaket er omgjort"),
    OPPHEVE_VEDTAK("OPPHEVE_VEDTAK", "Vedtaket er opphevet"),
    OPPRETTHOLDT_VEDTAK("OPPRETTHOLDT_VEDTAK", "Vedtaket er opprettholdt"),
    OVERSTYRING_FAKTA_UTTAK("OVERSTYRING_FAKTA_UTTAK", "Overstyrt vurdering:"),
    STADFESTET_VEDTAK("STADFESTET_VEDTAK", "Vedtaket er stadfestet"),
    UTFALL_UENDRET("UTFALL_UENDRET", "Overstyrt vurdering: Utfall er uendret"),
    ;

    private static final Map<String, HistorikkResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_RESULTAT_TYPE";

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

    HistorikkResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, HistorikkResultatType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<HistorikkResultatType, String> {
        @Override
        public String convertToDatabaseColumn(HistorikkResultatType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public HistorikkResultatType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static HistorikkResultatType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent HistorikkResultatType: " + kode);
            }
            return ad;
        }
    }
}
