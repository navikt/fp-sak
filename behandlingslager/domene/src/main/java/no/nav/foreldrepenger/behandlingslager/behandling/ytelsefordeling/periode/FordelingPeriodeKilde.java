package no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum FordelingPeriodeKilde implements Kodeverdi, DatabaseKode {

    SØKNAD("SØKNAD", "Søknad"),
    TIDLIGERE_VEDTAK("TIDLIGERE_VEDTAK", "Vedtak"),
    ANDRE_NAV_VEDTAK("ANDRE_NAV_VEDTAK", "Vedtak andre ytelser"),
    SAKSBEHANDLER("SAKSBEHANDLER", "Saksbehandler")
    ;
    private static final Map<String, FordelingPeriodeKilde> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String navn;
    @JsonValue
    @EnumeratedValue
    private final String kode;

    FordelingPeriodeKilde(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

}
