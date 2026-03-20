package no.nav.foreldrepenger.mottak.fyllutsendinn;

/**
 * Prefilled address component (navAddress).
 * Fields are pre-populated from the national registry (Folkeregisteret).
 */
public class NavAddress {

    private String adresse;
    private String postnummer;
    private String poststed;
    private String land;

    public NavAddress() {}

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getPostnummer() { return postnummer; }
    public void setPostnummer(String postnummer) { this.postnummer = postnummer; }

    public String getPoststed() { return poststed; }
    public void setPoststed(String poststed) { this.poststed = poststed; }

    public String getLand() { return land; }
    public void setLand(String land) { this.land = land; }
}
