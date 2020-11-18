package no.nav.foreldrepenger.behandlingslager.uttak.fp;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;
import no.nav.vedtak.konfig.Tid;

@JsonSerialize(using=GraderingAvslagÅrsakSerializer.class)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum GraderingAvslagÅrsak implements Kodeverdi {

    UKJENT("-", "Ikke definert", null),
    GRADERING_FØR_UKE_7("4504", "§14-16 andre ledd: Avslag gradering - gradering før uke 7", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16\"}}}"),
    FOR_SEN_SØKNAD("4501", "§14-16: Ikke gradering pga. for sen søknad", "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16\"}}}"),
    MANGLENDE_GRADERINGSAVTALE("4502", "§14-16 femte ledd, jf §21-3: Avslag graderingsavtale mangler - ikke dokumentert",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16,21-3\"}}}"),
    MOR_OPPFYLLER_IKKE_AKTIVITETSKRAV("4503", "§14-16 fjerde ledd: Avslag gradering – ikke rett til gradert uttak pga. redusert oppfylt aktivitetskrav på mor",
            "{\"fagsakYtelseType\": {\"FP\": {\"lovreferanse\": \"14-16\"}}}"),
    AVSLAG_PGA_100_PROSENT_ARBEID("4523", "Avslag gradering - arbeid 100% eller mer", null),
    ;

    private static final Map<String, GraderingAvslagÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "GRADERING_AVSLAG_AARSAK";

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
    @JsonIgnore
    private String lovHjemmelData;

    private GraderingAvslagÅrsak(String kode) {
        this.kode = kode;
    }

    private GraderingAvslagÅrsak(String kode, String navn, String lovHjemmel) {
        this.kode = kode;
        this.navn = navn;
        this.lovHjemmelData = lovHjemmel;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static GraderingAvslagÅrsak fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        String kode = TempAvledeKode.getVerdi(GraderingAvslagÅrsak.class, node, "kode");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent GraderingAvslagÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, GraderingAvslagÅrsak> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public String getLovHjemmelData() {
        return lovHjemmelData;
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

    public LocalDate getGyldigFraOgMed() {
        return LocalDate.of(2001, 01, 01);
    }

    public LocalDate getGyldigTilOgMed() {
        return Tid.TIDENES_ENDE;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<GraderingAvslagÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(GraderingAvslagÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public GraderingAvslagÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    /**
     * Enkel serialisering av KodeverkTabell klass GraderingAvslagÅrsak, uten at disse trenger @JsonIgnore eller lignende. Deserialisering går
     * av seg selv normalt (får null for andre felter).
     */
    public static class GraderingAvslagÅrsakSerializer extends StdSerializer<GraderingAvslagÅrsak> {

        public GraderingAvslagÅrsakSerializer() {
            super(GraderingAvslagÅrsak.class);
        }

        @Override
        public void serialize(GraderingAvslagÅrsak value, JsonGenerator jgen, SerializerProvider provider) throws IOException {

            jgen.writeStartObject();

            jgen.writeStringField("kode", value.getKode());
            jgen.writeStringField("navn", value.getNavn());
            jgen.writeStringField("kodeverk", value.getKodeverk());
            jgen.writeStringField("gyldigFom", value.getGyldigFraOgMed().toString());
            jgen.writeStringField("gyldigTom", value.getGyldigTilOgMed().toString());

            jgen.writeEndObject();
        }

    }

}
