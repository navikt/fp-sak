package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum VirksomhetType implements Kodeverdi {

    DAGMAMMA("DAGMAMMA", "Dagmamma i eget hjem/familiebarnehage", Inntektskategori.DAGMAMMA),
    FISKE("FISKE", "Fiske", Inntektskategori.FISKER),
    FRILANSER("FRILANSER", "Frilanser", Inntektskategori.FRILANSER),
    JORDBRUK_SKOGBRUK("JORDBRUK_SKOGBRUK", "Jordbruk", Inntektskategori.JORDBRUKER),
    ANNEN("ANNEN", "Annen næringsvirksomhet", Inntektskategori.UDEFINERT),
    UDEFINERT("-", "Ikke definert", Inntektskategori.UDEFINERT),
    ;

    private static final Map<String, VirksomhetType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "VIRKSOMHET_TYPE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;
    private Inntektskategori inntektskategori;

    private String kode;

    VirksomhetType(String kode, String navn, Inntektskategori inntektskategori) {
        this.kode = kode;
        this.navn = navn;
        this.inntektskategori = inntektskategori;
    }

    @JsonCreator
    public static VirksomhetType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent VirksomhetType: " + kode);
        }
        return ad;
    }

    public static Map<String, VirksomhetType> kodeMap() {
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

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }
}
