package no.nav.foreldrepenger.behandling.kabal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentBestiltEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class KabalDokumenter {

    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private HistorikkRepository historikkRepository;

    KabalDokumenter() {
        // for CDI proxy
    }

    @Inject
    public KabalDokumenter(DokumentArkivTjeneste dokumentArkivTjeneste,
                           MottatteDokumentRepository mottatteDokumentRepository,
                           BehandlingDokumentRepository behandlingDokumentRepository,
                           HistorikkRepository historikkRepository) {
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.historikkRepository = historikkRepository;
    }

    LocalDate utledDokumentMottattDato(Behandling behandling) {
        return finnMottattDokumentFor(behandling.getId(), erKlageEllerAnkeDokument())
            .map(MottattDokument::getMottattDato)
            .min(Comparator.naturalOrder())
            .orElseGet(() -> behandling.getOpprettetDato().toLocalDate());
    }

    List<TilKabalDto.DokumentReferanse> finnDokumentReferanserForKlage(long behandlingId, Saksnummer saksnummer, KlageResultatEntitet resultat, KlageHjemmel hjemmel) {
        List<TilKabalDto.DokumentReferanse> referanser = new ArrayList<>();

        opprettReferanseFraBestilltDokument(behandlingId, erKlageOversendtBrevSent(), referanser,
            TilKabalDto.DokumentReferanseType.OVERSENDELSESBREV);

        resultat.getPåKlagdBehandlingId()
            .ifPresent(b -> opprettReferanseFraBestilltDokument(b, erVedtakDokument(), referanser,
                TilKabalDto.DokumentReferanseType.OPPRINNELIG_VEDTAK));

        resultat.getPåKlagdBehandlingId()
            .ifPresent(b -> opprettReferanseFraBestilltDokument(b, erKlageAvvist(), referanser,
                TilKabalDto.DokumentReferanseType.OPPRINNELIG_VEDTAK));

        opprettReferanseFraMottattDokument(behandlingId, erKlageEllerAnkeDokument(), referanser, TilKabalDto.DokumentReferanseType.BRUKERS_KLAGE);

        resultat.getPåKlagdBehandlingId()
            .ifPresent(
                b -> opprettReferanseFraMottattDokument(b, erSøknadDokument(), referanser, TilKabalDto.DokumentReferanseType.BRUKERS_SOEKNAD));

        if (KlageHjemmel.TILBAKE.equals(hjemmel) || resultat.getPåKlagdEksternBehandlingUuid().isPresent()) {
            var utgåendeTilbake = dokumentArkivTjeneste.hentAlleUtgåendeJournalposterForSak(saksnummer).stream()
                .filter(d -> d.tittel() != null && (d.tittel().startsWith("Vedtak tilbakebetaling") || d.tittel().startsWith("Varsel tilbakebetaling")))
                .toList();
            utgåendeTilbake.stream().filter(d -> d.tittel().startsWith("Vedtak tilbakebetaling"))
                .forEach(d -> referanser.add(new TilKabalDto.DokumentReferanse(d.journalpostId().getVerdi(), TilKabalDto.DokumentReferanseType.OPPRINNELIG_VEDTAK)));
            utgåendeTilbake.stream().filter(d -> d.tittel().startsWith("Varsel tilbakebetaling"))
                .forEach(d -> referanser.add(new TilKabalDto.DokumentReferanse(d.journalpostId().getVerdi(), TilKabalDto.DokumentReferanseType.ANNET)));
        }

        return referanser;
    }

    List<TilKabalDto.DokumentReferanse> finnDokumentReferanserForAnke(long behandlingId, AnkeResultatEntitet resultat, boolean bleKlageBehandletKabal) {
        List<TilKabalDto.DokumentReferanse> referanser = new ArrayList<>();

        opprettReferanseFraMottattDokument(behandlingId, erKlageEllerAnkeDokument(), referanser, TilKabalDto.DokumentReferanseType.BRUKERS_KLAGE);

        if (!bleKlageBehandletKabal) {
            resultat.getPåAnketKlageBehandlingId()
                .ifPresent(b -> opprettReferanseFraMottattDokument(b, erKlageEllerAnkeDokument(), referanser,
                    TilKabalDto.DokumentReferanseType.BRUKERS_KLAGE));

            resultat.getPåAnketKlageBehandlingId()
                .ifPresent(b -> opprettReferanseFraBestilltDokument(b, erKlageOversendtBrevSent(), referanser,
                    TilKabalDto.DokumentReferanseType.OVERSENDELSESBREV));

            resultat.getPåAnketKlageBehandlingId()
                .ifPresent(b -> opprettReferanseFraBestilltDokument(b, erKlageVedtakDokument(), referanser,
                    TilKabalDto.DokumentReferanseType.KLAGE_VEDTAK));
        }

        return referanser;
    }

    private void opprettReferanseFraMottattDokument(long behandlingId,
                                                    Predicate<MottattDokument> mottattDokumentPredicate,
                                                    List<TilKabalDto.DokumentReferanse> referanser,
                                                    TilKabalDto.DokumentReferanseType referanseType) {
        finnMottattDokumentFor(behandlingId, mottattDokumentPredicate)
            .map(MottattDokument::getJournalpostId)
            .filter(Objects::nonNull)
            .distinct()
            .forEach(j -> referanser.add(new TilKabalDto.DokumentReferanse(j.getVerdi(), referanseType)));
    }

    private Stream<MottattDokument> finnMottattDokumentFor(long behandlingId, Predicate<MottattDokument> filterPredicate) {
        return mottatteDokumentRepository.hentMottatteDokument(behandlingId)
            .stream()
            .filter(filterPredicate);
    }



    /**
     * Prøver å finne dokumentReferanse blant bestillte dokumenter i fpformidling - om refereansen ikke finnes
     * så skanner man gjennom historikk innslag til å finne riktig referanse der.
     * @param behandlingId - Behandling referanse.
     * @param bestilltDokumentPredicate - predicate filter til å filtrere riktig dokument fra bestillte dokumenter.
     * @param referanser - resultat list med referanser.
     * @param referanseType - Hva slags referanseType skal opprettes som resultat.
     */
    private void opprettReferanseFraBestilltDokument(long behandlingId,
                                                     Predicate<BehandlingDokumentBestiltEntitet> bestilltDokumentPredicate,
                                                     List<TilKabalDto.DokumentReferanse> referanser,
                                                     TilKabalDto.DokumentReferanseType referanseType) {
        hentBestilltDokumentFor(behandlingId, bestilltDokumentPredicate)
            .ifPresent(j -> referanser.add(new TilKabalDto.DokumentReferanse(j.getVerdi(), referanseType)));
    }

    private Optional<JournalpostId> hentBestilltDokumentFor(long behandlingId, Predicate<BehandlingDokumentBestiltEntitet> filterPredicate) {
        return behandlingDokumentRepository.hentHvisEksisterer(behandlingId)
            .map(BehandlingDokumentEntitet::getBestilteDokumenter)
            .orElse(List.of())
            .stream()
            .filter(filterPredicate)
            .map(BehandlingDokumentBestiltEntitet::getJournalpostId)
            .filter(Objects::nonNull)
            .findFirst();
    }

    private Predicate<MottattDokument> erSøknadDokument() {
        return MottattDokument::erSøknadsDokument;
    }

    private Predicate<MottattDokument> erKlageEllerAnkeDokument() {
        return d -> DokumentTypeId.KLAGE_DOKUMENT.equals(d.getDokumentType()) || DokumentKategori.KLAGE_ELLER_ANKE.equals(d.getDokumentKategori());
    }

    private Predicate<BehandlingDokumentBestiltEntitet> erKlageOversendtBrevSent() {
        return d -> d.getDokumentMalType() != null && DokumentMalType.erOversendelsesBrev(DokumentMalType.fraKode(d.getDokumentMalType()));
    }

    private Predicate<BehandlingDokumentBestiltEntitet> erVedtakDokument() {
        return d -> d.getDokumentMalType() != null && DokumentMalType.erVedtaksBrev(DokumentMalType.fraKode(d.getDokumentMalType()));
    }

    private Predicate<BehandlingDokumentBestiltEntitet> erKlageAvvist() {
        return d -> d.getDokumentMalType() != null && DokumentMalType.KLAGE_AVVIST.equals(DokumentMalType.fraKode(d.getDokumentMalType()));
    }

    private Predicate<BehandlingDokumentBestiltEntitet> erKlageVedtakDokument() {
        return d -> d.getDokumentMalType() != null && DokumentMalType.erKlageVedtaksBrev(DokumentMalType.fraKode(d.getDokumentMalType()));
    }

    public void lagHistorikkinnslagForBrevSendt(Behandling behandling, JournalpostId journalpostId) {
        var journalPost = dokumentArkivTjeneste.hentUtgåendeJournalpostForSak(journalpostId).orElse(null);
        if (journalPost == null) {
            return;
        }
        var historikkInnslag = new Historikkinnslag.Builder()
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medType(HistorikkinnslagType.BREV_SENT)
            .build();

        new HistorikkInnslagTekstBuilder().medHendelse(HistorikkinnslagType.BREV_SENT).medBegrunnelse("").build(historikkInnslag);

        var doklink = new HistorikkinnslagDokumentLink.Builder().medHistorikkinnslag(historikkInnslag)
            .medLinkTekst(journalPost.tittel())
            .medDokumentId(journalPost.dokumentId())
            .medJournalpostId(journalPost.journalpostId())
            .build();
        historikkInnslag.setDokumentLinker(List.of(doklink));

        historikkRepository.lagre(historikkInnslag);
    }

}
