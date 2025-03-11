package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavetype;

public enum OppgaveType implements Kodeverdi {

    VUR_KONSEKVENS("VUR_KONSEKVENS", "Vurder konsekvens for ytelse"),
    VUR_DOKUMENT("VUR_DOKUMENT", "Vurder dokument");

    public static final String KODEVERK = "OPPGAVE_TYPE";
    private static final Map<String, OppgaveType> KODER = new LinkedHashMap<>();

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

    OppgaveType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static OppgaveType fra(Oppgavetype oppgavetype) {
        return switch (oppgavetype) {
            case Oppgavetype.VURDER_KONSEKVENS_YTELSE -> VUR_KONSEKVENS;
            case Oppgavetype.VURDER_DOKUMENT -> VUR_DOKUMENT;
            default -> null;
        };
    }

    public static Map<String, OppgaveType> kodeMap() {
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
