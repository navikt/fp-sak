package no.nav.foreldrepenger.mottak.fyllutsendinn;

/** Shared identity component: fødselsnummer or D-nummer. */
public class Identitet {

    /** "ja" or "nei" – whether the person has a Norwegian national ID number. */
    private JaNei harDuFodselsnummer;
    private String identitetsnummer;

    public Identitet() {}

    public JaNei getHarDuFodselsnummer() { return harDuFodselsnummer; }
    public void setHarDuFodselsnummer(JaNei harDuFodselsnummer) { this.harDuFodselsnummer = harDuFodselsnummer; }

    public String getIdentitetsnummer() { return identitetsnummer; }
    public void setIdentitetsnummer(String identitetsnummer) { this.identitetsnummer = identitetsnummer; }
}
