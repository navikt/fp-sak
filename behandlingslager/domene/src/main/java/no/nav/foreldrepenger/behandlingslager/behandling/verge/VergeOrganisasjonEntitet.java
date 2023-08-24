package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import jakarta.persistence.*;
import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;

import java.util.Objects;

//TODO bør forenkles og flyttes ned til VERGE siden det kun er BRUKER_ID eller orgnr + navn
@Entity(name = "VergeOrganisasjon")
@Table(name = "VERGE_ORGANISASJON")
public class VergeOrganisasjonEntitet extends BaseCreateableEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VERGE_ORGANISASJON")
    private Long id;

    @Column(name = "orgnr")
    private String organisasjonsnummer;

    @Column(name = "navn")
    private String navn;

    @OneToOne(mappedBy = "vergeOrganisasjon")
    private VergeEntitet verge;

    protected VergeOrganisasjonEntitet() {
    }

    // deep copy
    VergeOrganisasjonEntitet(VergeOrganisasjonEntitet vergeOrganisasjon, VergeEntitet verge) {
        this.organisasjonsnummer = vergeOrganisasjon.getOrganisasjonsnummer();
        this.navn = vergeOrganisasjon.getNavn();
        this.verge = verge;
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public void setOrganisasjonsnummer(String organisasjonsnummer) {
        this.organisasjonsnummer = organisasjonsnummer;
    }


    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }


    public VergeEntitet getVerge() {
        return verge;
    }

    public void setVerge(VergeEntitet verge) {
        this.verge = verge;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var entitet = (VergeOrganisasjonEntitet) o;
        return Objects.equals(organisasjonsnummer, entitet.organisasjonsnummer) && Objects.equals(navn, entitet.navn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organisasjonsnummer, navn);
    }

    public static class Builder {
        private final VergeOrganisasjonEntitet kladd;

        public Builder() {
            kladd = new VergeOrganisasjonEntitet();
        }

        public Builder medOrganisasjonsnummer(String organisasjonsnummer) {
            kladd.organisasjonsnummer = organisasjonsnummer;
            return this;
        }

        public Builder medNavn(String navn) {
            kladd.navn = navn;
            return this;
        }

        public Builder medVerge(VergeEntitet verge) {
            kladd.verge = verge;
            return this;
        }

        public VergeOrganisasjonEntitet build() {
            Objects.requireNonNull(kladd.organisasjonsnummer, "organisasjonsnummer");
            return kladd;
        }
    }
}
