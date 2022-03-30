package no.nav.foreldrepenger.dokumentbestiller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto;

public class HistorikkFraBrevKvitteringMapperTest {

    @Test
    void testBrevKvitteringMapper() {
        var dokumentbestillingUuid = UUID.randomUUID();
        var dokumentMal = DokumentMalType.INNHENTE_OPPLYSNINGER;
        var journalpostId = "1231234234234";
        var dokumentId = "232342342";
        var kvittering = new DokumentProdusertDto(UUID.randomUUID(), dokumentbestillingUuid, dokumentMal.getKode(), journalpostId, dokumentId);

        var behandlingId = 12L;
        var fagsakId = 34L;
        var historikkinnslag = HistorikkFraBrevKvitteringMapper.opprettHistorikkInnslag(kvittering, behandlingId, fagsakId);

        assertEquals(behandlingId, historikkinnslag.getBehandlingId());
        assertEquals(fagsakId, historikkinnslag.getFagsakId());
        assertEquals(HistorikkAktør.VEDTAKSLØSNINGEN, historikkinnslag.getAktør());
        assertEquals(NavBrukerKjønn.UDEFINERT, historikkinnslag.getKjoenn());

        assertNotNull(historikkinnslag.getDokumentLinker());
        assertEquals(journalpostId, historikkinnslag.getDokumentLinker().get(0).getJournalpostId().getVerdi());
        assertEquals(dokumentId, historikkinnslag.getDokumentLinker().get(0).getDokumentId());
        assertEquals(dokumentMal.getNavn(), historikkinnslag.getDokumentLinker().get(0).getLinkTekst());
    }

}
