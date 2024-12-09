package no.nav.foreldrepenger.tilganger;

import java.util.Objects;
import java.util.Set;

import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;

public record InnloggetNavAnsatt(String brukernavn, String navn, Set<AnsattGruppe> ansattGrupper) {


    // Legg til flere ved behov
    public boolean kanSaksbehandle() {
        return ansattGrupper().contains(AnsattGruppe.SAKSBEHANDLER);
    }

    public boolean kanOverstyre() {
        return ansattGrupper().contains(AnsattGruppe.OVERSTYRER);
    }

    @Override
    public String toString() {
        return "InnloggetNavAnsatt{" +
            "ansattGrupper=" + ansattGrupper + '}';
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InnloggetNavAnsatt that = (InnloggetNavAnsatt) o;
        return ansattGrupper.size() == that.ansattGrupper().size() && ansattGrupper.containsAll(that.ansattGrupper())
            && Objects.equals(navn, that.navn) && Objects.equals(brukernavn, that.brukernavn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brukernavn, navn, ansattGrupper);
    }
}
