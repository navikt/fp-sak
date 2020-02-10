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
public enum FagsakYtelseType implements Kodeverdi {

    ENGANGSTØNAD("ES", "Engangsstønad"),
    FORELDREPENGER("FP", "Foreldrepenger"),
    SVANGERSKAPSPENGER("SVP", "Svangerskapspenger"),
    UDEFINERT("-", "Ikke definert"),
    ;

    public static final String KODEVERK = "FAGSAK_YTELSE"; //$NON-NLS-1$

    private static final Map<String, FagsakYtelseType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    public enum YtelseType {
        ES, FP, SVP;
    }

    @JsonIgnore
    private String navn;

    private String kode;

    private FagsakYtelseType(String kode) {
        this.kode = kode;
    }

    private FagsakYtelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static FagsakYtelseType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FagsakYtelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, FagsakYtelseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }
    
    @Override
    public String getOffisiellKode() {
        return getKode();
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    /**
     * @deprecated Ikke switch på dette i koden. Marker heller klasse og pakke for angitt ytelse (eks. behandlingssteg, aksjonspuntutleder,
     *             kompletthetsjekk).
     *             Til nød bruk en negativ guard
     *             <code>if(!ENGANGSSTØNAD.getKode().equals(this.getKode())) throw IllegalStateException("No trespassing in this code"); </code>
     */
    @Deprecated
    public final boolean gjelderEngangsstønad() {
        return ENGANGSTØNAD.getKode().equals(this.getKode());
    }

    /**
     * @deprecated Ikke switch på dette i koden. Marker heller klasse og pakke for angitt ytelse (eks. behandlingssteg, aksjonspuntutleder,
     *             kompletthetsjekk)
     *             Til nød bruk en negativ guard
     *             <code>if(!FORELDREPENGER.getKode().equals(this.getKode())) throw IllegalStateException("No trespassing in this code"); </code>
     */
    @Deprecated
    public final boolean gjelderForeldrepenger() {
        return FORELDREPENGER.getKode().equals(this.getKode());
    }

    /**
     * @deprecated Ikke switch på dette i koden. Marker heller klasse og pakke for angitt ytelse (eks. behandlingssteg, aksjonspuntutleder,
     *             kompletthetsjekk)
     *             Til nød bruk en negativ guard
     *             <code>if(!SVANGERSKAPSPENGER.getKode().equals(this.getKode())) throw IllegalStateException("No trespassing in this code"); </code>
     */
    @Deprecated
    public final boolean gjelderSvangerskapspenger() {
        return SVANGERSKAPSPENGER.getKode().equals(this.getKode());
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<FagsakYtelseType, String> {
        @Override
        public String convertToDatabaseColumn(FagsakYtelseType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public FagsakYtelseType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

}
