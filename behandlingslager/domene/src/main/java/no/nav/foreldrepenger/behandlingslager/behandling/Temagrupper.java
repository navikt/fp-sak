package no.nav.foreldrepenger.behandlingslager.behandling;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum Temagrupper implements Kodeverdi, MedOffisiellKode {

    FAMILIEYTELSER("FMLI", "Familie", "FMLI"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Udefinert", null),
    ;

    @JsonValue
    private String kode;

    private String navn;

    private String offisiellKode;

    Temagrupper(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

}
