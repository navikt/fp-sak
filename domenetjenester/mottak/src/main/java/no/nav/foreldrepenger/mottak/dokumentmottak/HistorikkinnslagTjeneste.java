package no.nav.foreldrepenger.mottak.dokumentmottak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.*;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.*;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class HistorikkinnslagTjeneste {

    private static final String KLAGE = "Klage";
    private static final String VEDLEGG = "Vedlegg";
    private static final String PAPIRSØKNAD = "Papirsøknad";
    private static final String SØKNAD = "Søknad";
    private static final String INNTEKTSMELDING = "Inntektsmelding";
    private static final String ETTERSENDELSE = "Ettersendelse";
    private HistorikkRepository historikkRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    HistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HistorikkinnslagTjeneste(HistorikkRepository historikkRepository,
                                    DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.historikkRepository = historikkRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    public void opprettHistorikkinnslag(Behandling behandling, JournalpostId journalpostId, Boolean selvOmLoggetTidligere, boolean elektronisk, boolean erIM) {
        if (!selvOmLoggetTidligere && historikkinnslagForBehandlingStartetErLoggetTidligere(behandling.getId(), HistorikkinnslagType.BEH_STARTET)) {
            return;
        }

        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.SØKER);
        historikkinnslag.setType(HistorikkinnslagType.BEH_STARTET);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());

        leggTilHistorikkinnslagDokumentlinker(behandling.getType(), journalpostId, historikkinnslag, elektronisk, erIM);

        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(BehandlingType.KLAGE.equals(behandling.getType()) ? HistorikkinnslagType.KLAGEBEH_STARTET : HistorikkinnslagType.BEH_STARTET);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    private boolean historikkinnslagForBehandlingStartetErLoggetTidligere(Long behandlingId, HistorikkinnslagType historikkinnslagType) {
        var eksisterendeHistorikkListe = historikkRepository.hentHistorikk(behandlingId);


        if (!eksisterendeHistorikkListe.isEmpty()) {
            for (var eksisterendeHistorikk : eksisterendeHistorikkListe) {
                if (historikkinnslagType.equals(eksisterendeHistorikk.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    void leggTilHistorikkinnslagDokumentlinker(BehandlingType behandlingType, JournalpostId journalpostId,
                                               Historikkinnslag historikkinnslag, boolean elektronisk, boolean erIM) {
        List<HistorikkinnslagDokumentLink> dokumentLinker = new ArrayList<>();
        if (journalpostId != null) {
            dokumentArkivTjeneste.hentJournalpostForSak(journalpostId).ifPresent(jp -> {
                leggTilSøknadDokumentLenke(behandlingType, journalpostId, historikkinnslag, dokumentLinker, jp.getHovedDokument(), elektronisk, erIM);
                jp.getAndreDokument().forEach(ad -> dokumentLinker.add(lagHistorikkInnslagDokumentLink(ad, journalpostId, historikkinnslag, VEDLEGG)));
            });
        }

        historikkinnslag.setDokumentLinker(dokumentLinker);
    }

    private void leggTilSøknadDokumentLenke(BehandlingType behandlingType, JournalpostId journalpostId, Historikkinnslag historikkinnslag,
                                            List<HistorikkinnslagDokumentLink> dokumentLinker, ArkivDokument arkivDokument, boolean elektronisk, boolean erIM) {
        if (elektronisk) {
            var linkTekst = BehandlingType.KLAGE.equals(behandlingType) ? KLAGE : erIM ? INNTEKTSMELDING : SØKNAD;
            dokumentLinker.add(lagHistorikkInnslagDokumentLink(arkivDokument, journalpostId, historikkinnslag, linkTekst));
        } else {
            var linkTekst = BehandlingType.KLAGE.equals(behandlingType) ? KLAGE : BehandlingType.UDEFINERT.equals(behandlingType) ? ETTERSENDELSE : PAPIRSØKNAD;
            if (arkivDokument != null)
                dokumentLinker.add(lagHistorikkInnslagDokumentLink(arkivDokument, journalpostId, historikkinnslag, linkTekst));
        }
    }

    private HistorikkinnslagDokumentLink lagHistorikkInnslagDokumentLink(ArkivDokument arkivDokument, JournalpostId journalpostId, Historikkinnslag historikkinnslag, String linkTekst) {
        var historikkinnslagDokumentLink = new HistorikkinnslagDokumentLink();
        historikkinnslagDokumentLink.setDokumentId(arkivDokument.getDokumentId());
        historikkinnslagDokumentLink.setJournalpostId(journalpostId);
        historikkinnslagDokumentLink.setLinkTekst(linkTekst);
        historikkinnslagDokumentLink.setHistorikkinnslag(historikkinnslag);
        return historikkinnslagDokumentLink;
    }

    public void opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(Behandling behandling){
        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.AVBRUTT_BEH)
            .medÅrsak(BehandlingResultatType.MERGET_OG_HENLAGT);
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.AVBRUTT_BEH);
        historikkinnslag.setBehandlingId(behandling.getId());
        builder.build(historikkinnslag);
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForVedlegg(Fagsak fagsak, JournalpostId journalpostId, DokumentTypeId dokumentTypeId, boolean elektronisk) {
        var historikkinnslag = new Historikkinnslag();
        if (DokumentTypeId.INNTEKTSMELDING.equals(dokumentTypeId)) {
            historikkinnslag.setAktør(HistorikkAktør.ARBEIDSGIVER);
        } else {
            historikkinnslag.setAktør(HistorikkAktør.SØKER);
        }
        historikkinnslag.setType(HistorikkinnslagType.VEDLEGG_MOTTATT);
        historikkinnslag.setFagsakId(fagsak.getId());

        leggTilHistorikkinnslagDokumentlinker(BehandlingType.UDEFINERT, journalpostId, historikkinnslag,
            elektronisk, DokumentTypeId.INNTEKTSMELDING.equals(dokumentTypeId));

        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.VEDLEGG_MOTTATT);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Behandling behandling, HistorikkinnslagType historikkinnslagType, LocalDateTime frist, Venteårsak venteårsak) {
        var builder = new HistorikkInnslagTekstBuilder();
        builder.medHendelse(historikkinnslagType);
        if (frist != null) {
            builder.medHendelse(historikkinnslagType, frist.toLocalDate());
        }
        if (!Venteårsak.UDEFINERT.equals(venteårsak)) {
            builder.medÅrsak(venteårsak);
        }
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(historikkinnslagType);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());
        builder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());

        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL)
            .medBegrunnelse(behandlingÅrsakType);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForEndringshendelse(Fagsak fagsak, String begrunnelse) {
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL);
        historikkinnslag.setFagsakId(fagsak.getId());

        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.BEH_OPPDATERT_NYE_OPPL)
            .medBegrunnelse(begrunnelse);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }
}
