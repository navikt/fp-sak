package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum Diskresjonskode implements Kodeverdi, MedOffisiellKode {

    UDEFINERT("UDEF", "Udefinert", null),
    KODE6("SPSF", "Sperret adresse, strengt fortrolig", "SPSF"),
    KODE7("SPFO", "Sperret adresse, fortrolig", "SPFO"),

    ;

    private String navn;

    private String offisiellKode;
    @JsonValue
    private String kode;

    Diskresjonskode(String kode, String navn, String offisiellKode) {
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

    public static Diskresjonskode finnForKodeverkEiersKode(String diskresjonskode) {
        if (diskresjonskode == null) {
            return Diskresjonskode.UDEFINERT;
        }
        return Stream.of(values()).filter(k -> Objects.equals(k.offisiellKode, diskresjonskode)).findFirst().orElse(UDEFINERT);
    }

}
