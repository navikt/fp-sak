package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum Tema implements Kodeverdi, MedOffisiellKode {

    FOR("FOR", "Foreldre- og Svangerskapspenger", "FOR")
    ;

    private final String navn;

    private final String offisiellKode;
    @JsonValue
    private final String kode;

    Tema(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    public static Optional<Tema> finnForKodeverkEiersKode(String tema) {
        return Stream.of(values()).filter(k -> Objects.equals(k.offisiellKode, tema)).findFirst();
    }

}
