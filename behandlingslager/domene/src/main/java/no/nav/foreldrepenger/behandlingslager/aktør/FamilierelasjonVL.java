package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class FamilierelasjonVL {
    private PersonIdent personIdent;
    private RelasjonsRolleType relasjonsrolle;

    public FamilierelasjonVL(PersonIdent personIdent, RelasjonsRolleType relasjonsrolle) {
        this.personIdent = personIdent;
        this.relasjonsrolle = relasjonsrolle;
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public RelasjonsRolleType getRelasjonsrolle() {
        return relasjonsrolle;
    }

    @Override
    public String toString() {
        // tar ikke med personIdent i toString så det ikke lekkeri logger etc.
        return getClass().getSimpleName() + "<relasjon=" + relasjonsrolle + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (FamilierelasjonVL) o;
        return Objects.equals(personIdent, that.personIdent) && Objects.equals(relasjonsrolle, that.relasjonsrolle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personIdent, relasjonsrolle);
    }
}
