package no.nav.foreldrepenger.behandlingslager.uttak;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum PeriodeResultatType implements Kodeverdi {

    INNVILGET("INNVILGET", "Innvilget"),
    AVSLÅTT("AVSLÅTT", "Avslått"),
    MANUELL_BEHANDLING("MANUELL_BEHANDLING", "Til manuell behandling"),
    ;

    private final String navn;

    @EnumeratedValue
    @JsonValue
    private final String kode;

    PeriodeResultatType(String kode, String navn) {
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
