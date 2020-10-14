package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public enum ForenkletPersonstatusType implements Kodeverdi {

    BOSATT("bosattEtterFolkeregisterloven", "Bosatt Etter Folkeregisterloven", true),
    AVDØD("doedIFolkeregisteret", "Død i Folkeregisteret", true),
    IKKE_BOSATT("ikkeBosatt", "Ikke Bosatt", true),  // TODO(jol) Avklar - Mulig false (utva - true, resten false).
    DNUMMER("dNummer", "D-Nummer", false),
    OPPHØRT("opphoert", "Opphørt", false),
    FORSVUNNET("forsvunnet", "Forsvunnet", false),

    UDEFINERT("-", "Ikke definert", false),
    ;

    private static final Map<PersonstatusType, ForenkletPersonstatusType> FRA_TPS = Map.ofEntries(
        Map.entry(PersonstatusType.ABNR, UDEFINERT),
        Map.entry(PersonstatusType.ADNR, DNUMMER),
        Map.entry(PersonstatusType.BOSA, BOSATT),
        Map.entry(PersonstatusType.DØD, AVDØD),
        Map.entry(PersonstatusType.DØDD, AVDØD),
        Map.entry(PersonstatusType.FOSV, FORSVUNNET),
        Map.entry(PersonstatusType.FØDR, IKKE_BOSATT),
        Map.entry(PersonstatusType.UFUL, UDEFINERT),
        Map.entry(PersonstatusType.UREG, IKKE_BOSATT),
        Map.entry(PersonstatusType.UTAN, IKKE_BOSATT),
        Map.entry(PersonstatusType.UTPE, OPPHØRT),
        Map.entry(PersonstatusType.UTVA, IKKE_BOSATT)
    );

    private static final Map<String, ForenkletPersonstatusType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "PERSONSTATUS_TYPE";

    @JsonIgnore
    private String navn;

    private String kode;

    @JsonIgnore
    private boolean fortsettBehandling;

    private ForenkletPersonstatusType(String kode, String navn, boolean fortsettBehandling) {
        this.kode = kode;
        this.navn = navn;
        this.fortsettBehandling = fortsettBehandling;
    }

    @JsonCreator
    public static ForenkletPersonstatusType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent PersonstatusType: " + kode);
        }
        return ad;
    }

    public static ForenkletPersonstatusType fraPersonstatusType(PersonstatusType type) {
        return FRA_TPS.getOrDefault(type, UDEFINERT);
    }

    public static Map<String, ForenkletPersonstatusType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
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
    public static class KodeverdiConverter implements AttributeConverter<ForenkletPersonstatusType, String> {
        @Override
        public String convertToDatabaseColumn(ForenkletPersonstatusType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public ForenkletPersonstatusType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    public static Set<ForenkletPersonstatusType> personstatusTyperFortsattBehandling() {
        return List.of(values()).stream().filter(s -> s.fortsettBehandling).collect(Collectors.toSet());
    }

}
