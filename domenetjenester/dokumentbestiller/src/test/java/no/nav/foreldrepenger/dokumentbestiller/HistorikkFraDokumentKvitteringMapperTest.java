package no.nav.foreldrepenger.dokumentbestiller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;

class HistorikkFraDokumentKvitteringMapperTest {

    @Test
    void testDokumentKvitteringMapper() {
        var dokumentMal = DokumentMalType.INNHENTE_OPPLYSNINGER;
        var journalpostId = "1231234234234";
        var dokumentId = "232342342";

        var behandlingId = 12L;
        var fagsakId = 34L;
        var historikkinnslag = HistorikkFraDokumentKvitteringMapper.opprettHistorikkInnslag(dokumentMal, journalpostId, dokumentId, behandlingId,
            fagsakId);

        assertEquals(behandlingId, historikkinnslag.getBehandlingId());
        assertEquals(fagsakId, historikkinnslag.getFagsakId());
        assertEquals(HistorikkAktør.VEDTAKSLØSNINGEN, historikkinnslag.getAktør());

        assertNotNull(historikkinnslag.getDokumentLinker());
        var dokumentLink = historikkinnslag.getDokumentLinker().getFirst();
        assertEquals(journalpostId, dokumentLink.getJournalpostId().getVerdi());
        assertEquals(dokumentId, dokumentLink.getDokumentId());
        assertEquals(dokumentMal.getNavn(), dokumentLink.getLinkTekst());
    }

}
