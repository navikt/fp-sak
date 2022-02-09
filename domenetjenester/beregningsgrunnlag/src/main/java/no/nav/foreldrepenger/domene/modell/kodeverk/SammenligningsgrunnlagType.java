package no.nav.foreldrepenger.domene.modell.kodeverk;

/**
 * <h3>Internt kodeverk</h3>
 * Definerer status/type av {@link SammenligningsgrunnlagPrStatus}
 * <p>
 *
 */
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum SammenligningsgrunnlagType implements Kodeverdi {

    SAMMENLIGNING_AT("SAMMENLIGNING_AT", "Sammenligningsgrunnlag arbeidstaker"),
    SAMMENLIGNING_FL("SAMMENLIGNING_FL", "Sammenligningsgrunnlag frilans"),
    SAMMENLIGNING_SN("SAMMENLIGNING_SN", "Sammenligningsgrunnlag næring"),
    SAMMENLIGNING_ATFL_SN("SAMMENLIGNING_ATFL_SN", "Sammenligningsgrunnlag for Arbeidstaker, frilans og selvstendig næringsdrivende"),
    ;

    private static final Map<String, SammenligningsgrunnlagType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "SAMMENLIGNINGSGRUNNLAG_TYPE";

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

    private SammenligningsgrunnlagType(String kode) {
        this.kode = kode;
    }

    private SammenligningsgrunnlagType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static SammenligningsgrunnlagType fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(SammenligningsgrunnlagType.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent SammenligningsgrunnlagType: " + kode);
        }
        return ad;
    }

    public static Map<String, SammenligningsgrunnlagType> kodeMap() {
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
