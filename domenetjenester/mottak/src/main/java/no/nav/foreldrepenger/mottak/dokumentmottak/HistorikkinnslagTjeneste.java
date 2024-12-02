package no.nav.foreldrepenger.mottak.dokumentmottak;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.format;

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
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2DokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
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
    private Historikkinnslag2Repository historikkinnslagRepository;
    private DokumentArkivTjeneste dokumentArkivTjeneste;

    HistorikkinnslagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public HistorikkinnslagTjeneste(Historikkinnslag2Repository historikkinnslagRepository, DokumentArkivTjeneste dokumentArkivTjeneste) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
    }

    public void opprettHistorikkinnslag(Behandling behandling,
                                        JournalpostId journalpostId,
                                        Boolean selvOmLoggetTidligere,
                                        boolean elektronisk,
                                        boolean erIM) {
        var dokumenter = lagDokumenterLenker(behandling.getType(), journalpostId, elektronisk, erIM);
        var tittel = BehandlingType.KLAGE.equals(behandling.getType()) ? "Klage mottatt" : "Behandling startet";
        var h = new Historikkinnslag2.Builder().medTittel(tittel)
            .medAktør(HistorikkAktør.SØKER)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medDokumenter(dokumenter)
            .build();

        if (historikkinnslagRepository.hent(behandling.getId()).stream().noneMatch(ek -> tittel.equals(ek.getTittel())) || selvOmLoggetTidligere) {
            historikkinnslagRepository.lagre(h);
        }
    }

    private List<Historikkinnslag2DokumentLink> lagDokumenterLenker(BehandlingType behandlingType,
                                                                    JournalpostId journalpostId,
                                                                    boolean elektronisk,
                                                                    boolean erIM) {
        List<Historikkinnslag2DokumentLink> dokumentLinker = new ArrayList<>();
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
                                            List<Historikkinnslag2DokumentLink> dokumentLinker,
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

    private Historikkinnslag2DokumentLink lagHistorikkInnslagDokumentLink(ArkivDokument arkivDokument,
                                                                          JournalpostId journalpostId,
                                                                          String linkTekst) {
        var historikkinnslagDokumentLink = new Historikkinnslag2DokumentLink();
        historikkinnslagDokumentLink.setDokumentId(arkivDokument.getDokumentId());
        historikkinnslagDokumentLink.setJournalpostId(journalpostId);
        historikkinnslagDokumentLink.setLinkTekst(linkTekst);
        return historikkinnslagDokumentLink;
    }

    public void opprettHistorikkinnslagForAutomatiskHenlegelsePgaNySøknad(Behandling behandling) {
        var historikk = new Historikkinnslag2.Builder().medTittel("Behandling er henlagt")
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .addTekstlinje("Mottatt ny søknad")
            .build();

        historikkinnslagRepository.lagre(historikk);
    }

    public void opprettHistorikkinnslagForVedlegg(Fagsak fagsak, JournalpostId journalpostId, DokumentTypeId dokumentTypeId, boolean elektronisk) {
        var dokumenter = lagDokumenterLenker(BehandlingType.UDEFINERT, journalpostId, elektronisk,
            DokumentTypeId.INNTEKTSMELDING.equals(dokumentTypeId));
        historikkinnslagRepository.lagre(new Historikkinnslag2.Builder().medTittel("Vedlegg mottatt")
            .medFagsakId(fagsak.getId())
            .medAktør(DokumentTypeId.INNTEKTSMELDING.equals(dokumentTypeId) ? HistorikkAktør.ARBEIDSGIVER : HistorikkAktør.SØKER)
            .medDokumenter(dokumenter)
            .build());
    }

    public void opprettHistorikkinnslagForVenteFristRelaterteInnslag(Behandling behandling,
                                                                     HistorikkinnslagType historikkinnslagType,
                                                                     LocalDateTime frist,
                                                                     Venteårsak venteårsak) {
        var tittel = frist == null ? historikkinnslagType.getNavn() :
            historikkinnslagType.getNavn() + " " + format(frist.toLocalDate());
        var build = new Historikkinnslag2.Builder().medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel(tittel)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId());
        if (!Venteårsak.UDEFINERT.equals(venteårsak)) {
            build.addTekstlinje(venteårsak.getNavn());
        }
        historikkinnslagRepository.lagre(build.build());
    }

    public void opprettHistorikkinnslagForBehandlingOppdatertMedNyeOpplysninger(Behandling behandling, BehandlingÅrsakType behandlingÅrsakType) {
        var historikkinnslag = new Historikkinnslag2.Builder().medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel("Behandlingen oppdatert med nye opplysninger")
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .addTekstlinje(behandlingÅrsakType.getNavn())
            .build();

        historikkinnslagRepository.lagre(historikkinnslag);
    }

    public void opprettHistorikkinnslagForEndringshendelse(Fagsak fagsak, String begrunnelse) {
        var historikkinnslag = new Historikkinnslag2.Builder().medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medTittel("Behandlingen oppdatert med nye opplysninger")
            .medFagsakId(fagsak.getId())
            .addTekstlinje(begrunnelse)
            .build();

        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
