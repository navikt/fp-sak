package no.nav.foreldrepenger.behandlingslager.uttak;

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum UttakArbeidType implements Kodeverdi {

    ORDINÆRT_ARBEID("ORDINÆRT_ARBEID", "Ordinært arbeid"),
    SELVSTENDIG_NÆRINGSDRIVENDE("SELVSTENDIG_NÆRINGSDRIVENDE", "Selvstendig næringsdrivende"),
    FRILANS("FRILANS", "Frilans"),
    ANNET("ANNET", "Annet"),
    ;

    private final String navn;

    @EnumeratedValue
    @JsonValue
    private final String kode;

    UttakArbeidType(String kode, String navn) {
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

    public boolean erArbeidstakerEllerFrilans() {
        return ORDINÆRT_ARBEID.equals(this) || FRILANS.equals(this);
    }
}
