package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Collections;
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

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.TempAvledeKode;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum BehandlingType implements Kodeverdi, MedOffisiellKode {

    /**
     * Konstanter for å skrive ned kodeverdi. For å hente ut andre data konfigurert, må disse leses fra databasen (eks.
     * for å hente offisiell kode for et Nav kodeverk).
     */
    FØRSTEGANGSSØKNAD("BT-002", "Førstegangsbehandling", "ae0034", 6, true),
    KLAGE("BT-003", "Klage", "ae0058", 10, false),
    REVURDERING("BT-004", "Revurdering", "ae0028", 6, false),
    ANKE("BT-008", "Anke", "ae0046", 0, false),
    INNSYN("BT-006", "Dokumentinnsyn", "ae0042", 1, false),

    /** @deprecated fjern herfra. */
    TILBAKEBETALING_ENDRING("BT-005", "Tilbakebetaling endring", "ae0043", 6, false),
    /** @deprecated fjern herfra. */
    TILBAKEKREVING("BT-007", "Tilbakekreving", null, 0, false),
    /** @deprecated fjern herfra. */
    REVURDERING_TILBAKEKREVING("BT-009", "Revurdering tilbakekreving", null, 0, false),

    UDEFINERT("-", "Ikke definert", null, 0, false),
    ;

    private static final Set<BehandlingType> YTELSE_BEHANDLING_TYPER = Set.of(FØRSTEGANGSSØKNAD, REVURDERING);
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

    @JsonIgnore
    private int behandlingstidFristUker;
    @JsonIgnore
    private Boolean behandlingstidVarselbrev;

    @JsonIgnore
    private String navn;

    @JsonIgnore
    private String offisiellKode;

    private String kode;

    private BehandlingType(String kode) {
        this.kode = kode;
    }

    private BehandlingType(String kode, String navn, String offisiellKode, int behandlingstidFristUker, Boolean behandlingstidVarselbrev) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
        this.behandlingstidFristUker = behandlingstidFristUker;
        this.behandlingstidVarselbrev = behandlingstidVarselbrev;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BehandlingType fraKode(@JsonProperty(value = "kode") Object node) {
        if (node == null) {
            return null;
        }
        var kode = TempAvledeKode.getVerdi(BehandlingType.class, node, "kode");
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

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @JsonProperty
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

    public int getBehandlingstidFristUker() {
        return behandlingstidFristUker;
    }

    public boolean isBehandlingstidVarselbrev() {
        return behandlingstidVarselbrev;
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
