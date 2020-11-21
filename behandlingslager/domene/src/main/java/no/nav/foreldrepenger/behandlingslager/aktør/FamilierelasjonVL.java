package no.nav.foreldrepenger.behandlingslager.aktør;

import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class FamilierelasjonVL {
    private PersonIdent personIdent;
    private RelasjonsRolleType relasjonsrolle;
    private Boolean harSammeBosted;

    public FamilierelasjonVL(PersonIdent personIdent, RelasjonsRolleType relasjonsrolle,
                             Boolean harSammeBosted) {
        this.personIdent = personIdent;
        this.relasjonsrolle = relasjonsrolle;
        this.harSammeBosted = harSammeBosted;
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public RelasjonsRolleType getRelasjonsrolle() {
        return relasjonsrolle;
    }

    public Boolean getHarSammeBosted() {
        return harSammeBosted;
    }

    @Override
    public String toString() {
        // tar ikke med personIdent i toString så det ikke lekkeri logger etc.
        return getClass().getSimpleName()
                + "<relasjon=" + relasjonsrolle  //$NON-NLS-1$
                + ">"; //$NON-NLS-1$
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FamilierelasjonVL that = (FamilierelasjonVL) o;
        return Objects.equals(personIdent, that.personIdent) && Objects.equals(relasjonsrolle, that.relasjonsrolle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(personIdent, relasjonsrolle);
    }
}
