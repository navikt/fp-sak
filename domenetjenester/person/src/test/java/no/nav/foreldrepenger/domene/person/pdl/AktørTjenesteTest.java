package no.nav.foreldrepenger.domene.person.pdl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.Identliste;
import no.nav.vedtak.felles.integrasjon.person.Persondata;

class AktørTjenesteTest {

    private AktørTjeneste aktørTjeneste;

    private Persondata pdlMock = Mockito.mock(Persondata.class);

    private final AktørId aktørId = AktørId.dummy();
    private final PersonIdent fnr = PersonIdent.randomMor();

    @BeforeEach
    void setup() {
        aktørTjeneste = new AktørTjeneste(pdlMock);
    }

    @Test
    void basics_hent_aktørid() {
        Mockito.when(pdlMock.hentIdenter(any(), any()))
                .thenReturn(new Identliste(List.of(new IdentInformasjon(aktørId.getId(), IdentGruppe.AKTORID, false))));

        var optAktørId = aktørTjeneste.hentAktørIdForPersonIdent(fnr);
        assertThat(optAktørId)
            .isPresent()
            .hasValueSatisfying(v -> assertThat(v).isEqualTo(aktørId));
    }

    @Test
    void basics_hent_ident() {
        Mockito.when(pdlMock.hentIdenter(any(), any()))
                .thenReturn(new Identliste(List.of(new IdentInformasjon(fnr.getIdent(), IdentGruppe.FOLKEREGISTERIDENT, false))));

        var optFnr = aktørTjeneste.hentPersonIdentForAktørId(aktørId);
        assertThat(optFnr)
            .isPresent()
            .hasValueSatisfying(v -> assertThat(v).isEqualTo(fnr));
    }

}
