package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


public enum MorsAktivitet implements Kodeverdi {

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke satt eller valgt kode"),
    ARBEID("ARBEID", "Er i arbeid"),
    UTDANNING("UTDANNING", "Tar utdanning på heltid"),
    KVALPROG("KVALPROG", "Deltar i kvalifiseringsprogrammet"),
    INTROPROG("INTROPROG", "Deltar i introduksjonsprogram for nykomne innvandrere"),
    TRENGER_HJELP("TRENGER_HJELP", "Er avhengig av hjelp til å ta seg av barnet"),
    INNLAGT("INNLAGT", "Er innlagt på institusjon"),
    ARBEID_OG_UTDANNING("ARBEID_OG_UTDANNING", "Er i arbeid og utdanning"),
    UFØRE("UFØRE", "Mor mottar uføretrygd"),
    IKKE_OPPGITT("IKKE_OPPGITT", "Periode uten oppgitt aktivitetskrav"),
    ;
    private static final Map<String, MorsAktivitet> KODER = new LinkedHashMap<>();

    private static final Set<String> LEGACY_KODER = Set.of("SAMTIDIGUTTAK");

    public static final String KODEVERK = "MORS_AKTIVITET";

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

    MorsAktivitet(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static MorsAktivitet fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            if (LEGACY_KODER.contains(kode)) {
                return IKKE_OPPGITT;
            }
            throw new IllegalArgumentException("Ukjent MorsAktivitet: " + kode);
        }
        return ad;
    }
    public static Map<String, MorsAktivitet> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static boolean forventerDokumentasjon(MorsAktivitet aktivitet) {
        return aktivitet != null && !Set.of(UDEFINERT, UFØRE, IKKE_OPPGITT).contains(aktivitet);
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
    public static class KodeverdiConverter implements AttributeConverter<MorsAktivitet, String> {
        @Override
        public String convertToDatabaseColumn(MorsAktivitet attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public MorsAktivitet convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
