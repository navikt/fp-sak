package no.nav.foreldrepenger.behandlingslager.uttak.svp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum PeriodeIkkeOppfyltÅrsak implements Kodeverdi {

    INGEN(STANDARDKODE_UDEFINERT, "Ikke definert"),

    // Se også Arbeidsforhold ikke oppfylt - der brukes 8301-8303 + 8312
    _8304("8304", "Bruker er død"),
    _8305("8305", "Barnet er dødt"),
    _8306("8306", "Bruker er ikke medlem"),
    _8308_SØKT_FOR_SENT("8308", "Søkt for sent"),
    _8309("8309", "Perioden er ikke før fødsel"),
    _8310("8310", "Perioden må slutte senest tre uker før termin"),
    _8311("8311", "Perioden er samtidig som en ferie"),
    _8313("8313", "Perioden er etter et opphold i ytelsen"),
    _8314("8314", "Perioden er etter startdato foreldrepenger"),
    SVANGERSKAPSVILKÅRET_IKKE_OPPFYLT("8315", "Svangerskapsvilkåret er ikke oppfylt"),
    OPPTJENINGSVILKÅRET_IKKE_OPPFYLT("8316", "Opptjeningsvilkåret er ikke oppfylt"),
    PERIODEN_ER_SAMTIDIG_SOM_SYKEPENGER("8317", "Uttaksperioden er samtidig som det mottas sykepenger")
    ;

    private static final Map<String, PeriodeIkkeOppfyltÅrsak> KODER = new LinkedHashMap<>();

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

    PeriodeIkkeOppfyltÅrsak(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
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
        return Set.of(_8304, _8305, _8306, _8309, _8314, SVANGERSKAPSVILKÅRET_IKKE_OPPFYLT, OPPTJENINGSVILKÅRET_IKKE_OPPFYLT);
    }
}
