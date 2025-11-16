package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum MedlemskapManuellVurderingType implements Kodeverdi {

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),
    MEDLEM("MEDLEM", "Periode med medlemskap"),
    UNNTAK("UNNTAK", "Periode med unntak fra medlemskap"),
    IKKE_RELEVANT("IKKE_RELEVANT", "Ikke relevant periode"),
    SAKSBEHANDLER_SETTER_OPPHØR_AV_MEDL_PGA_ENDRINGER_I_TPS("OPPHOR_PGA_ENDRING_I_TPS", "Opphør av medlemskap på grunn av endringer i tps"),

    ;

    private static final Set<MedlemskapManuellVurderingType> GUI = Set.of(MEDLEM, UNNTAK, IKKE_RELEVANT);

    private static final Map<String, MedlemskapManuellVurderingType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "MEDLEMSKAP_MANUELL_VURD";

    @JsonIgnore
    private final String navn;

    @JsonValue
    private final String kode;

    MedlemskapManuellVurderingType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static MedlemskapManuellVurderingType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent MedlemskapManuellVurderingType: " + kode);
        }
        return ad;
    }

    public static Map<String, MedlemskapManuellVurderingType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public boolean visesPåKlient() {
        return GUI.contains(this);
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<MedlemskapManuellVurderingType, String> {
        @Override
        public String convertToDatabaseColumn(MedlemskapManuellVurderingType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public MedlemskapManuellVurderingType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
