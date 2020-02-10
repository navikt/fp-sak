package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
public enum DokumentKategori implements Kodeverdi {

    UDEFINERT("-", "Ikke definert", null),
    KLAGE_ELLER_ANKE("KLGA", "Klage eller anke", "KA"),
    IKKE_TOLKBART_SKJEMA("ITSKJ", "Ikke tolkbart skjema", "IS"),
    SØKNAD("SOKN", "Søknad", "SOK"),
    ELEKTRONISK_SKJEMA("ESKJ", "Elektronisk skjema", "ES"),
    BRV("BRV", "Brev", "B"),
    EDIALOG("EDIALOG", "Elektronisk dialog", "ELEKTRONISK_DIALOG"),
    FNOT("FNOT", "Forvaltningsnotat", "FORVALTNINGSNOTAT"),
    IBRV("IBRV", "Informasjonsbrev", "IB"),
    KONVEARK("KONVEARK", "Konvertert fra elektronisk arkiv", "KD"),
    KONVSYS("KONVSYS", "Konverterte data fra system", "KS"),
    PUBEOS("PUBEOS", "Publikumsblankett EØS", "PUBL_BLANKETT_EOS"),
    SEDOK("SEDOK", "Strukturert elektronisk dokument - EU/EØS", "SED"),
    TSKJ("TSKJ", "Tolkbart skjema", "TS"),
    VBRV("VBRV", "Vedtaksbrev", "VB"),
    ;

    public static final String KODEVERK = "DOKUMENT_KATEGORI";

    private static final Map<String, DokumentKategori> KODER = new LinkedHashMap<>();

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private String offisiellKode;

    private String kode;

    private DokumentKategori(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    @JsonCreator
    public static DokumentKategori fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent DokumentKategori: " + kode);
        }
        return ad;
    }

    public static Map<String, DokumentKategori> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet().stream().map(k -> "'" + k + "'").collect(Collectors.toList()));
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
        return offisiellKode;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<DokumentKategori, String> {
        @Override
        public String convertToDatabaseColumn(DokumentKategori attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public DokumentKategori convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    public static DokumentKategori finnForKodeverkEiersKode(String offisiellDokumentType) {
        return List.of(values()).stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }
    
}
