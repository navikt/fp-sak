package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
public enum RelatertYtelseTilstand implements Kodeverdi {

    ÅPEN("ÅPEN", "Åpen"),
    LØPENDE("LØPENDE", "Løpende"),
    AVSLUTTET("AVSLUTTET", "Avsluttet"),
    IKKE_STARTET("IKKESTARTET", "Ikke startet"),
    ;

    private static final Map<String, RelatertYtelseTilstand> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "RELATERT_YTELSE_TILSTAND";

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

    private RelatertYtelseTilstand(String kode) {
        this.kode = kode;
    }

    private RelatertYtelseTilstand(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static RelatertYtelseTilstand fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent RelatertYtelseTilstand: " + kode);
        }
        return ad;
    }

    public static Map<String, RelatertYtelseTilstand> kodeMap() {
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
}
