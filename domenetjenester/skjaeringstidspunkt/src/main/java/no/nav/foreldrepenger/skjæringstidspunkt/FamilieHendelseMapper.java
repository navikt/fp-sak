package no.nav.foreldrepenger.skjæringstidspunkt;

import no.nav.foreldrepenger.behandling.FamilieHendelseDato;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;

public class FamilieHendelseMapper {

    private FamilieHendelseMapper() {
    }

    public static FamilieHendelseDato mapTilFamilieHendelseDato(FamilieHendelseEntitet familieHendelse) {
        if (familieHendelse.getGjelderFødsel()) {
            return new FamilieHendelseDato(familieHendelse.getTermindato().orElse(null), familieHendelse.getFødselsdato().orElse(null), null);
        } else {
            return new FamilieHendelseDato(null, null, familieHendelse.getSkjæringstidspunkt());
        }
    }

}
