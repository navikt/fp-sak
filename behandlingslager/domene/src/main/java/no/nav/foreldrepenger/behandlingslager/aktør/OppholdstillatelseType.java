package no.nav.foreldrepenger.behandlingslager.aktør;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum OppholdstillatelseType implements Kodeverdi, DatabaseKode {

    MIDLERTIDIG("MIDLERTIDIG", "Midlertidig oppholdstillatelse"),
    PERMANENT("PERMANENT", "Permanent oppholdstillatelse"),
    ;

    private final String navn;

    @JsonValue
    @EnumeratedValue
    private final String kode;

    OppholdstillatelseType(String kode, String navn) {
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
