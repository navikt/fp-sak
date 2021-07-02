package no.nav.foreldrepenger.behandlingslager.aktør;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.typer.PersonIdent;



public record FamilierelasjonVL(PersonIdent personIdent, RelasjonsRolleType relasjonsrolle) {

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [relasjonsrolle" + relasjonsrolle + "]";
    }
}
