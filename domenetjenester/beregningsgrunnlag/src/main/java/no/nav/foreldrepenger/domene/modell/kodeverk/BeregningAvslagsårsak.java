package no.nav.foreldrepenger.domene.modell.kodeverk;


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;


@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public enum BeregningAvslagsårsak implements Kodeverdi {

    SØKT_FL_INGEN_FL_INNTEKT("SØKT_FL_INGEN_FL_INNTEKT", "Søkt frilans uten frilansinntekt"),
    FOR_LAVT_BG("FOR_LAVT_BG", "For lavt beregningsgrunnlag"),
    AVKORTET_GRUNNET_ANNEN_INNTEKT("AVKORTET_GRUNNET_ANNEN_INNTEKT", "Avkortet grunnet annen inntekt"),
    UNDEFINED,;

    static final String KODEVERK = "BEREGNING_AVSLAG_ÅRSAK";

    private static final Map<String, BeregningAvslagsårsak> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String kode;

    @JsonIgnore
    private String navn;

    private BeregningAvslagsårsak() {
        // for hibernate
    }

    private BeregningAvslagsårsak(String kode, String navn) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    @JsonCreator(mode = Mode.DELEGATING)
    public static BeregningAvslagsårsak fraKode(Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(BeregningAvslagsårsak.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BeregningAvslagsårsak: " + kode);
        }
        return ad;
    }

    public static Map<String, BeregningAvslagsårsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

}
