package no.nav.foreldrepenger.domene.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;


@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum AndelKilde implements Kodeverdi {

    SAKSBEHANDLER_KOFAKBER("SAKSBEHANDLER_KOFAKBER", "Saksbehandler kontroller fakta"),
    SAKSBEHANDLER_FORDELING("SAKSBEHANDLER_FORDELING", "Saksbehandler fordeling"),
    PROSESS_PERIODISERING("PROSESS_PERIODISERING", "Prosess for periodisering"),
    PROSESS_OMFORDELING("PROSESS_OMFORDELING", "Saksbehandler for omfordeling"),
    PROSESS_START("PROSESS_START", "Start")
    ;
    public static final String KODEVERK = "ANDEL_KILDE";
    private static final Map<String, AndelKilde> KODER = new LinkedHashMap<>();

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

    AndelKilde(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static AndelKilde fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(AndelKilde.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Andelkilde: " + kode);
        }
        return ad;
    }

    public static Map<String, AndelKilde> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty(value = "kodeverk", access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty(value="kode")
    @Override
    public String getKode() {
        return kode;
    }

}
