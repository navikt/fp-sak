package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum BehandlingType implements Kodeverdi, MedOffisiellKode {

    /**
     * Konstanter for å skrive ned kodeverdi. For å hente ut andre data konfigurert, må disse leses fra databasen (eks.
     * for å hente offisiell kode for et Nav kodeverk).
     */
    FØRSTEGANGSSØKNAD("BT-002", "Førstegangsbehandling", "ae0034", 6),
    KLAGE("BT-003", "Klage", "ae0058", 10),
    REVURDERING("BT-004", "Revurdering", "ae0028", 6),
    ANKE("BT-008", "Anke", "ae0046", 0),
    INNSYN("BT-006", "Dokumentinnsyn", "ae0042", 1),

    /** @deprecated fjern herfra. */
    TILBAKEBETALING_ENDRING("BT-005", "Tilbakebetaling endring", "ae0043", 6),
    /** @deprecated fjern herfra. */
    TILBAKEKREVING("BT-007", "Tilbakekreving", null, 0),
    /** @deprecated fjern herfra. */
    REVURDERING_TILBAKEKREVING("BT-009", "Revurdering tilbakekreving", null, 0),

    UDEFINERT("-", "Ikke definert", null, 0),
    ;

    private static final Set<BehandlingType> YTELSE_BEHANDLING_TYPER = Set.of(FØRSTEGANGSSØKNAD, REVURDERING);
    private static final Set<BehandlingType> KLAGE_BEHANDLING_TYPER = Set.of(KLAGE, ANKE);
    private static final Set<BehandlingType> ANDRE_BEHANDLING_TYPER = Set.of(KLAGE, ANKE, INNSYN);

    public static final String KODEVERK = "BEHANDLING_TYPE";
    private static final Map<String, BehandlingType> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final int behandlingstidFristUker;

    private final String navn;

    private final String offisiellKode;

    @JsonValue
    private final String kode;

    BehandlingType(String kode,
                   String navn,
                   String offisiellKode,
                   int behandlingstidFristUker) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
        this.behandlingstidFristUker = behandlingstidFristUker;
    }

    public static BehandlingType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent BehandlingType: " + kode);
        }
        return ad;
    }

    public static Map<String, BehandlingType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
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
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    public static Set<BehandlingType> getYtelseBehandlingTyper() {
        return YTELSE_BEHANDLING_TYPER;
    }

    public static Set<BehandlingType> getAndreBehandlingTyper() {
        return ANDRE_BEHANDLING_TYPER;
    }

    public boolean erYtelseBehandlingType() {
        return YTELSE_BEHANDLING_TYPER.contains(this);
    }

    public boolean erKlageAnkeType() {
        return KLAGE_BEHANDLING_TYPER.contains(this);
    }

    public int getBehandlingstidFristUker() {
        return behandlingstidFristUker;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<BehandlingType, String> {
        @Override
        public String convertToDatabaseColumn(BehandlingType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public BehandlingType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
