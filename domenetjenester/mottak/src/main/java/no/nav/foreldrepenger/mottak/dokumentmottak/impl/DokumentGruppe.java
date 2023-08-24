package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum DokumentGruppe implements Kodeverdi {

    SØKNAD("SØKNAD", "Søknad"),
    INNTEKTSMELDING("INNTEKTSMELDING", "Inntektsmelding"),
    ENDRINGSSØKNAD("ENDRINGSSØKNAD", "Endringssøknad"),
    KLAGE("KLAGE", "Klage"),
    VEDLEGG("VEDLEGG", "Vedlegg"),
    UDEFINERT("-", "Ikke definert"),

    ;

    public static final String KODEVERK = "DOKUMENT_GRUPPE";

    private String navn;

    @JsonValue
    private String kode;

    DokumentGruppe(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
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
