package no.nav.foreldrepenger.behandlingslager.behandling.historikk;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum HistorikkBegrunnelseType implements Kodeverdi {

    UDEFINIERT("-", "Ikke definert"),
    SAKSBEH_START_PA_NYTT("SAKSBEH_START_PA_NYTT", "Saksbehandling starter på nytt"),
    BEH_STARTET_PA_NYTT("BEH_STARTET_PA_NYTT", "Behandling startet på nytt"),
    BERORT_BEH_ENDRING_DEKNINGSGRAD("BERORT_BEH_ENDRING_DEKNINGSGRAD", "Den andre forelderens behandling har endret antall disponible stønadsdager"),
    BERORT_BEH_OPPHOR("BERORT_BEH_OPPHOR", "Den andre forelderens vedtak er opphørt"),
    ;

    private static final Map<String, HistorikkBegrunnelseType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "HISTORIKK_BEGRUNNELSE_TYPE";

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

    HistorikkBegrunnelseType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, HistorikkBegrunnelseType> kodeMap() {
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

}
