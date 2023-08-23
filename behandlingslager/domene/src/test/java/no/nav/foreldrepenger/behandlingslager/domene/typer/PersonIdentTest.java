package no.nav.foreldrepenger.behandlingslager.domene.typer;

import no.nav.foreldrepenger.domene.typer.PersonIdent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonIdentTest {

    private static final String NASJONALT_TEST_FNR = "10108000398";

    @Test
    void gyldigFoedselsnummer_Fnr() {
        assertThat(PersonIdent.erGyldigFnr(NASJONALT_TEST_FNR)).isTrue();

        assertThat(new PersonIdent(NASJONALT_TEST_FNR).erDnr()).isFalse();
    }

    @Test
    void gyldigFoedselsnummer_Masker() {
        assertThat(new PersonIdent(NASJONALT_TEST_FNR)).hasToString("PersonIdent<ident=1010******8>");
    }

    @Test
    void gyldigFoedselsnummer_Dnr() {
        var testDnr = "65038300827";
        assertThat(PersonIdent.erGyldigFnr(testDnr)).isTrue();

        assertThat(new PersonIdent(testDnr).erDnr()).isTrue();
    }

    @Test
    void ugyldigFoedselsnummer() {
        var foedselsnummer = NASJONALT_TEST_FNR.replace("398", "388");
        assertThat(PersonIdent.erGyldigFnr(foedselsnummer)).isFalse();

        assertThat(PersonIdent.erGyldigFnr("9999999999")).isFalse();

        assertThat(PersonIdent.erGyldigFnr("101080-0388")).isFalse();
    }
}
