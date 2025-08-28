package no.nav.foreldrepenger.familiehendelse.rest;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;

public enum SøknadType {
    FØDSEL("ST-001"),
    ADOPSJON("ST-002"),
    ;
    @JsonValue
    private final String kode;

    SøknadType(String kode) {
        this.kode = kode;
    }

    public String getKode() {
        return kode;
    }

    public static SøknadType fra(FamilieHendelseEntitet type) {
        if (type == null) {
            return null;
        }
        if (type.getGjelderFødsel()) {
            return SøknadType.FØDSEL;
        }
        if (type.getGjelderAdopsjon()) {
            return SøknadType.ADOPSJON;
        }
        throw new IllegalArgumentException("Kan ikke mappe fra familieHendelse" + type + " til SøknadType");
    }

}

