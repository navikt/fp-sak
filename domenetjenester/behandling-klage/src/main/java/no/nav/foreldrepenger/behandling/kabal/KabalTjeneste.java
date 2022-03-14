package no.nav.foreldrepenger.behandling.kabal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentBestiltEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
public class KabalTjeneste {

    private AnkeVurderingTjeneste ankeVurderingTjeneste;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private BehandlingDokumentRepository behandlingDokumentRepository;
    private VergeRepository vergeRepository;
    private PersoninfoAdapter personinfoAdapter;
    private HistorikkRepository historikkRepository;
    private KabalKlient kabalKlient;

    KabalTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KabalTjeneste(PersoninfoAdapter personinfoAdapter,
                         KabalKlient kabalKlient,
                         BehandlingRepository behandlingRepository,
                         MottatteDokumentRepository mottatteDokumentRepository,
                         BehandlingDokumentRepository behandlingDokumentRepository,
                         VergeRepository vergeRepository,
                         AnkeVurderingTjeneste ankeVurderingTjeneste,
                         KlageVurderingTjeneste klageVurderingTjeneste,
                         HistorikkRepository historikkRepository) {
        this.personinfoAdapter = personinfoAdapter;
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.vergeRepository = vergeRepository;
        this.historikkRepository = historikkRepository;
        this.kabalKlient = kabalKlient;
    }

