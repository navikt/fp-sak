package no.nav.foreldrepenger.behandlingslager.behandling.anke;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum AnkeAvvistÅrsak implements Kodeverdi {

    ANKE_FOR_SENT("ANKE_FOR_SENT", "Bruker har anket for sent"),
    ANKE_UGYLDIG("ANKE_UGYLDIG", "Anke er ugyldig"),
    ANKE_IKKE_PAANKET_VEDTAK("ANKE_IKKE_PAANKET_VEDTAK", "Ikke påanket et vedtak"),
    ANKE_IKKE_PART("ANKE_IKKE_PART", "Anke er ikke part"),
    ANKE_IKKE_KONKRET("ANKE_IKKE_KONKRET", "Anke er ikke konkret"),
    ANKE_IKKE_SIGNERT("ANKE_IKKE_SIGNERT", "Anke er ikke signert"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Udefinert"),
    ;

    private String navn;

    @JsonValue
    private String kode;

    AnkeAvvistÅrsak(String kode, String navn) {
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
