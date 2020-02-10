package no.nav.foreldrepenger.behandlingslager.fagsak;

import java.util.Collections;
import java.util.LinkedHashMap;
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
public enum FagsakStatus implements Kodeverdi {

    OPPRETTET("OPPR", "Opprettet"),
    UNDER_BEHANDLING("UBEH", "Under behandling"),
    LØPENDE("LOP", "Løpende"),
    AVSLUTTET("AVSLU", "Avsluttet"),
    ;

    public static final FagsakStatus DEFAULT = OPPRETTET;
    private static final Map<String, FagsakStatus> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "FAGSAK_STATUS";

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

    private FagsakStatus(String kode) {
        this.kode = kode;
    }

    private FagsakStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static FagsakStatus fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FagsakStatus: " + kode);
        }
        return ad;
    }

    public static Map<String, FagsakStatus> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
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

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<FagsakStatus, String> {
        @Override
        public String convertToDatabaseColumn(FagsakStatus attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public FagsakStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
