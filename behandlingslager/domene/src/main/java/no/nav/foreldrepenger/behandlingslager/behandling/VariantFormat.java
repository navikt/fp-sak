package no.nav.foreldrepenger.behandlingslager.behandling;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedOffisiellKode;

public enum VariantFormat implements Kodeverdi, MedOffisiellKode {

    SLADDET("SLADD", "Sladdet format", "SLADDET"),
    SKANNING_META("SKANM", "Skanning metadata", "SKANNING_META"),
    PRODUKSJON("PROD", "Produksjonsformat", "PRODUKSJON"),
    PRODUKSJON_DLF("PRDLF", "Produksjonsformat DLF", "PRODUKSJON_DLF"),
    ORIGINAL("ORIG", "Originalformat", "ORIGINAL"),
    FULLVERSJON("FULL", "Versjon med infotekster", "FULLVERSJON"),
    BREVBESTILLING("BREVB", "Brevbestilling data", "BREVBESTILLING"),
    ARKIV("ARKIV", "Arkivformat", "ARKIV"),
    UDEFINERT("-", "Ikke definert", null),

    ;
    private static final String KODEVERK = "VARIANT_FORMAT";

    private String navn;

    private String offisiellKode;
    @JsonValue
    private String kode;

    VariantFormat(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
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

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

    public static VariantFormat finnForKodeverkEiersKode(String offisiellDokumentType) {
        return List.of(values()).stream().filter(k -> Objects.equals(k.offisiellKode, offisiellDokumentType)).findFirst().orElse(UDEFINERT);
    }
}
