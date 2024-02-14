package no.nav.foreldrepenger.dokumentbestiller;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.vedtak.exception.TekniskException;

class HistorikkFraDokumentKvitteringMapper {

    private HistorikkFraDokumentKvitteringMapper() {
    }

    static Historikkinnslag opprettHistorikkInnslag(DokumentMalType dokumentMal, String journalpostId, String dokumentId, long behandlingId, long fagsakId) {
        return opprettHistorikk(dokumentMal, journalpostId, dokumentId, behandlingId, fagsakId);
    }

    private static Historikkinnslag opprettHistorikk(DokumentMalType dokumentMal, String journalpostId, String dokumentId, long behandlingId, long fagsakId) {
        var nyttHistorikkInnslag = new Historikkinnslag.Builder()
            .medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medType(HistorikkinnslagType.BREV_SENT)
            .build();

        mapBrevSendtDel(nyttHistorikkInnslag);
        var dokumentLenker = mapDokumentlink(dokumentMal, dokumentId, journalpostId, nyttHistorikkInnslag);
        nyttHistorikkInnslag.setDokumentLinker(List.of(dokumentLenker));
        return nyttHistorikkInnslag;
    }

    private static void mapBrevSendtDel(Historikkinnslag nyttHistorikkInnslag) {
        new HistorikkInnslagTekstBuilder().medHendelse(nyttHistorikkInnslag.getType()).medBegrunnelse("").build(nyttHistorikkInnslag);
    }

    private static HistorikkinnslagDokumentLink mapDokumentlink(DokumentMalType dokumentMal, String dokumentId, String journalpostId, Historikkinnslag historikkinnslag) {
        var builder = new HistorikkinnslagDokumentLink.Builder()
            .medLinkTekst(dokumentMal.getNavn())
            .medHistorikkinnslag(historikkinnslag)
            .medDokumentId(dokumentId);
        if (JournalpostId.erGyldig(journalpostId)) {
            builder.medJournalpostId(new JournalpostId(journalpostId));
        } else {
            throw new TekniskException("FP-164754", "Mottok ugyldig journalpost ID " + journalpostId);
        }
        return builder.build();
    }
}
