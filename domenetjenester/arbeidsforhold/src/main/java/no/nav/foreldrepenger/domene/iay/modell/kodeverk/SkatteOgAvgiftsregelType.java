package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum SkatteOgAvgiftsregelType implements Kodeverdi {

    SÆRSKILT_FRADRAG_FOR_SJØFOLK("SÆRSKILT_FRADRAG_FOR_SJØFOLK", "Særskilt fradrag for sjøfolk"),
    SVALBARD("SVALBARD", "Svalbardinntekt"),
    SKATTEFRI_ORGANISASJON("SKATTEFRI_ORGANISASJON", "Skattefri Organisasjon"),
    NETTOLØNN_FOR_SJØFOLK("NETTOLØNN_FOR_SJØFOLK", "Nettolønn for sjøfolk"),
    NETTOLØNN("NETTOLØNN", "Nettolønn"),
    KILDESKATT_PÅ_PENSJONER("KILDESKATT_PÅ_PENSJONER", "Kildeskatt på pensjoner"),
    JAN_MAYEN_OG_BILANDENE("JAN_MAYEN_OG_BILANDENE", "Inntekt på Jan Mayen og i norske biland i Antarktis"),

    UDEFINERT(STANDARDKODE_UDEFINERT, "Udefinert"),
    ;

    private static final Map<String, SkatteOgAvgiftsregelType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private final String navn;
    @JsonValue
    private final String kode;

    SkatteOgAvgiftsregelType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static SkatteOgAvgiftsregelType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent SkatteOgAvgiftsregelType: " + kode);
        }
        return ad;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

}
