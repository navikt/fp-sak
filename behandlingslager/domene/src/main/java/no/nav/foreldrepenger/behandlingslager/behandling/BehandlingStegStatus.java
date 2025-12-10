package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

/**
 * Kodefor status i intern håndtering av flyt på et steg
 * <p>
 * Kommer kun til anvendelse dersom det oppstår aksjonspunkter eller noe må legges på vent i et steg. Hvis ikke
 * flyter et rett igjennom til UTFØRT.
 */
public enum BehandlingStegStatus implements Kodeverdi {

    /** midlertidig intern tilstand når steget startes (etter inngang). */
    STARTET("STARTET", "Steget er startet"),
    INNGANG("INNGANG", "Inngangkriterier er ikke oppfylt"),
    UTGANG("UTGANG", "Utgangskriterier er ikke oppfylt"),
    VENTER("VENTER", "På vent"),
    AVBRUTT("AVBRUTT", "Avbrutt"),
    UTFØRT("UTFØRT", "Utført"),
    FREMOVERFØRT("FREMOVERFØRT", "Fremoverført"),
    TILBAKEFØRT("TILBAKEFØRT", "Tilbakeført"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Ikke definert"),

    ;
    private static final Set<BehandlingStegStatus> KAN_UTFØRE_STEG = new HashSet<>(Arrays.asList(STARTET, VENTER));
    private static final Set<BehandlingStegStatus> KAN_FORTSETTE_NESTE = new HashSet<>(Arrays.asList(UTFØRT, FREMOVERFØRT));
    private static final Set<BehandlingStegStatus> SLUTT_STATUSER = new HashSet<>(Arrays.asList(AVBRUTT, UTFØRT, TILBAKEFØRT));

    private static final Map<String, BehandlingStegStatus> KODER = new LinkedHashMap<>();

    private final String navn;
    @JsonValue
    private final String kode;

    BehandlingStegStatus(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public boolean kanUtføreSteg() {
        return KAN_UTFØRE_STEG.contains(this);
    }

    public boolean kanFortsetteTilNeste() {
        return KAN_FORTSETTE_NESTE.contains(this);
    }

    public static boolean erSluttStatus(BehandlingStegStatus status) {
        return SLUTT_STATUSER.contains(status);
    }

    public boolean erVedInngang() {
       return Objects.equals(INNGANG, this);
    }

    public static boolean erVedUtgang(BehandlingStegStatus stegStatus) {
        return Objects.equals(UTGANG, stegStatus);
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
    public static class KodeverdiConverter implements AttributeConverter<BehandlingStegStatus, String> {
        @Override
        public String convertToDatabaseColumn(BehandlingStegStatus attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BehandlingStegStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static BehandlingStegStatus fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent BehandlingStegStatus: " + kode);
            }
            return ad;
        }
    }

}
