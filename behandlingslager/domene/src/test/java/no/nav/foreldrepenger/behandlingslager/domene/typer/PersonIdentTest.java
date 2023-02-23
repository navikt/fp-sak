package no.nav.foreldrepenger.behandlingslager.domene.typer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.typer.PersonIdent;

public class PersonIdentTest {

    private static final String NASJONALT_TEST_FNR = "10108000398";

    @Test
    public void gyldigFoedselsnummer_Fnr() {
        assertThat(PersonIdent.erGyldigFnr(NASJONALT_TEST_FNR)).isTrue();

        assertThat(new PersonIdent(NASJONALT_TEST_FNR).erDnr()).isFalse();
    }

    @Test
    public void gyldigFoedselsnummer_Masker() {
        assertThat(new PersonIdent(NASJONALT_TEST_FNR)).hasToString("PersonIdent<ident=1010******8>");
    }

    @Test
    public void gyldigFoedselsnummer_Dnr() {
        var testDnr = "65038300827";
        assertThat(PersonIdent.erGyldigFnr(testDnr)).isTrue();

        assertThat(new PersonIdent(testDnr).erDnr()).isTrue();
    }

    @Test
    public void ugyldigFoedselsnummer() {
        var foedselsnummer = NASJONALT_TEST_FNR.replace("398", "388");
        assertThat(PersonIdent.erGyldigFnr(foedselsnummer)).isFalse();

        assertThat(PersonIdent.erGyldigFnr("9999999999")).isFalse();

        assertThat(PersonIdent.erGyldigFnr("101080-0388")).isFalse();
    }
}
