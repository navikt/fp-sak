package no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving;

public class VarseltekstDto {

    private String varseltekst;

    VarseltekstDto() {
        // Jackson
    }

    public VarseltekstDto(String varseltekst) {
        this.varseltekst = varseltekst;
    }

    public String getVarseltekst() {
        return varseltekst;
    }
}
