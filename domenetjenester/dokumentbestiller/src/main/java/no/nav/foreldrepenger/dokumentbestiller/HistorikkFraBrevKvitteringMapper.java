package no.nav.foreldrepenger.dokumentbestiller;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto;
import no.nav.vedtak.exception.TekniskException;

public class HistorikkFraBrevKvitteringMapper {

    protected static final String FP_FORMIDLING_SYSTEM = "FP-FORMIDLING";

    static Historikkinnslag opprettHistorikkInnslag(DokumentProdusertDto kvittering, long behandlingId, long fagsakId) {
        var nyttHistorikkInnslag = new Historikkinnslag.Builder()
            .medFagsakId(fagsakId)
            .medBehandlingId(behandlingId)
            .medUuid(kvittering.dokumentbestillingUuid())
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medKjoenn(NavBrukerKjønn.UDEFINERT)
            .medType(HistorikkinnslagType.BREV_SENT)
            .medOpprettetISystem(FP_FORMIDLING_SYSTEM)
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
            .medLinkTekst(DokumentMalType.fraKode(dokumentMal).getNavn())
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
