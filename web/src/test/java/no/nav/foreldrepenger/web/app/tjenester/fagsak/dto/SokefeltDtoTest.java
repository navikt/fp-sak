package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;

public class SokefeltDtoTest {

    @Test
    public void skal_ha_spesial_abac_type_når_det_er_et_fødslelsnummer_siden_alle_sakene_kan_være_knyttet_til_andre_parter() throws Exception {
        var fnr = new FiktiveFnr().nesteKvinneFnr();
        var dto = new SokefeltDto(fnr);

        assertThat(dto.abacAttributter()).isEqualTo(AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.FNR, fnr)
                .leggTil(AppAbacAttributtType.SAKER_MED_FNR, fnr));
    }

    @Test
    public void skal_ha_normal_saksnummer_abac_type_når_det_ikke_er_et_fødslelsnummer() throws Exception {
        var saksnummer = new Saksnummer("123123123123");
        assertThat(new SokefeltDto(saksnummer).abacAttributter()).isEqualTo(AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKSNUMMER, saksnummer.getVerdi()));
    }
}
