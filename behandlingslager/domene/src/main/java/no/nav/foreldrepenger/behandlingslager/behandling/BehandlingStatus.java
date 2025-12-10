package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

/**
 * NB: Pass på! Ikke legg koder vilkårlig her
 * Denne definerer etablerte behandlingstatuser ihht. modell angitt av FFA (Forretning og Fag).
 */
public enum BehandlingStatus implements Kodeverdi {

    AVSLUTTET("AVSLU", "Avsluttet"),
    FATTER_VEDTAK("FVED", "Fatter vedtak"),
    IVERKSETTER_VEDTAK("IVED", "Iverksetter vedtak"),
    OPPRETTET("OPPRE", "Opprettet"),
    UTREDES("UTRED", "Behandling utredes"),

    ;

    private static final Map<String, BehandlingStatus> KODER = new LinkedHashMap<>();

    private static final Set<BehandlingStatus> FERDIGBEHANDLET_STATUS = Set.of(AVSLUTTET, IVERKSETTER_VEDTAK);

    private final String navn;
    @JsonValue
    private final String kode;

    BehandlingStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static BehandlingStatus fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingStatus: " + kode);
        }
        return ad;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public static Set<BehandlingStatus> getFerdigbehandletStatuser() {
        return FERDIGBEHANDLET_STATUS;
    }

    public boolean erFerdigbehandletStatus() {
        return FERDIGBEHANDLET_STATUS.contains(this);
    }

    @Override
    public String getKode() {
        return kode;
    }

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<BehandlingStatus, String> {
        @Override
        public String convertToDatabaseColumn(BehandlingStatus attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BehandlingStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

    }
}
