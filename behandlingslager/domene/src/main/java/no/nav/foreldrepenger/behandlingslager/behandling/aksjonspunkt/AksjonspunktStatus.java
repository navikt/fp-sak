package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum AksjonspunktStatus implements Kodeverdi {

    AVBRUTT ("AVBR", "Avbrutt"),
    OPPRETTET("OPPR", "Opprettet"),
    UTFØRT ("UTFO", "Utført"),
    ;

    private static final Map<String, AksjonspunktStatus> KODER = new LinkedHashMap<>();
    private static final List<AksjonspunktStatus> ÅPNE_AKSJONSPUNKT_KODER = List.of(OPPRETTET);

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

    AksjonspunktStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public boolean erÅpentAksjonspunkt() {
        return ÅPNE_AKSJONSPUNKT_KODER.contains(this);
    }

    public static List<AksjonspunktStatus> getÅpneAksjonspunktStatuser() {
        return new ArrayList<>(ÅPNE_AKSJONSPUNKT_KODER);
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<AksjonspunktStatus, String> {
        @Override
        public String convertToDatabaseColumn(AksjonspunktStatus attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AksjonspunktStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static AksjonspunktStatus fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent AksjonspunktStatus: " + kode);
            }
            return ad;
        }
    }

}