    public void sentKlageTilKabal(Behandling behandling, KlageHjemmel hjemmel) {
        if (!BehandlingType.KLAGE.equals(behandling.getType())) {
            throw new IllegalArgumentException("Utviklerfeil: Prøver sende noe annet enn klage/anke til Kabal!");
        }
        var resultat = klageVurderingTjeneste.hentKlageVurderingResultat(behandling, KlageVurdertAv.NFP).orElseThrow();
        if (resultat.getKlageResultat().erBehandletAvKabal()) {
            // Reset flagg før ny oversendelse
            klageVurderingTjeneste.oppdaterKlageMedKabalReferanse(behandling.getId(), null);
        }
        var brukHjemmel = Optional.ofNullable(hjemmel)
            .or(() -> Optional.ofNullable(resultat.getKlageHjemmel()))
            .orElseGet(() -> KlageHjemmel.standardHjemmelForYtelse(behandling.getFagsakYtelseType()));
        if (KlageHjemmel.UDEFINERT.equals(brukHjemmel)) {
            throw new TekniskException("FP-Hjemmel", "Utviklerfeil: Mangler hjemmel for behandling " + behandling.getId());
        }
        var enhet = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsakId())
            .map(Behandling::getBehandlendeEnhet).orElseThrow();
        var klageMottattDato  = utledDokumentMottattDato(behandling);
        var klager = utledKlager(behandling, resultat.getKlageResultat());
        var request = TilKabalDto.klage(behandling, klager, enhet, finnDokumentReferanser(behandling.getId(), resultat.getKlageResultat()),
            klageMottattDato, klageMottattDato, List.of(brukHjemmel.getKabal()), resultat.getBegrunnelse());
        kabalKlient.sendTilKabal(request);
    }

    public void lagreKlageUtfallFraKabal(Behandling behandling, KabalUtfall utfall) {
        var builder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(behandling, KlageVurdertAv.NK);
        switch (utfall) {
            case STADFESTELSE -> builder.medKlageVurdering(KlageVurdering.STADFESTE_YTELSESVEDTAK);
            case AVVIST -> builder.medKlageVurdering(KlageVurdering.AVVIS_KLAGE);
            case OPPHEVET -> builder.medKlageVurdering(KlageVurdering.OPPHEVE_YTELSESVEDTAK);
            case MEDHOLD -> builder.medKlageVurdering(KlageVurdering.MEDHOLD_I_KLAGE).medKlageVurderingOmgjør(KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE);
            case DELVIS_MEDHOLD -> builder.medKlageVurdering(KlageVurdering.MEDHOLD_I_KLAGE).medKlageVurderingOmgjør(KlageVurderingOmgjør.DELVIS_MEDHOLD_I_KLAGE);
            case UGUNST -> builder.medKlageVurdering(KlageVurdering.MEDHOLD_I_KLAGE).medKlageVurderingOmgjør(KlageVurderingOmgjør.UGUNST_MEDHOLD_I_KLAGE);
            default -> throw new IllegalStateException(String.format("Utviklerfeil forsøker lagre klage %s med utfall %s", behandling.getId(), utfall));
        }
        klageVurderingTjeneste.oppdaterBekreftetVurderingAksjonspunkt(behandling, builder.medGodkjentAvMedunderskriver(true), KlageVurdertAv.NK);
    }


    private TilKabalDto.Klager utledKlager(Behandling behandling, KlageResultatEntitet resultat) {
        var verge = vergeRepository.hentAggregat(behandling.getId())
            .or(() -> resultat.getPåKlagdBehandlingId().flatMap(b -> vergeRepository.hentAggregat(b)))
            .flatMap(VergeAggregat::getVerge)
            .map(v -> v.getVergeOrganisasjon().isPresent() ?
                new TilKabalDto.Part(TilKabalDto.PartsType.VIRKSOMHET, v.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getOrganisasjonsnummer).orElseThrow()) :
                new TilKabalDto.Part(TilKabalDto.PartsType.PERSON, personinfoAdapter.hentFnr(v.getBruker().getAktørId()).map(PersonIdent::getIdent).orElseThrow()))
            .map(p -> new TilKabalDto.Fullmektig(p, true));
        var klagerPart = new TilKabalDto.Part(TilKabalDto.PartsType.PERSON, personinfoAdapter.hentFnr(behandling.getAktørId()).map(PersonIdent::getIdent).orElseThrow());
        return new TilKabalDto.Klager(klagerPart, verge.orElse(null));

    }

    private LocalDate utledDokumentMottattDato(Behandling behandling) {
        return finnMottattDokumentFor(behandling.getId(), erKlageEllerAnkeDokument())
            .map(MottattDokument::getMottattDato)
            .min(Comparator.naturalOrder())
            .orElseGet(() -> behandling.getOpprettetDato().toLocalDate());
    }

    private Stream<MottattDokument> finnMottattDokumentFor(long behandlingId, Predicate<MottattDokument> filterPredicate) {
        return mottatteDokumentRepository.hentMottatteDokument(behandlingId)
            .stream()
            .filter(filterPredicate);
    }

    List<TilKabalDto.DokumentReferanse> finnDokumentReferanser(long behandlingId, KlageResultatEntitet resultat) {
        List<TilKabalDto.DokumentReferanse> referanser = new ArrayList<>();

        opprettDokumentReferanseFor(behandlingId, TilKabalDto.DokumentReferanseType.OVERSENDELSESBREV, referanser, erKlageOversendtBrevSent(),
            erKlageOversendtHistorikkInnslagOpprettet());

        resultat.getPåKlagdBehandlingId()
            .ifPresent(b -> opprettDokumentReferanseFor(b, TilKabalDto.DokumentReferanseType.OPPRINNELIG_VEDTAK, referanser, erVedtakDokument(),
                erVedtakHistorikkInnslagOpprettet()));

        opprettDokumentReferanseFor(behandlingId, TilKabalDto.DokumentReferanseType.BRUKERS_KLAGE, referanser, erKlageEllerAnkeDokument());

        resultat.getPåKlagdBehandlingId()
            .ifPresent(
                b -> opprettDokumentReferanseFor(b, TilKabalDto.DokumentReferanseType.BRUKERS_SOEKNAD, referanser, erSøknadDokument()));

        return referanser;
    }

    private void opprettDokumentReferanseFor(long behandlingId,
                                             TilKabalDto.DokumentReferanseType referanseType,
                                             List<TilKabalDto.DokumentReferanse> referanser,
                                             Predicate<MottattDokument> mottattDokumentPredicate) {
        finnMottattDokumentFor(behandlingId, mottattDokumentPredicate)
            .map(MottattDokument::getJournalpostId)
            .distinct()
            .forEach(opprettDokumentReferanse(referanser, referanseType));
    }

    /**
     * Prøver å finne dokumentReferanse blant bestillte dokumenter i fpformidling - om refereansen ikke finnes
     * så skanner man gjennom historikk innslag til å finne riktig referanse der.
     * @param behandlingId - Behandling referanse.
     * @param referanseType - Hva slags referanseType skal opprettes som resultat.
     * @param referanser - resultat list med referanser.
     * @param bestilltDokumentPredicate - predicate filter til å filtrere riktig dokument fra bestillte dokumenter.
     * @param historikkInnslagPredicate - predicate filter til å filtrere riktig dokument fra historikk innslag.
     */
    private void opprettDokumentReferanseFor(long behandlingId,
                                             TilKabalDto.DokumentReferanseType referanseType,
                                             List<TilKabalDto.DokumentReferanse> referanser,
                                             Predicate<BehandlingDokumentBestiltEntitet> bestilltDokumentPredicate,
                                             Predicate<HistorikkinnslagDokumentLink> historikkInnslagPredicate) {
        hentBestilltDokumentFor(behandlingId, bestilltDokumentPredicate)
            .or(() -> hentDokumentFraHistorikkFor(behandlingId, historikkInnslagPredicate))
            .ifPresent(opprettDokumentReferanse(referanser, referanseType));
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

    private Optional<JournalpostId> hentDokumentFraHistorikkFor(long behandlingId, Predicate<HistorikkinnslagDokumentLink> filterPredicate) {
        return historikkRepository.hentHistorikk(behandlingId)
            .stream()
            .flatMap(h -> h.getDokumentLinker().stream())
            .filter(filterPredicate)
            .map(HistorikkinnslagDokumentLink::getJournalpostId)
            .filter(Objects::nonNull)
            .distinct()
            .findFirst();
    }

    private Consumer<JournalpostId> opprettDokumentReferanse(List<TilKabalDto.DokumentReferanse> referanser,
                                                             TilKabalDto.DokumentReferanseType referanseType) {
        return j -> referanser.add(new TilKabalDto.DokumentReferanse(j.getVerdi(), referanseType));
    }

    private Predicate<MottattDokument> erSøknadDokument() {
        return MottattDokument::erSøknadsDokument;
    }

    private Predicate<MottattDokument> erKlageEllerAnkeDokument() {
        return d -> DokumentTypeId.KLAGE_DOKUMENT.equals(d.getDokumentType()) || DokumentKategori.KLAGE_ELLER_ANKE.equals(d.getDokumentKategori());
    }

    private Predicate<BehandlingDokumentBestiltEntitet> erKlageOversendtBrevSent() {
        return d -> DokumentMalType.KLAGE_OVERSENDT.getKode().equals(d.getDokumentMalType());
    }

    private Predicate<BehandlingDokumentBestiltEntitet> erVedtakDokument() {
        return d -> d.getDokumentMalType() != null && DokumentMalType.erVedtaksBrev(DokumentMalType.fraKode(d.getDokumentMalType()));
    }

    private Predicate<HistorikkinnslagDokumentLink> erKlageOversendtHistorikkInnslagOpprettet() {
        return d -> d.getLinkTekst().equals(DokumentMalType.KLAGE_OVERSENDT.getNavn());
    }

    private Predicate<HistorikkinnslagDokumentLink> erVedtakHistorikkInnslagOpprettet() {
        return d -> DokumentMalType.VEDTAKSBREV.stream().anyMatch(malType -> d.getLinkTekst().equals(malType.getNavn()));
    }
}
