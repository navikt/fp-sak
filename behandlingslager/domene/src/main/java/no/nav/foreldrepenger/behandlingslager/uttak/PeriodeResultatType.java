package no.nav.foreldrepenger.behandlingslager.uttak;

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
public enum PeriodeResultatType implements Kodeverdi {

    INNVILGET("INNVILGET", "Innvilget"),
    AVSLÅTT("AVSLÅTT", "Avslått"),
    IKKE_FASTSATT("IKKE_FASTSATT", "Ikke fastsatt"),
    MANUELL_BEHANDLING("MANUELL_BEHANDLING", "Til manuell behandling"),

    /** @deprecated kan fjernes når beregning har sluttet å bruke. */
    @Deprecated
    GYLDIG_UTSETTELSE("GYLDIG_UTSETTELSE", "Gyldig utsettelse"),

    /** @deprecated kan fjernes når beregning har sluttet å bruke. */
    @Deprecated
    UGYLDIG_UTSETTELSE("UGYLDIG_UTSETTELSE", "Ugyldig utsettelse"),
    ;

    private static final Map<String, PeriodeResultatType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "PERIODE_RESULTAT_TYPE";

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

    private PeriodeResultatType(String kode) {
        this.kode = kode;
    }

    private PeriodeResultatType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PeriodeResultatType fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(PeriodeResultatType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent PeriodeResultatType: " + kode);
        }
        return ad;
    }

    public static Map<String, PeriodeResultatType> kodeMap() {
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<PeriodeResultatType, String> {
        @Override
        public String convertToDatabaseColumn(PeriodeResultatType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public PeriodeResultatType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
