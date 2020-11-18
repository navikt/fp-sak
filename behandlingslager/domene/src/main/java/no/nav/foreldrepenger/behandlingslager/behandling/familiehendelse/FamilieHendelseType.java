package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

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
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum FamilieHendelseType implements Kodeverdi {

    ADOPSJON("ADPSJN", "Adopsjon"),
    OMSORG("OMSRGO", "Omsorgoverdragelse"),
    FØDSEL("FODSL", "Fødsel"),
    TERMIN("TERM", "Termin"),
    UDEFINERT("-", "Ikke satt eller valgt kode"),

    ;

    private static final Map<String, FamilieHendelseType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "FAMILIE_HENDELSE_TYPE";

    @JsonIgnore
    private String navn;

    private String kode;

    private FamilieHendelseType(String kode) {
        this.kode = kode;
    }

    private FamilieHendelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static FamilieHendelseType fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(FamilieHendelseType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FamilieHendelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, FamilieHendelseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static boolean gjelderFødsel(FamilieHendelseType type) {
        return TERMIN.equals(type) || FØDSEL.equals(type);
    }

    public static boolean gjelderAdopsjon(FamilieHendelseType type) {
        return ADOPSJON.equals(type) || OMSORG.equals(type);
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
    public static class KodeverdiConverter implements AttributeConverter<FamilieHendelseType, String> {
        @Override
        public String convertToDatabaseColumn(FamilieHendelseType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public FamilieHendelseType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
