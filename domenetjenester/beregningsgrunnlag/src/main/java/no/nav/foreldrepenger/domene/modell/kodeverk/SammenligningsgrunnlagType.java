package no.nav.foreldrepenger.domene.modell.kodeverk;

/**
 * <h3>Internt kodeverk</h3>
 * Definerer status/type av {@link SammenligningsgrunnlagPrStatus}
 * <p>
 *
 */

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum SammenligningsgrunnlagType implements Kodeverdi {

    SAMMENLIGNING_AT("SAMMENLIGNING_AT", "Sammenligningsgrunnlag arbeidstaker"),
    SAMMENLIGNING_FL("SAMMENLIGNING_FL", "Sammenligningsgrunnlag frilans"),
    SAMMENLIGNING_AT_FL("SAMMENLIGNING_AT_FL", "Sammenligningsgrunnlag arbeidstaker og frilans samlet"),
    SAMMENLIGNING_SN("SAMMENLIGNING_SN", "Sammenligningsgrunnlag næring"),
    SAMMENLIGNING_ATFL_SN("SAMMENLIGNING_ATFL_SN", "Sammenligningsgrunnlag for Arbeidstaker, frilans og selvstendig næringsdrivende"),
    ;

    private static final Map<String, SammenligningsgrunnlagType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;

    @JsonValue
    private final String kode;

    SammenligningsgrunnlagType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static SammenligningsgrunnlagType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent SammenligningsgrunnlagType: " + kode);
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
