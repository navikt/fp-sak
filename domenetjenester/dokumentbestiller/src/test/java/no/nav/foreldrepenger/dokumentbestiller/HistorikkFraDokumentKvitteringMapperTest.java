package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;

class HistorikkFraDokumentKvitteringMapperTest {

    @Test
    void testDokumentKvitteringMapper() {
        var dokumentMal = DokumentMalType.INNHENTE_OPPLYSNINGER;
        var journalpostId = "1231234234234";
        var dokumentId = "232342342";

        var behandlingId = 12L;
        var fagsakId = 34L;
        var historikkinnslag = HistorikkFraDokumentKvitteringMapper.opprettHistorikkInnslag(dokumentMal, journalpostId, dokumentId, behandlingId, fagsakId);

        assertThat(behandlingId).isEqualTo(historikkinnslag.getBehandlingId());
        assertThat(fagsakId).isEqualTo(historikkinnslag.getFagsakId());
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.VEDTAKSLØSNINGEN);

        assertThat(historikkinnslag.getDokumentLinker()).isNotNull();
        var dokumentLink = historikkinnslag.getDokumentLinker().getFirst();
        assertThat(journalpostId).isEqualTo(dokumentLink.getJournalpostId().getVerdi());
        assertThat(dokumentId).isEqualTo(dokumentLink.getDokumentId());
        assertThat(dokumentMal.getNavn()).isEqualTo(dokumentLink.getLinkTekst());
    }

}
