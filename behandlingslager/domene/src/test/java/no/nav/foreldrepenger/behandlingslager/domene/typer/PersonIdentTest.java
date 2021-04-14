package no.nav.foreldrepenger.behandlingslager.domene.typer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class PersonIdentTest {

    @Test
    public void gyldigFoedselsnummer_Fnr() {
        var nasjoaltTestFnr = "10108000398";
        var gyldig = PersonIdent.erGyldigFnr(nasjoaltTestFnr);
        assertThat(gyldig).isTrue();

        assertThat(new PersonIdent(nasjoaltTestFnr).erDnr()).isFalse();
    }

    @Test
    public void gyldigFoedselsnummer_Dnr() {
        var dnr = "65038300827";
        var gyldig = PersonIdent.erGyldigFnr(dnr);
        assertThat(gyldig).isTrue();

        assertThat(new PersonIdent(dnr).erDnr()).isTrue();
    }

    @Test
    public void ugyldigFoedselsnummer() {
        var foedselsnummer = "10108000388";
        var gyldig = PersonIdent.erGyldigFnr(foedselsnummer);
        assertThat(gyldig).isFalse();

        foedselsnummer = "9999999999";
        gyldig = PersonIdent.erGyldigFnr(foedselsnummer);
        assertThat(gyldig).isFalse();
    }
}
