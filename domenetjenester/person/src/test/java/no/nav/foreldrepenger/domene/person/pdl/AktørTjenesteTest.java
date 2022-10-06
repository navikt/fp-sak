package no.nav.foreldrepenger.domene.person.pdl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.pdl.IdentGruppe;
import no.nav.pdl.IdentInformasjon;
import no.nav.pdl.Identliste;
import no.nav.vedtak.felles.integrasjon.person.Persondata;

public class AktørTjenesteTest {

    private AktørTjeneste aktørTjeneste;

    private Persondata pdlMock = Mockito.mock(Persondata.class);

    private final AktørId aktørId = AktørId.dummy();
    private final PersonIdent fnr = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());

    @BeforeEach
    public void setup() {
        aktørTjeneste = new AktørTjeneste(pdlMock);
    }

    @Test
    public void basics_hent_aktørid() {
        Mockito.when(pdlMock.hentIdenter(any(), any()))
                .thenReturn(new Identliste(List.of(new IdentInformasjon(aktørId.getId(), IdentGruppe.AKTORID, false))));

        var optAktørId = aktørTjeneste.hentAktørIdForPersonIdent(fnr);
        assertThat(optAktørId).isPresent();
        assertThat(optAktørId).hasValueSatisfying(v -> assertThat(v).isEqualTo(aktørId));
    }

    @Test
    public void basics_hent_ident() {
        Mockito.when(pdlMock.hentIdenter(any(), any()))
                .thenReturn(new Identliste(List.of(new IdentInformasjon(fnr.getIdent(), IdentGruppe.FOLKEREGISTERIDENT, false))));

        var optFnr = aktørTjeneste.hentPersonIdentForAktørId(aktørId);
        assertThat(optFnr).isPresent();
        assertThat(optFnr).hasValueSatisfying(v -> assertThat(v).isEqualTo(fnr));
    }

}
