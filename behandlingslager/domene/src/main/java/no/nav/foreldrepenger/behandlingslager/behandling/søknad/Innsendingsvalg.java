package no.nav.foreldrepenger.behandlingslager.behandling.søknad;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum Innsendingsvalg implements Kodeverdi {

    LASTET_OPP("LASTET_OPP", "Lastet opp"),
    SEND_SENERE("SEND_SENERE", "Send senere"),
    SENDES_IKKE("SENDES_IKKE", "Sendes ikke"),
    VEDLEGG_SENDES_AV_ANDRE("VEDLEGG_SENDES_AV_ANDRE", "Vedlegg sendes av andre"),
    IKKE_VALGT("IKKE_VALGT", "Ikke valgt"),
    VEDLEGG_ALLEREDE_SENDT("VEDLEGG_ALLEREDE_SENDT", "Vedlegg aallerede sendt"),
    ;

    private static final Map<String, Innsendingsvalg> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;
    @JsonValue
    private final String kode;

    Innsendingsvalg(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Innsendingsvalg fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Innsendingsvalg: " + kode);
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
    public static class KodeverdiConverter implements AttributeConverter<Innsendingsvalg, String> {
        @Override
        public String convertToDatabaseColumn(Innsendingsvalg attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public Innsendingsvalg convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
