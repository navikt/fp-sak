package no.nav.foreldrepenger.dokumentbestiller;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto;
import no.nav.vedtak.exception.TekniskException;

public class HistorikkFraBrevKvitteringMapper {

    static Historikkinnslag opprettHistorikkInnslag(DokumentProdusertDto kvittering, long behandlingId, long fagsakId) {
        var nyttHistorikkInnslag = new Historikkinnslag.Builder()
            .medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medType(HistorikkinnslagType.BREV_SENT)
            .build();

        mapBrevSendtDel(nyttHistorikkInnslag);
        var dokumentLenker = mapDokumentlink(kvittering.dokumentMal(), kvittering.dokumentId(), kvittering.journalpostId(), nyttHistorikkInnslag);
        nyttHistorikkInnslag.setDokumentLinker(List.of(dokumentLenker));
        return nyttHistorikkInnslag;
    }

    private static void mapBrevSendtDel(Historikkinnslag nyttHistorikkInnslag) {
        new HistorikkInnslagTekstBuilder().medHendelse(nyttHistorikkInnslag.getType()).medBegrunnelse("").build(nyttHistorikkInnslag);
    }

    private static HistorikkinnslagDokumentLink mapDokumentlink(String dokumentMal, String dokumentId, String journalpostId, Historikkinnslag historikkinnslag) {
        var builder = new HistorikkinnslagDokumentLink.Builder()
            .medLinkTekst(DokumentMalType.utledDokumentTittel(dokumentMal))
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
