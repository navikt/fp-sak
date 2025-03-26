package no.nav.foreldrepenger.mottak.dokumentmottak;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.typer.JournalpostId;

@ApplicationScoped
public class HistorikkinnslagTjeneste {

    private static final String KLAGE = "Klage";
    private static final String VEDLEGG = "Vedlegg";
    private static final String PAPIRSØKNAD = "Papirsøknad";
    private static final String SØKNAD = "Søknad";
    private static final String INNTEKTSMELDING = "Inntektsmelding";
    private static final String ETTERSENDELSE = "Ettersendelse";
    private HistorikkinnslagRepository historikkinnslagRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    HistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HistorikkinnslagTjeneste(HistorikkinnslagRepository historikkinnslagRepository, DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    public void opprettHistorikkinnslag(Behandling behandling,
                                        JournalpostId journalpostId,
                                        boolean elektronisk,
                                        boolean erIM) {
        var dokumenter = lagDokumenterLenker(behandling.getType(), journalpostId, elektronisk, erIM);
        var tittel = BehandlingType.KLAGE.equals(behandling.getType()) ? "Klage er mottatt" : "Behandling er startet";
        var h = new Historikkinnslag.Builder().medTittel(tittel)
            .medAktør(HistorikkAktør.SØKER)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medDokumenter(dokumenter)
            .build();

        historikkinnslagRepository.lagre(h);
    }

    private List<HistorikkinnslagDokumentLink> lagDokumenterLenker(BehandlingType behandlingType,
                                                                   JournalpostId journalpostId,
                                                                   boolean elektronisk,
                                                                   boolean erIM) {
        List<HistorikkinnslagDokumentLink> dokumentLinker = new ArrayList<>();
        if (journalpostId != null) {
            dokumentArkivTjeneste.hentJournalpostForSak(journalpostId).ifPresent(jp -> {
                leggTilSøknadDokumentLenke(behandlingType, journalpostId, dokumentLinker, jp.getHovedDokument(), elektronisk, erIM);
                jp.getAndreDokument().forEach(ad -> dokumentLinker.add(lagHistorikkInnslagDokumentLink(ad, journalpostId, VEDLEGG)));
            });
        }

        return dokumentLinker;
    }

    private void leggTilSøknadDokumentLenke(BehandlingType behandlingType,
                                            JournalpostId journalpostId,
                                            List<HistorikkinnslagDokumentLink> dokumentLinker,
                                            ArkivDokument arkivDokument,
                                            boolean elektronisk,
                                            boolean erIM) {
        if (elektronisk) {
            var linkTekst = BehandlingType.KLAGE.equals(behandlingType) ? KLAGE : erIM ? INNTEKTSMELDING : SØKNAD;
            dokumentLinker.add(lagHistorikkInnslagDokumentLink(arkivDokument, journalpostId, linkTekst));
        } else {
            var linkTekst = BehandlingType.KLAGE.equals(behandlingType) ? KLAGE : BehandlingType.UDEFINERT.equals(
                behandlingType) ? ETTERSENDELSE : PAPIRSØKNAD;
            if (arkivDokument != null) {
                dokumentLinker.add(lagHistorikkInnslagDokumentLink(arkivDokument, journalpostId, linkTekst));
            }
        }
    }

    private HistorikkinnslagDokumentLink lagHistorikkInnslagDokumentLink(ArkivDokument arkivDokument,
                                                                         JournalpostId journalpostId,
                                                                         String linkTekst) {
        var historikkinnslagDokumentLink = new HistorikkinnslagDokumentLink();
        historikkinnslagDokumentLink.setDokumentId(arkivDokument.getDokumentId());
        historikkinnslagDokumentLink.setJournalpostId(journalpostId);
        historikkinnslagDokumentLink.setLinkTekst(linkTekst);
        return historikkinnslagDokumentLink;
    }

    public void opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(Behandling behandling) {
        var historikk = new Historikkinnslag.Builder().medTittel("Behandlingen er henlagt")
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .addLinje("Mottatt ny søknad")
            .build();

        historikkinnslagRepository.lagre(historikk);
    }

    public void opprettHistorikkinnslagForVedlegg(Fagsak fagsak,
                                                  Behandling behandling,
                                                  JournalpostId journalpostId,
                                                  DokumentTypeId dokumentTypeId,
                                                  boolean elektronisk) {
        var dokumenter = lagDokumenterLenker(BehandlingType.UDEFINERT, journalpostId, elektronisk,
            DokumentTypeId.INNTEKTSMELDING.equals(dokumentTypeId));
        historikkinnslagRepository.lagre(new Historikkinnslag.Builder().medTittel("Vedlegg er mottatt")
            .medFagsakId(fagsak.getId())
            .medBehandlingId(behandling == null ? null : behandling.getId())
            .medAktør(DokumentTypeId.INNTEKTSMELDING.equals(dokumentTypeId) ? HistorikkAktør.ARBEIDSGIVER : HistorikkAktør.SØKER)
            .medDokumenter(dokumenter)
            .build());
    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Behandling behandling,
                                                                     LocalDateTime frist,
                                                                     Venteårsak venteårsak) {
        var historikkinnslagType = "Behandlingen er satt på vent";
        var tittel = frist == null ? historikkinnslagType :
            historikkinnslagType + " til " + format(frist.toLocalDate());
        var build = new Historikkinnslag.Builder().medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel(tittel)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId());
        if (!Venteårsak.UDEFINERT.equals(venteårsak)) {
            build.addLinje(venteårsak.getNavn());
        }
        historikkinnslagRepository.lagre(build.build());
    }

    public void opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel("Behandlingen oppdatert med nye opplysninger")
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .addLinje(behandlingÅrsakType.getNavn())
            .build();

        historikkinnslagRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForEndringshendelse(Fagsak fagsak, String begrunnelse) {
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel("Behandlingen oppdatert med nye opplysninger")
            .medFagsakId(fagsak.getId())
            .addLinje(begrunnelse)
            .build();

        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
