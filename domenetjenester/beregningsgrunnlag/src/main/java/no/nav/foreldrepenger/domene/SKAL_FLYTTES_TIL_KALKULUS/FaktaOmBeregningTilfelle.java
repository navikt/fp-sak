package no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum FaktaOmBeregningTilfelle implements Kodeverdi {

    VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD("VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD", "Vurder tidsbegrenset arbeidsforhold"),
    VURDER_SN_NY_I_ARBEIDSLIVET("VURDER_SN_NY_I_ARBEIDSLIVET", "Vurder om søker er SN og ny i arbeidslivet"),
    VURDER_NYOPPSTARTET_FL("VURDER_NYOPPSTARTET_FL", "Vurder nyoppstartet frilans"),
    FASTSETT_MAANEDSINNTEKT_FL("FASTSETT_MAANEDSINNTEKT_FL", "Fastsett månedsinntekt frilans"),
    FASTSETT_BG_ARBEIDSTAKER_UTEN_INNTEKTSMELDING("FASTSETT_BG_ARBEIDSTAKER_UTEN_INNTEKTSMELDING", "Fastsette beregningsgrunnlag for arbeidstaker uten inntektsmelding"),
    VURDER_LØNNSENDRING("VURDER_LØNNSENDRING", "Vurder lønnsendring"),
    FASTSETT_MÅNEDSLØNN_ARBEIDSTAKER_UTEN_INNTEKTSMELDING("FASTSETT_MÅNEDSLØNN_ARBEIDSTAKER_UTEN_INNTEKTSMELDING", "Fastsett månedslønn arbeidstaker uten inntektsmelding"),
    VURDER_AT_OG_FL_I_SAMME_ORGANISASJON("VURDER_AT_OG_FL_I_SAMME_ORGANISASJON", "Vurder om bruker er arbeidstaker og frilanser i samme organisasjon"),
    FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE("FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE", "Fastsett besteberegning fødende kvinne"),
    VURDER_ETTERLØNN_SLUTTPAKKE("VURDER_ETTERLØNN_SLUTTPAKKE", "Vurder om søker har etterlønn og/eller sluttpakke"),
    FASTSETT_ETTERLØNN_SLUTTPAKKE("FASTSETT_ETTERLØNN_SLUTTPAKKE", "Fastsett søkers beregningsgrunnlag for etterlønn og/eller sluttpakke andel"),
    VURDER_MOTTAR_YTELSE("VURDER_MOTTAR_YTELSE", "Vurder om søker mottar ytelse for aktivitet."),
    VURDER_BESTEBEREGNING("VURDER_BESTEBEREGNING", "Vurder om søker skal ha besteberegning"),
    VURDER_MILITÆR_SIVILTJENESTE("VURDER_MILITÆR_SIVILTJENESTE", "Vurder om søker har hatt militær- eller siviltjeneste i opptjeningsperioden."),
    VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT("VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT", "Vurder refusjonskrav fremsatt for sent skal være med i beregning."),
    FASTSETT_BG_KUN_YTELSE("FASTSETT_BG_KUN_YTELSE", "Fastsett beregningsgrunnlag for kun ytelse uten arbeidsforhold"),
    TILSTØTENDE_YTELSE("TILSTØTENDE_YTELSE", "Avklar beregningsgrunnlag og inntektskategori for tilstøtende ytelse"),
    FASTSETT_ENDRET_BEREGNINGSGRUNNLAG("FASTSETT_ENDRET_BEREGNINGSGRUNNLAG", "Fastsette endring i beregningsgrunnlag"),
    UDEFINERT("-", "Ikke definert"),
    ;
    public static final String KODEVERK = "FAKTA_OM_BEREGNING_TILFELLE";

    private static final Map<String, FaktaOmBeregningTilfelle> KODER = new LinkedHashMap<>();

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

    FaktaOmBeregningTilfelle(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @JsonCreator
    public static FaktaOmBeregningTilfelle fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent FaktaOmBeregningTilfelle: " + kode);
        }
        return ad;
    }

    public static Map<String, FaktaOmBeregningTilfelle> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static void main(String[] args) {
        System.out.println(KODER.keySet().stream().map(k -> "'" + k + "'").collect(Collectors.toList()));
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<FaktaOmBeregningTilfelle, String> {

        @Override
        public String convertToDatabaseColumn(FaktaOmBeregningTilfelle attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public FaktaOmBeregningTilfelle convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
