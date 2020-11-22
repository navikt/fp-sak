package no.nav.foreldrepenger.behandlingslager.uttak.svp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.behandling.ÅrsakskodeMedLovreferanse;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum PeriodeIkkeOppfyltÅrsak implements Kodeverdi, ÅrsakskodeMedLovreferanse {

    INGEN("-", "Ikke definert", null),

    _4072("4072", "§14-9 sjuende ledd: Barnet er dødt", "{\"fagsakYtelseType\": {\"SVP\": {\"lovreferanse\": \"14-9\"}}}"),
    _4071("4071", "§14-10: Bruker er død", "{\"fagsakYtelseType\": {\"fagsakYtelseType\": SVP\": {\"lovreferanse\": \"14-10\"}}}"),
    _4087("4087", "§14-2: Opphør medlemskap", "{\"fagsakYtelseType\": {\"fagsakYtelseType\": SVP\": {\"lovreferanse\": \"14-2\"}}}"),
    _4098("4098", "§14-5: Foreldreansvarsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"SVP\": {\"lovreferanse\": \"14-5\"}}}"),
    _4099("4099", "§14-6: Opptjeningsvilkåret er ikke oppfylt", "{\"fagsakYtelseType\": {\"SVP\": {\"lovreferanse\": \"14-6\"}}}"),
    _8304("8304", "Bruker er død", null),
    _8305("8305", "Barnet er dødt", null),
    _8306("8306", "Bruker er ikke medlem", null),
    _8308_SØKT_FOR_SENT("8308", "Søkt for sent", null),
    _8309("8309", "Perioden er ikke før fødsel", null),
    _8310("8310", "Perioden må slutte senest tre uker før termin", null),
    _8311("8311", "Perioden er samtidig som en ferie", null),
    _8313("8313", "Perioden er etter et opphold i ytelsen", null),

    ;

    private static final Map<String, PeriodeIkkeOppfyltÅrsak> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "SVP_PERIODE_IKKE_OPPFYLT_AARSAK";

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
    private String lovHjemmel;

    private PeriodeIkkeOppfyltÅrsak(String kode) {
        this.kode = kode;
    }

    private PeriodeIkkeOppfyltÅrsak(String kode, String navn, String lovHjemmel) {
        this.kode = kode;
        this.navn = navn;
        this.lovHjemmel = lovHjemmel;
    }

    @JsonCreator
    public static PeriodeIkkeOppfyltÅrsak fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent PeriodeIkkeOppfyltÅrsak: " + kode);
        }
        return ad;
    }

    public static Map<String, PeriodeIkkeOppfyltÅrsak> kodeMap() {
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
    public String getLovHjemmelData() {
        return lovHjemmel;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<PeriodeIkkeOppfyltÅrsak, String> {
        @Override
        public String convertToDatabaseColumn(PeriodeIkkeOppfyltÅrsak attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public PeriodeIkkeOppfyltÅrsak convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    public static Set<PeriodeIkkeOppfyltÅrsak> opphørsAvslagÅrsaker() {
        return new HashSet<>(Arrays.asList(
            _8304,
            _8305,
            _8306,
            _8309));
    }
}
