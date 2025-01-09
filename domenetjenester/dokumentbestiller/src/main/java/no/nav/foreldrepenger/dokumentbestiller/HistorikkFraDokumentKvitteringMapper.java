package no.nav.foreldrepenger.dokumentbestiller;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

class HistorikkFraDokumentKvitteringMapper {

    private HistorikkFraDokumentKvitteringMapper() {
    }

    public static Historikkinnslag opprettHistorikkInnslag(DokumentMalType dokumentMal,
                                                           String journalpostId,
                                                           String dokumentId,
                                                           Long behandlingId,
                                                           Long fagsakId) {
        return new Historikkinnslag.Builder()
            .medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel("Brev er sendt")
            .medDokumenter(List.of(new HistorikkinnslagDokumentLink.Builder()
                    .medDokumentId(dokumentId)
                    .medJournalpostId(JournalpostId.erGyldig(journalpostId) ? new JournalpostId(journalpostId) : null)
                    .medLinkTekst(dokumentMal.getNavn())
                .build()))
            .build();
    }
}
