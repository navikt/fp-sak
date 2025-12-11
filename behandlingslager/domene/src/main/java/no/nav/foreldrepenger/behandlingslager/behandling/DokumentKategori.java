package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum DokumentKategori implements Kodeverdi, MedOffisiellKode {

    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert", null),
    KLAGE_ELLER_ANKE("KLGA", "Klage eller anke", "KA"),
    IKKE_TOLKBART_SKJEMA("ITSKJ", "Ikke tolkbart skjema", "IS"),
    SØKNAD("SOKN", "Søknad", "SOK"),
    ELEKTRONISK_SKJEMA("ESKJ", "Elektronisk skjema", "ES"),
    BRV("BRV", "Brev", "B"),
    EDIALOG("EDIALOG", "Elektronisk dialog", "ELEKTRONISK_DIALOG"),
    FNOT("FNOT", "Forvaltningsnotat", "FORVALTNINGSNOTAT"),
    IBRV("IBRV", "Informasjonsbrev", "IB"),
    KONVEARK("KONVEARK", "Konvertert fra elektronisk arkiv", "KD"),
    KONVSYS("KONVSYS", "Konverterte data fra system", "KS"),
    PUBEOS("PUBEOS", "Publikumsblankett EØS", "PUBL_BLANKETT_EOS"),
    SEDOK("SEDOK", "Strukturert elektronisk dokument - EU/EØS", "SED"),
    TSKJ("TSKJ", "Tolkbart skjema", "TS"),
    VBRV("VBRV", "Vedtaksbrev", "VB"),
    ;

    private static final Map<String, DokumentKategori> KODER = new LinkedHashMap<>();

    private String navn;

    private String offisiellKode;
    @JsonValue
    private String kode;

    DokumentKategori(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<DokumentKategori, String> {
        @Override
        public String convertToDatabaseColumn(DokumentKategori attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public DokumentKategori convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static DokumentKategori fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent DokumentKategori: " + kode);
            }
            return ad;
        }
    }

    public static DokumentKategori finnForKodeverkEiersKode(String offisiellDokumentType) {
        return Stream.of(values()).filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }

}
