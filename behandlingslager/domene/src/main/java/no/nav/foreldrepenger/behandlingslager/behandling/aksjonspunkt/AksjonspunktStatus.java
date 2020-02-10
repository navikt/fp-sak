package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum AksjonspunktStatus implements Kodeverdi {


    AVBRUTT ("AVBR", "Avbrutt"),
    OPPRETTET("OPPR", "Opprettet"),
    UTFØRT ("UTFO", "Utført"),
    ;

    public static final String KODEVERK = "AKSJONSPUNKT_STATUS";
    private static final Map<String, AksjonspunktStatus> KODER = new LinkedHashMap<>();
    private static final List<AksjonspunktStatus> ÅPNE_AKSJONSPUNKT_KODER = List.of(OPPRETTET);

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    private String kode;

    private AksjonspunktStatus(String kode) {
        this.kode = kode;
    }

    private AksjonspunktStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonCreator
    public static AksjonspunktStatus fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AksjonspunktStatus: " + kode);
        }
        return ad;
    }

    public static Map<String, AksjonspunktStatus> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    public boolean erÅpentAksjonspunkt() {
        return ÅPNE_AKSJONSPUNKT_KODER.contains(this);  // NOSONAR
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
    }

}
