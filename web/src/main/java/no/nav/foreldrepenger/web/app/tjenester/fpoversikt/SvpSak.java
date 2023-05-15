package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import java.util.Set;

record SvpSak(String saksnummer,
              String aktørId,
              FamilieHendelse familieHendelse,
              Status status,
              Set<Aksjonspunkt> aksjonspunkt,
              Set<Egenskap> egenskaper) implements Sak {

    @Override
    public String toString() {
        return "SvpSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + ", status=" + status + ", aksjonspunkt="
            + aksjonspunkt + ", egenskaper=" + egenskaper + '}';
    }
}
