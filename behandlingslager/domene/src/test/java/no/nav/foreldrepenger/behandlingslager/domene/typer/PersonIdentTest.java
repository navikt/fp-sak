package no.nav.foreldrepenger.behandlingslager.domene.typer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class PersonIdentTest {

    @Test
    public void gyldigFoedselsnummer_Fnr() {
        String nasjoaltTestFnr = "10108000398";
        boolean gyldig = PersonIdent.erGyldigFnr(nasjoaltTestFnr);
        assertThat(gyldig).isEqualTo(true);

        assertThat(new PersonIdent(nasjoaltTestFnr).erDnr()).isFalse();
    }

    @Test
    public void gyldigFoedselsnummer_Dnr() {
        String dnr = "65038300827";
        boolean gyldig = PersonIdent.erGyldigFnr(dnr);
        assertThat(gyldig).isEqualTo(true);

        assertThat(new PersonIdent(dnr).erDnr()).isTrue();
    }

    @Test
    public void ugyldigFoedselsnummer() {
        String foedselsnummer = "10108000388";
        boolean gyldig = PersonIdent.erGyldigFnr(foedselsnummer);
        assertThat(gyldig).isEqualTo(false);

        foedselsnummer = "9999999999";
        gyldig = PersonIdent.erGyldigFnr(foedselsnummer);
        assertThat(gyldig).isEqualTo(false);
    }
}
