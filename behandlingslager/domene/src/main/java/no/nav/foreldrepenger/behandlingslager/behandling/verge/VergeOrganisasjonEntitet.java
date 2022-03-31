package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import no.nav.foreldrepenger.behandlingslager.BaseCreateableEntitet;

//TODO bør forenkles og flyttes ned til VERGE siden det kun er BRUKER_ID eller orgnr + navn
@Entity(name = "VergeOrganisasjon")
@Table(name = "VERGE_ORGANISASJON")
public class VergeOrganisasjonEntitet extends BaseCreateableEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VERGE_ORGANISASJON")
    private Long id;

    @Column(name = "orgnr")
    String organisasjonsnummer;

    @Column(name = "navn")
    String navn;

    @OneToOne(mappedBy = "vergeOrganisasjon")
    VergeEntitet verge;

    protected VergeOrganisasjonEntitet() {}

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

}
