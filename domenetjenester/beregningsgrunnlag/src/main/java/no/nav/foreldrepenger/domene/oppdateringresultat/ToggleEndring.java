package no.nav.foreldrepenger.domene.oppdateringresultat;

public class ToggleEndring {

    private Boolean fraVerdi;
    private Boolean tilVerdi;

    public ToggleEndring(Boolean fraVerdi, Boolean tilVerdi) {
        this.fraVerdi = fraVerdi;
        this.tilVerdi = tilVerdi;
    }

    public Boolean getFraVerdi() {
        return fraVerdi;
    }

    public Boolean getTilVerdi() {
        return tilVerdi;
    }
}
