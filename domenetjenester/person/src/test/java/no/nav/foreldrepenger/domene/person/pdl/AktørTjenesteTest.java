package no.nav.foreldrepenger.domene.person.pdl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumer;
import no.nav.vedtak.felles.integrasjon.aktør.klient.DetFinnesFlereAktørerMedSammePersonIdentException;
import no.nav.vedtak.felles.integrasjon.pdl.PdlKlient;

public class AktørTjenesteTest {

    private AktørTjeneste aktørTjeneste;

    private AktørConsumer aktørConsumerMock = Mockito.mock(AktørConsumer.class);
    private PdlKlient pdlMock = Mockito.mock(PdlKlient.class);

    private final AktørId aktørId = AktørId.dummy();
    private final PersonIdent fnr = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());

    @BeforeEach
    public void setup() {
        aktørTjeneste = new AktørTjeneste(pdlMock, aktørConsumerMock);
    }

    @Test
    public void skal_returnere_tom_når_det_finnes_flere_enn_en_aktør_på_samme_ident___kan_skje_ved_dødfødsler() throws Exception {
        DetFinnesFlereAktørerMedSammePersonIdentException exception = new DetFinnesFlereAktørerMedSammePersonIdentException(Mockito.mock(Feil.class));
        String fnr2 = "12345678901";
        Mockito.when(aktørConsumerMock.hentAktørIdForPersonIdent(fnr2))
                .thenThrow(exception);

        Optional<AktørId> optAktørId = aktørTjeneste.hentAktørIdForPersonIdent(new PersonIdent(fnr2));
        assertThat(optAktørId).isEmpty();
    }


}
