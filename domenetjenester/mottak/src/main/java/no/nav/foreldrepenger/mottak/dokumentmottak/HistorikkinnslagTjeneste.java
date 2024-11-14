package no.nav.foreldrepenger.mottak.dokumentmottak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2DokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagV2;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class HistorikkinnslagTjeneste {

    private static final String KLAGE = "Klage";
    private static final String VEDLEGG = "Vedlegg";
    private static final String PAPIRSØKNAD = "Papirsøknad";
    private static final String SØKNAD = "Søknad";
    private static final String INNTEKTSMELDING = "Inntektsmelding";
    private static final String ETTERSENDELSE = "Ettersendelse";
    private HistorikkRepository historikkRepository;
    private Historikkinnslag2Repository historikkinnslag2Repository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    HistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HistorikkinnslagTjeneste(HistorikkRepository historikkRepository, Historikkinnslag2Repository historikkinnslag2Repository,
                                    DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.historikkRepository = historikkRepository;
        this.historikkinnslag2Repository = historikkinnslag2Repository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    public void opprettHistorikkinnslag(Behandling behandling, JournalpostId journalpostId, Boolean selvOmLoggetTidligere, boolean elektronisk, boolean erIM) {
        var dokumenter = lagDokumenterLenker(behandling.getType(), journalpostId, elektronisk, erIM);
        var tittel = BehandlingType.KLAGE.equals(behandling.getType()) ? "Klage mottatt" : "Behandling startet";
        var h = new HistorikkinnslagV2.Builder()
            .medTittel(tittel)
            .medAktør(HistorikkAktør.SØKER)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medDokumenter(dokumenter)
            .build();

        if (historikkinnslag2Repository.hent(behandling.getId()).stream().noneMatch(ek -> tittel.equals(ek.getTittel())) || selvOmLoggetTidligere) {
            historikkinnslag2Repository.lagre(h);
        }
    }

    private List<Historikkinnslag2DokumentLink> lagDokumenterLenker(BehandlingType behandlingType, JournalpostId journalpostId, boolean elektronisk, boolean erIM) {
        List<Historikkinnslag2DokumentLink> dokumentLinker = new ArrayList<>();
        if (journalpostId != null) {
            dokumentArkivTjeneste.hentJournalpostForSak(journalpostId).ifPresent(jp -> {
                leggTilSøknadDokumentLenke(behandlingType, journalpostId, dokumentLinker, jp.getHovedDokument(), elektronisk, erIM);
                jp.getAndreDokument().forEach(ad -> dokumentLinker.add(lagHistorikkInnslagDokumentLink(ad, journalpostId, VEDLEGG)));
            });
        }

        return dokumentLinker;
    }

    private void leggTilSøknadDokumentLenke(BehandlingType behandlingType, JournalpostId journalpostId,
                                            List<Historikkinnslag2DokumentLink> dokumentLinker, ArkivDokument arkivDokument, boolean elektronisk, boolean erIM) {
        if (elektronisk) {
            var linkTekst = BehandlingType.KLAGE.equals(behandlingType) ? KLAGE : erIM ? INNTEKTSMELDING : SØKNAD;
            dokumentLinker.add(lagHistorikkInnslagDokumentLink(arkivDokument, journalpostId, linkTekst));
        } else {
            var linkTekst = BehandlingType.KLAGE.equals(behandlingType) ? KLAGE : BehandlingType.UDEFINERT.equals(behandlingType) ? ETTERSENDELSE : PAPIRSØKNAD;
            if (arkivDokument != null)
                dokumentLinker.add(lagHistorikkInnslagDokumentLink(arkivDokument, journalpostId, linkTekst));
        }
    }

    private Historikkinnslag2DokumentLink lagHistorikkInnslagDokumentLink(ArkivDokument arkivDokument, JournalpostId journalpostId, String linkTekst) {
        var historikkinnslagDokumentLink = new Historikkinnslag2DokumentLink();
        historikkinnslagDokumentLink.setDokumentId(arkivDokument.getDokumentId());
        historikkinnslagDokumentLink.setJournalpostId(journalpostId);
        historikkinnslagDokumentLink.setLinkTekst(linkTekst);
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
        var dokumenter = lagDokumenterLenker(BehandlingType.UDEFINERT, journalpostId, elektronisk,
            DokumentTypeId.INNTEKTSMELDING.equals(dokumentTypeId));
        historikkinnslag2Repository.lagre(new HistorikkinnslagV2.Builder()
            .medTittel("Vedlegg mottatt")
            .medFagsakId(fagsak.getId())
            .medAktør(DokumentTypeId.INNTEKTSMELDING.equals(dokumentTypeId) ? HistorikkAktør.ARBEIDSGIVER : HistorikkAktør.SØKER)
            .medDokumenter(dokumenter)
            .build());
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
