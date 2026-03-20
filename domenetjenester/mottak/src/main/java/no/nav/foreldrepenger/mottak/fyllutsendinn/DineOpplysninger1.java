package no.nav.foreldrepenger.mottak.fyllutsendinn;

/**
 * Shared "dineOpplysninger1" container – present in all forms.
 * Contains personal information pre-filled from national registry.
 */
public class DineOpplysninger1 {

    private String fornavn;
    private String etternavn;
    private Identitet identitet;
    private NavAddress adresse;
    private AddressValidity adresseVarighet;

    public DineOpplysninger1() {}

    public String getFornavn() { return fornavn; }
    public void setFornavn(String fornavn) { this.fornavn = fornavn; }

    public String getEtternavn() { return etternavn; }
    public void setEtternavn(String etternavn) { this.etternavn = etternavn; }

    public Identitet getIdentitet() { return identitet; }
    public void setIdentitet(Identitet identitet) { this.identitet = identitet; }

    public NavAddress getAdresse() { return adresse; }
    public void setAdresse(NavAddress adresse) { this.adresse = adresse; }

    public AddressValidity getAdresseVarighet() { return adresseVarighet; }
    public void setAdresseVarighet(AddressValidity adresseVarighet) { this.adresseVarighet = adresseVarighet; }
}
