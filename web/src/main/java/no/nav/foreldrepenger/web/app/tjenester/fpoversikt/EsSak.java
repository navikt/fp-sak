package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

record EsSak(String saksnummer, String aktørId, FamilieHendelse familieHendelse) implements Sak {

    @Override
    public String toString() {
        return "EsSak{" + "saksnummer='" + saksnummer + '\'' + ", familieHendelse=" + familieHendelse + '}';
    }
}
