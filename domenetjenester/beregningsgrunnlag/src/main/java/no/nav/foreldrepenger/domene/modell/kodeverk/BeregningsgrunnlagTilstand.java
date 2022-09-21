package no.nav.foreldrepenger.domene.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


public enum BeregningsgrunnlagTilstand implements Kodeverdi {

    OPPRETTET("OPPRETTET", "Opprettet", true),
    FASTSATT_BEREGNINGSAKTIVITETER("FASTSATT_BEREGNINGSAKTIVITETER", "Fastsatt beregningsaktiviteter", false),
    OPPDATERT_MED_ANDELER("OPPDATERT_MED_ANDELER", "Oppdatert med andeler", true),
    KOFAKBER_UT("KOFAKBER_UT", "Kontroller fakta beregningsgrunnlag - Ut", false),
    BESTEBEREGNET("BESTEBEREGNET", "Besteberegnet", false),
    FORESLÅTT("FORESLÅTT", "Foreslått", true),
    FORESLÅTT_UT("FORESLÅTT_UT", "Foreslått ut", false),
    FORESLÅTT_2("FORESLÅTT_DEL_2", "Foreslått del 2 ut", true),
    FORESLÅTT_2_UT("FORESLÅTT_DEL_2_UT", "Foreslått del 2 ut", true),
    VURDERT_VILKÅR("VURDERT_VILKÅR", "Vurder beregning beregningsgrunnlagvilkår", true),
    VURDERT_REFUSJON("VURDERT_REFUSJON", "Vurder refusjonskrav beregning", true),
    VURDERT_REFUSJON_UT("VURDERT_REFUSJON_UT", "Vurder refusjonskrav beregning - Ut", false),
    OPPDATERT_MED_REFUSJON_OG_GRADERING("OPPDATERT_MED_REFUSJON_OG_GRADERING", "Tilstand for splittet periode med refusjon og gradering", true),
    FASTSATT_INN("FASTSATT_INN", "Fastsatt - Inn", false),
    FASTSATT("FASTSATT", "Fastsatt", true),
    UDEFINERT("-", "Ikke definert", false),
    ;
    public static final String KODEVERK = "BEREGNINGSGRUNNLAG_TILSTAND";

    private static final Map<String, BeregningsgrunnlagTilstand> KODER = new LinkedHashMap<>();

    private static final List<BeregningsgrunnlagTilstand> tilstandRekkefølge = Collections.unmodifiableList(
        List.of(
        OPPRETTET,
        FASTSATT_BEREGNINGSAKTIVITETER,
        OPPDATERT_MED_ANDELER,
        KOFAKBER_UT,
        FORESLÅTT,
        FORESLÅTT_UT,
        FORESLÅTT_2,
        FORESLÅTT_2_UT,
        VURDERT_VILKÅR,
        VURDERT_REFUSJON,
        VURDERT_REFUSJON_UT,
        OPPDATERT_MED_REFUSJON_OG_GRADERING,
        FASTSATT_INN,
        FASTSATT
    ));

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private String navn;
    @JsonValue
    private String kode;
    private boolean obligatoriskTilstand;

    BeregningsgrunnlagTilstand(String kode, String navn, boolean obligatoriskTilstand) {
        this.kode = kode;
        this.navn = navn;
        this.obligatoriskTilstand = obligatoriskTilstand;
    }

    public static BeregningsgrunnlagTilstand fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BeregningsgrunnlagTilstand: " + kode);
        }
        return ad;
    }

    public static Map<String, BeregningsgrunnlagTilstand> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public boolean erObligatoriskTilstand() {
        return this.obligatoriskTilstand;
    }

    public boolean erFør(BeregningsgrunnlagTilstand that) {
        var thisIndex = tilstandRekkefølge.indexOf(this);
        var thatIndex = tilstandRekkefølge.indexOf(that);
        return thisIndex < thatIndex;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<BeregningsgrunnlagTilstand, String> {

        @Override
        public String convertToDatabaseColumn(BeregningsgrunnlagTilstand attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BeregningsgrunnlagTilstand convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
