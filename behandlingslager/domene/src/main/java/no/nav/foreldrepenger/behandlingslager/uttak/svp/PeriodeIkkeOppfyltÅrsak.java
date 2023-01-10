package no.nav.foreldrepenger.behandlingslager.uttak.svp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.ÅrsakskodeMedLovreferanse;

public enum PeriodeIkkeOppfyltÅrsak implements Kodeverdi, ÅrsakskodeMedLovreferanse {

    INGEN("-", "Ikke definert", null),

    // Se også Arbeidsforhold ikke oppfylt - der brukes 8301-8303 + 8312
    _8304("8304", "Bruker er død", null),
    _8305("8305", "Barnet er dødt", null),
    _8306("8306", "Bruker er ikke medlem", null),
    _8308_SØKT_FOR_SENT("8308", "Søkt for sent", null),
    _8309("8309", "Perioden er ikke før fødsel", null),
    _8310("8310", "Perioden må slutte senest tre uker før termin", null),
    _8311("8311", "Perioden er samtidig som en ferie", null),
    _8313("8313", "Perioden er etter et opphold i ytelsen", null),
    _8314("8314", "Perioden er etter startdato foreldrepenger", null),
    SVANGERSKAPSVILKÅRET_IKKE_OPPFYLT("8315", "Svangerskapsvilkåret er ikke oppfylt", null),
    OPPTJENINGSVILKÅRET_IKKE_OPPFYLT("8316", "Opptjeningsvilkåret er ikke oppfylt", null)
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

    private String navn;
    @JsonValue
    private String kode;

    private String lovHjemmel;

    PeriodeIkkeOppfyltÅrsak(String kode) {
        this.kode = kode;
    }

    PeriodeIkkeOppfyltÅrsak(String kode, String navn, String lovHjemmel) {
        this.kode = kode;
        this.navn = navn;
        this.lovHjemmel = lovHjemmel;
    }

    public static PeriodeIkkeOppfyltÅrsak fraKode(String kode) {
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

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

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
        return Set.of(_8304, _8305, _8306, _8309, _8314);
    }
}
