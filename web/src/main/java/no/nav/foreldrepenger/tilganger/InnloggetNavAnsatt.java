package no.nav.foreldrepenger.tilganger;

public record InnloggetNavAnsatt(String brukernavn, String navn) {


    @Override
    public String toString() {
        return "InnloggetNavAnsatt{}";
    }

}
