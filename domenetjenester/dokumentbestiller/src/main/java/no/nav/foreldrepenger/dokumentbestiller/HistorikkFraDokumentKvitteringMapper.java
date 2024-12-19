package no.nav.foreldrepenger.dokumentbestiller;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2DokumentLink;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

class HistorikkFraDokumentKvitteringMapper {

    private HistorikkFraDokumentKvitteringMapper() {
    }

    public static Historikkinnslag2 opprettHistorikkInnslag(DokumentMalType dokumentMal,
                                                            String journalpostId,
                                                            String dokumentId,
                                                            Long behandlingId,
                                                            Long fagsakId) {
        return new Historikkinnslag2.Builder()
            .medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel("Brev er sendt")
            .medDokumenter(List.of(new Historikkinnslag2DokumentLink.Builder()
                    .medDokumentId(dokumentId)
                    .medJournalpostId(JournalpostId.erGyldig(journalpostId) ? new JournalpostId(journalpostId) : null)
                    .medLinkTekst(dokumentMal.getNavn())
                .build()))
            .build();
    }
}
