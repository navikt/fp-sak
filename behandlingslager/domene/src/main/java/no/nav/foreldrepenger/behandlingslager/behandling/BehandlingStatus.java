package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

/**
 * NB: Pass på! Ikke legg koder vilkårlig her
 * Denne definerer etablerte behandlingstatuser ihht. modell angitt av FFA (Forretning og Fag).
 */
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingStatus implements Kodeverdi {

    AVSLUTTET("AVSLU", "Avsluttet"),
    FATTER_VEDTAK("FVED", "Fatter vedtak"),
    IVERKSETTER_VEDTAK("IVED", "Iverksetter vedtak"),
    OPPRETTET("OPPRE", "Opprettet"),
    UTREDES("UTRED", "Behandling utredes"),
    
    ;
    
    private static final Map<String, BehandlingStatus> KODER = new LinkedHashMap<>();
    
    public static final String KODEVERK = "BEHANDLING_STATUS";

    private static final Set<BehandlingStatus> FERDIGBEHANDLET_STATUS = Set.of(AVSLUTTET, IVERKSETTER_VEDTAK);

    @JsonIgnore
    private String navn;
    
    private String kode;

    private BehandlingStatus(String kode) {
        this.kode = kode;
    }

    private BehandlingStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static BehandlingStatus fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingStatus: " + kode);
        }
        return ad;
    }

    public static Map<String, BehandlingStatus> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }
    
    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }
    
    public static Set<BehandlingStatus> getFerdigbehandletStatuser() {
        return FERDIGBEHANDLET_STATUS;
    }

    public boolean erFerdigbehandletStatus() {
        return FERDIGBEHANDLET_STATUS.contains(this);
    }
    
    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
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
    
    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }
    
    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<BehandlingStatus, String> {
        @Override
        public String convertToDatabaseColumn(BehandlingStatus attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BehandlingStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
