package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
public enum InntektsFilter implements Kodeverdi {

    BEREGNINGSGRUNNLAG("BEREGNINGSGRUNNLAG", "Beregningsgrunnlag", "8-28",
            InntektsKilde.INNTEKT_BEREGNING, InntektsFormål.FORMAAL_FORELDREPENGER),
    OPPTJENINGSGRUNNLAG("OPPTJENINGSGRUNNLAG", "Pensjonsgivende inntekt", "PensjonsgivendeA-Inntekt",
            InntektsKilde.INNTEKT_OPPTJENING, InntektsFormål.FORMAAL_PGI),
    SAMMENLIGNINGSGRUNNLAG("SAMMENLIGNINGSGRUNNLAG", "Sammenligningsgrunnlag", "8-30",
            InntektsKilde.INNTEKT_SAMMENLIGNING, InntektsFormål.FORMAAL_FORELDREPENGER),
    UDEFINERT("-", "Ikke definert", null,
            InntektsKilde.UDEFINERT, InntektsFormål.UDEFINERT),
            ;

    private static final Map<InntektsFilter, InntektsFormål> INNTEKTSFILTER_TIL_INNTEKTSFORMÅL = new LinkedHashMap<>();
    private static final Map<InntektsKilde, InntektsFilter> INNTEKTSKILDE_TIL_INNTEKTSFILTER = new LinkedHashMap<>();

    private static final Map<String, InntektsFilter> KODER = new LinkedHashMap<>();

    private static final String KODEVERK = "INNTEKTS_FILTER";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }

        List.of(values()).stream()
            .forEach(v -> INNTEKTSKILDE_TIL_INNTEKTSFILTER.putIfAbsent(v.getInntektsKilde(), v));

        List.of(values()).stream()
            .forEach(v -> INNTEKTSFILTER_TIL_INNTEKTSFORMÅL.putIfAbsent(v, v.getInntektsFormål()));

    }

    @JsonIgnore
    private InntektsFormål inntektsFormål;
    @JsonIgnore
    private InntektsKilde inntektsKilde;

    private String kode;

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private String offisiellKode;

    private InntektsFilter(String kode) {
        this.kode = kode;
    }

    private InntektsFilter(String kode, String navn, String offisiellKode, InntektsKilde inntektsKilde, InntektsFormål inntektsFormål) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
        this.inntektsKilde = inntektsKilde;
        this.inntektsFormål = inntektsFormål;
    }

    public static InntektsFilter finnForKodeverkEiersKode(String offisiellDokumentType) {
        return List.of(values()).stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }

    @JsonCreator
    public static InntektsFilter fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent InntektsFilter: " + kode);
        }
        return ad;
    }

    public static Map<String, InntektsFilter> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet());
    }

    public static Map<InntektsFilter, InntektsFormål> mapInntektsFilterTilFormål() {
        return Collections.unmodifiableMap(INNTEKTSFILTER_TIL_INNTEKTSFORMÅL);
    }

    public static Map<InntektsKilde, InntektsFilter> mapInntektsKildeTilFilter() {
        return Collections.unmodifiableMap(INNTEKTSKILDE_TIL_INNTEKTSFILTER);
    }

    public InntektsFormål getInntektsFormål() {
        return inntektsFormål;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    private InntektsKilde getInntektsKilde() {
        return inntektsKilde;
    }

}