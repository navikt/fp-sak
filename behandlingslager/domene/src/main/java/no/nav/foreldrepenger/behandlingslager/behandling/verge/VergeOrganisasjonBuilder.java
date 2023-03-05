package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import java.util.Objects;

public class VergeOrganisasjonBuilder {
    private VergeOrganisasjonEntitet kladd;

    public VergeOrganisasjonBuilder() {
        kladd = new VergeOrganisasjonEntitet();
    }

    public VergeOrganisasjonBuilder medOrganisasjonsnummer(String organisasjonsnummer) {
        kladd.organisasjonsnummer = organisasjonsnummer;
        return this;
    }

    public VergeOrganisasjonBuilder medNavn(String navn) {
        kladd.navn = navn;
        return this;
    }

    public VergeOrganisasjonBuilder medVerge(VergeEntitet verge) {
        kladd.verge = verge;
        return this;
    }

    public VergeOrganisasjonEntitet build() {
        Objects.requireNonNull(kladd.organisasjonsnummer, "organisasjonsnummer");
        return kladd;
    }

}
