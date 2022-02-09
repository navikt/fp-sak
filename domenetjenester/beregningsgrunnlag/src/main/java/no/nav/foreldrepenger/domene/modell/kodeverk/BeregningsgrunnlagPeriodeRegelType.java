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
public enum BeregningsgrunnlagPeriodeRegelType implements Kodeverdi {
    FORESLÅ("FORESLÅ", "Foreslå beregningsgrunnlag"),
    VILKÅR_VURDERING("VILKÅR_VURDERING", "Vurder beregningsvilkår"),
    FORDEL("FORDEL", "Fordel beregningsgrunnlag"),
    FASTSETT("FASTSETT", "Fastsett/fullføre beregningsgrunnlag"),
    OPPDATER_GRUNNLAG_SVP("OPPDATER_GRUNNLAG_SVP", "Oppdater grunnlag for SVP"),
    FASTSETT2("FASTSETT2", "Fastsette/fullføre beregningsgrunnlag for andre gangs kjøring for SVP"),
    FINN_GRENSEVERDI("FINN_GRENSEVERDI", "Finne grenseverdi til kjøring av fastsett beregningsgrunnlag for SVP"),
    UDEFINERT("-", "Ikke definert"),
    ;

    public static final String KODEVERK = "BG_PERIODE_REGEL_TYPE";
    private static final Map<String, BeregningsgrunnlagPeriodeRegelType> KODER = new LinkedHashMap<>();

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

    BeregningsgrunnlagPeriodeRegelType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static BeregningsgrunnlagPeriodeRegelType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(BeregningsgrunnlagPeriodeRegelType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BeregningsgrunnlagPeriodeRegelType: " + kode);
        }
        return ad;
    }

    public static Map<String, BeregningsgrunnlagPeriodeRegelType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

}
