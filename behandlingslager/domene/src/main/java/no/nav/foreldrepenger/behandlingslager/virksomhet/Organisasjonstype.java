package no.nav.foreldrepenger.behandlingslager.virksomhet;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum Organisasjonstype implements Kodeverdi {

    JURIDISK_ENHET("JURIDISK_ENHET", "Juridisk enhet"),
    VIRKSOMHET("VIRKSOMHET", "Virksomhet"),
    ORGLEDD("ORGANISASJONSLEDD", "Organisasjonsledd"),
    KUNSTIG("KUNSTIG", "Kunstig arbeidsforhold lagt til av saksbehandler"),
    UDEFINERT(STANDARDKODE_UDEFINERT, "Udefinert"),
    ;

    public static final String KODEVERK = "ORGANISASJONSTYPE";

    private final String navn;
    @JsonValue
    private final String kode;

    Organisasjonstype(String kode, String navn) {
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

    public static boolean erKunstig(String orgNr) {
        return OrgNummer.KUNSTIG_ORG.equals(orgNr);
    }
}
