package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
public enum RevurderingVarslingÅrsak implements Kodeverdi {

    BARN_IKKE_REGISTRERT_FOLKEREGISTER("BARNIKKEREG", "Barn er ikke registrert i folkeregisteret"),
    ARBEIDS_I_STØNADSPERIODEN("JOBBFULLTID", "Arbeid i stønadsperioden"),
    BEREGNINGSGRUNNLAG_UNDER_HALV_G("IKKEOPPTJENT", "Beregningsgrunnlaget er under 1/2 G"),
    BRUKER_REGISTRERT_UTVANDRET("UTVANDRET", "Bruker er registrert utvandret"),
    ARBEID_I_UTLANDET("JOBBUTLAND", "Arbeid i utlandet"),
    IKKE_LOVLIG_OPPHOLD("IKKEOPPHOLD", "Ikke lovlig opphold"),
    OPPTJENING_IKKE_OPPFYLT("JOBB6MND", "Opptjeningsvilkår ikke oppfylt"),
    MOR_AKTIVITET_IKKE_OPPFYLT("AKTIVITET", "Mors aktivitetskrav er ikke oppfylt"),
    ANNET("ANNET", "Annet"),
    ;

    private static final Map<String, RevurderingVarslingÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "REVURDERING_VARSLING_AARSAK";

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

    private RevurderingVarslingÅrsak(String kode) {
        this.kode = kode;
    }

    private RevurderingVarslingÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static RevurderingVarslingÅrsak fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent RevurderingVarslingÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, RevurderingVarslingÅrsak> kodeMap() {
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
        System.out.println(KODER.keySet().stream().map(k -> "\"" + k + "\"").collect(Collectors.toList()));
    }

}
