package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

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
public enum Innsendingsvalg implements Kodeverdi {

    LASTET_OPP("LASTET_OPP", "Lastet opp"),
    SEND_SENERE("SEND_SENERE", "Send senere"),
    SENDES_IKKE("SENDES_IKKE", "Sendes ikke"),
    VEDLEGG_SENDES_AV_ANDRE("VEDLEGG_SENDES_AV_ANDRE", "Vedlegg sendes av andre"),
    IKKE_VALGT("IKKE_VALGT", "Ikke valgt"),
    VEDLEGG_ALLEREDE_SENDT("VEDLEGG_ALLEREDE_SENDT", "Vedlegg aallerede sendt"),
    ;

    private static final Map<String, Innsendingsvalg> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "INNSENDINGSVALG";

    @Deprecated
    public static final String DISCRIMINATOR = "INNSENDINGSVALG";

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

    private Innsendingsvalg(String kode) {
        this.kode = kode;
    }

    private Innsendingsvalg(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static Innsendingsvalg fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Innsendingsvalg: " + kode);
        }
        return ad;
    }

    public static Map<String, Innsendingsvalg> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
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

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<Innsendingsvalg, String> {
        @Override
        public String convertToDatabaseColumn(Innsendingsvalg attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Innsendingsvalg convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
