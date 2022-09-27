package no.nav.foreldrepenger.behandling.kabal;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentBestiltEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.BehandlingDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokumentUtgående;
import no.nav.foreldrepenger.dokumentbestiller.DokumentMalType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

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
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
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
                         BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                         HistorikkRepository historikkRepository) {
        this.personinfoAdapter = personinfoAdapter;
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingDokumentRepository = behandlingDokumentRepository;
        this.vergeRepository = vergeRepository;
        this.historikkRepository = historikkRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.kabalKlient = kabalKlient;
    }

    public void sendKlageTilKabal(Behandling klageBehandling, KlageHjemmel hjemmel) {
        if (!BehandlingType.KLAGE.equals(klageBehandling.getType())) {
            throw new IllegalArgumentException("Utviklerfeil: Prøver sende noe annet enn klage/anke til Kabal!");
        }
        var resultat = klageVurderingTjeneste.hentKlageVurderingResultat(klageBehandling, KlageVurdertAv.NFP).orElseThrow();
        var brukHjemmel = Optional.ofNullable(hjemmel)
            .or(() -> Optional.ofNullable(resultat.getKlageHjemmel()))
            .orElseGet(() -> KlageHjemmel.standardHjemmelForYtelse(klageBehandling.getFagsakYtelseType()));
        var enhet = utledEnhet(klageBehandling.getFagsak());
        var klageMottattDato  = utledDokumentMottattDato(klageBehandling);
        var klager = utledKlager(klageBehandling, Optional.of(resultat.getKlageResultat()));
        var sakMottattKaDato = klageBehandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_KA)
            .map(Aksjonspunkt::getOpprettetTidspunkt)
            .orElse(LocalDateTime.now());
        var request = TilKabalDto.klage(klageBehandling, klager, enhet, finnDokumentReferanserForKlage(klageBehandling.getId(), resultat.getKlageResultat()),
            klageMottattDato, klageMottattDato, sakMottattKaDato, List.of(brukHjemmel.getKabal()), resultat.getBegrunnelse());
        kabalKlient.sendTilKabal(request);
    }

    public void sendAnkeTilKabal(Behandling ankeBehandling, KlageHjemmel hjemmel) {
        if (!BehandlingType.ANKE.equals(ankeBehandling.getType())) {
            throw new IllegalArgumentException("Utviklerfeil: Prøver sende noe annet enn klage/anke til Kabal!");
        }
        var ankeResultat = ankeVurderingTjeneste.hentAnkeResultat(ankeBehandling);
        var klageBehandling = ankeResultat.getPåAnketKlageBehandlingId().map(behandlingRepository::hentBehandling);
        var klageResultat = klageBehandling.flatMap(kb -> klageVurderingTjeneste.hentKlageResultatHvisEksisterer(kb));
        var brukHjemmel = Optional.ofNullable(hjemmel)
            .orElseGet(() -> KlageHjemmel.standardHjemmelForYtelse(ankeBehandling.getFagsakYtelseType()));
        var enhet = utledEnhet(ankeBehandling.getFagsak());
        var ankeMottattDato  = utledDokumentMottattDato(ankeBehandling);
        var klager = utledKlager(ankeBehandling, klageResultat);
        var bleKlageBehandletKabal = klageResultat.filter(KlageResultatEntitet::erBehandletAvKabal).isPresent();
        var kildereferanse = klageBehandling.filter(k -> bleKlageBehandletKabal).orElse(ankeBehandling).getUuid().toString();
        var sakMottattKaDato = ankeBehandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE)
            .map(Aksjonspunkt::getOpprettetTidspunkt)
            .orElseGet(ankeBehandling::getOpprettetTidspunkt);
        var request = TilKabalDto.anke(ankeBehandling, kildereferanse, klager, enhet,
            finnDokumentReferanserForAnke(ankeBehandling.getId(), ankeResultat, bleKlageBehandletKabal),
            ankeMottattDato, ankeMottattDato, sakMottattKaDato, List.of(brukHjemmel.getKabal()));
        kabalKlient.sendTilKabal(request);
    }

    public void sendTrygderettTilKabal(Behandling ankeBehandling, KlageHjemmel hjemmel) {
        if (!BehandlingType.ANKE.equals(ankeBehandling.getType())) {
            throw new IllegalArgumentException("Utviklerfeil: Prøver sende noe annet enn klage/anke til Kabal!");
        }
        var ankeResultat = ankeVurderingTjeneste.hentAnkeResultat(ankeBehandling);
        var klageBehandling = ankeResultat.getPåAnketKlageBehandlingId().map(behandlingRepository::hentBehandling);
        var klageResultat = klageBehandling.flatMap(kb -> klageVurderingTjeneste.hentKlageResultatHvisEksisterer(kb));
        var brukHjemmel = Optional.ofNullable(hjemmel)
            .orElseGet(() -> KlageHjemmel.standardHjemmelForYtelse(ankeBehandling.getFagsakYtelseType()));
        var klager = utledKlager(ankeBehandling, klageResultat);
        var bleKlageBehandletKabal = klageResultat.filter(KlageResultatEntitet::erBehandletAvKabal).isPresent();
        var kildereferanse = klageBehandling.filter(k -> bleKlageBehandletKabal).orElse(ankeBehandling).getUuid().toString();
        var sakMottattKaDato = ankeBehandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_ANKE)
            .map(Aksjonspunkt::getOpprettetTidspunkt)
            .orElseGet(ankeBehandling::getOpprettetTidspunkt);
        var ankeVurdering = ankeVurderingTjeneste.hentAnkeVurderingResultat(ankeBehandling).orElseThrow();
        var sendtTilTrygderetten = Optional.ofNullable(ankeVurdering.getSendtTrygderettDato()).map(d -> d.atStartOfDay().plusHours(12))
            .orElseGet(LocalDateTime::now);
        var utfall = utfallFraAnkeVurdering(ankeVurdering.getAnkeVurdering(), ankeVurdering.getAnkeVurderingOmgjør());
        var request = TilKabalTRDto.anke(ankeBehandling, kildereferanse, klager,
            finnDokumentReferanserForAnke(ankeBehandling.getId(), ankeResultat, bleKlageBehandletKabal),
            sakMottattKaDato, sendtTilTrygderetten, utfall, List.of(brukHjemmel.getKabal()));
        kabalKlient.sendTilKabalTR(request);
    }

    public void lagreKlageUtfallFraKabal(Behandling behandling, KabalUtfall utfall) {
        var builder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(behandling, KlageVurdertAv.NK)
            .medGodkjentAvMedunderskriver(true)
            .medKlageVurdering(klageVurderingFraUtfall(utfall))
            .medKlageVurderingOmgjør(klageVurderingOmgjørFraUtfall(utfall));
        klageVurderingTjeneste.oppdaterBekreftetVurderingAksjonspunkt(behandling, builder, KlageVurdertAv.NK);
        opprettHistorikkinnslagKlage(behandling, utfall);
    }

    /**
     * Det er en del mulige scenarier her:
     * - TR-behandling (sendtTryderetten != null): Sette ankevurdering og sendtTrygderettenDato
     * - Avsluttet (sendtTrygdretten == null)
     *    o Dersom avr.ankevurdering og avr.sendt_trygderetten er satt - lagre tr-vurdering
     *    o Dersom avr.sendt_trygderetten ikke satt - lagre ankevurdering avhengig
     */
    public void lagreAnkeUtfallFraKabal(Behandling behandling, KabalUtfall utfall, LocalDate sendtTrygderetten) {
        var gjeldendeAnkeVurdering = ankeVurderingTjeneste.hentAnkeVurderingResultat(behandling)
            .map(AnkeVurderingResultatEntitet::getAnkeVurdering)
            .filter(av -> !AnkeVurdering.UDEFINERT.equals(av));
        var gjeldendeSendtTrygderetten = ankeVurderingTjeneste.hentAnkeVurderingResultat(behandling)
            .map(AnkeVurderingResultatEntitet::getSendtTrygderettDato);
        // Sjekke på meldinger for tilfelle vi selv har sendt til Kabal i en overgangsfase. Kan fjernes i oktober 2022
        if (sendtTrygderetten != null) {
            if (gjeldendeAnkeVurdering.isPresent() && gjeldendeSendtTrygderetten.isPresent()) {
                return;
            } else if (gjeldendeSendtTrygderetten.isPresent()) {
                throw new IllegalStateException("Hm, har satt sendtTrygderetten, men mangler KAs ankevurdering. Skal ikke skje");
            }
        }
        var builder = ankeVurderingTjeneste.hentAnkeVurderingResultatBuilder(behandling)
            .medGodkjentAvMedunderskriver(true);
        // Denne er essensiell for AnkeMerknaderSteg
        Optional.ofNullable(sendtTrygderetten).ifPresent(builder::medSendtTrygderettDato);
        // Dette med avsluttet skyldes prematur avslutning da kabal har sendt anke avsluttet for anker som er sendt Trygderetten. Fjernes etter patch (TODO (jol))
        if (gjeldendeAnkeVurdering.isPresent() && (gjeldendeSendtTrygderetten.isPresent() || behandling.erAvsluttet())) {
            builder.medTrygderettVurdering(ankeVurderingFraUtfall(utfall)).medTrygderettVurderingOmgjør(ankeVurderingOmgjørFraUtfall(utfall));
        } else {
            builder.medAnkeVurdering(ankeVurderingFraUtfall(utfall)).medAnkeVurderingOmgjør(ankeVurderingOmgjørFraUtfall(utfall));
        }
        ankeVurderingTjeneste.oppdaterBekreftetVurderingAksjonspunkt(behandling, builder);
        opprettHistorikkinnslagAnke(behandling, utfall);
    }

    public void fjerneKabalFlagg(Behandling behandling) {
        klageVurderingTjeneste.oppdaterKlageMedKabalReferanse(behandling.getId(), null);
    }

    public void settKabalReferanse(Behandling behandling, String kabalReferanse) {
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            klageVurderingTjeneste.oppdaterKlageMedKabalReferanse(behandling.getId(), kabalReferanse);
        } else if (BehandlingType.ANKE.equals(behandling.getType())) {
            ankeVurderingTjeneste.oppdaterAnkeMedKabalReferanse(behandling.getId(), kabalReferanse);
        } else {
            throw new IllegalArgumentException("Utviklerfeil: Kabaloppdatering av behandling med feil type " + behandling.getId());
        }
    }

    public void opprettNyttAnkeResultat(Behandling ankeBehandling, String ref, Behandling klageBehandling) {
        ankeVurderingTjeneste.hentAnkeResultat(ankeBehandling); // Vil opprette dersom mangler
        ankeVurderingTjeneste.oppdaterAnkeMedKabalReferanse(ankeBehandling.getId(), ref);
        ankeVurderingTjeneste.oppdaterAnkeMedPåanketKlage(ankeBehandling, klageBehandling.getId());
    }

    public Optional<Behandling> finnAnkeBehandling(Long behandlingId, String kabalReferanse) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            return behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(behandling.getFagsakId()).stream()
                .filter(b -> BehandlingType.ANKE.equals(b.getType()))
                .filter(b -> {
                    var resultatReferanse = ankeVurderingTjeneste.hentAnkeResultat(b).getKabalReferanse();
                    return resultatReferanse == null || Objects.equals(kabalReferanse, ankeVurderingTjeneste.hentAnkeResultat(b).getKabalReferanse());
                })
                .filter(b -> !b.erSaksbehandlingAvsluttet())
                .findFirst();
        } else if (BehandlingType.ANKE.equals(behandling.getType())) {
            return Optional.of(behandling);
        } else {
            return Optional.empty();
        }
    }

    private String utledEnhet(Fagsak fagsak) {
        var enhet = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .map(Behandling::getBehandlendeEnhet)
            .map(e -> "4847".equals(e) ? "4817" : e) // Hardkoder den som treffer regelmessig nå
            .orElseThrow();
        return behandlendeEnhetTjeneste.gyldigEnhetNfpNk(enhet) ? enhet : behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak).enhetId();
    }

    private TilKabalDto.Klager utledKlager(Behandling behandling, Optional<KlageResultatEntitet> resultat) {
        var verge = vergeRepository.hentAggregat(behandling.getId())
            .or(() -> resultat.flatMap(KlageResultatEntitet::getPåKlagdBehandlingId).flatMap(b -> vergeRepository.hentAggregat(b)))
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

    List<TilKabalDto.DokumentReferanse> finnDokumentReferanserForKlage(long behandlingId, KlageResultatEntitet resultat) {
        List<TilKabalDto.DokumentReferanse> referanser = new ArrayList<>();

        opprettReferanseFraBestilltDokument(behandlingId, erKlageOversendtBrevSent(), referanser,
            TilKabalDto.DokumentReferanseType.OVERSENDELSESBREV);

        resultat.getPåKlagdBehandlingId()
            .ifPresent(b -> opprettReferanseFraBestilltDokument(b, erVedtakDokument(), referanser,
                TilKabalDto.DokumentReferanseType.OPPRINNELIG_VEDTAK));

        opprettReferanseFraMottattDokument(behandlingId, erKlageEllerAnkeDokument(), referanser, TilKabalDto.DokumentReferanseType.BRUKERS_KLAGE);

        resultat.getPåKlagdBehandlingId()
            .ifPresent(
                b -> opprettReferanseFraMottattDokument(b, erSøknadDokument(), referanser, TilKabalDto.DokumentReferanseType.BRUKERS_SOEKNAD));

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
            .forEach(opprettDokumentReferanse(referanser, referanseType));
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
        return d -> d.getDokumentMalType() != null && DokumentMalType.erOversendelsesBrev(DokumentMalType.fraKode(d.getDokumentMalType()));
    }

    private Predicate<BehandlingDokumentBestiltEntitet> erVedtakDokument() {
        return d -> d.getDokumentMalType() != null && DokumentMalType.erVedtaksBrev(DokumentMalType.fraKode(d.getDokumentMalType()));
    }

    private Predicate<BehandlingDokumentBestiltEntitet> erKlageVedtakDokument() {
        return d -> d.getDokumentMalType() != null && DokumentMalType.erKlageVedtaksBrev(DokumentMalType.fraKode(d.getDokumentMalType()));
    }

    private void opprettHistorikkinnslagKlage(Behandling behandling, KabalUtfall utfall) {
        var klageVurdering = klageVurderingFraUtfall(utfall);
        var klageVurderingOmgjør = klageVurderingOmgjørFraUtfall(utfall);

        var resultat = KlageVurderingTjeneste.historikkResultatForKlageVurdering(klageVurdering, KlageVurdertAv.NK, klageVurderingOmgjør);

        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.KLAGE_BEH_NK)
            .medEndretFelt(HistorikkEndretFeltType.KLAGE_RESULTAT_KA, null, resultat.getNavn());
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.KLAGE_BEH_NK);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    private void opprettHistorikkinnslagAnke(Behandling behandling, KabalUtfall utfall) {
        var klageVurdering = ankeVurderingFraUtfall(utfall);
        var klageVurderingOmgjør = ankeVurderingOmgjørFraUtfall(utfall);

        var resultat = AnkeVurderingTjeneste.konverterAnkeVurderingTilResultatType(klageVurdering, klageVurderingOmgjør);

        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.ANKE_BEH)
            .medEndretFelt(HistorikkEndretFeltType.ANKE_RESULTAT, null, resultat.getNavn());
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.ANKE_BEH);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }

    private KlageVurdering klageVurderingFraUtfall(KabalUtfall utfall) {
        return switch (utfall) {
            case STADFESTELSE -> KlageVurdering.STADFESTE_YTELSESVEDTAK;
            case AVVIST -> KlageVurdering.AVVIS_KLAGE;
            case OPPHEVET -> KlageVurdering.OPPHEVE_YTELSESVEDTAK;
            case MEDHOLD, DELVIS_MEDHOLD, UGUNST -> KlageVurdering.MEDHOLD_I_KLAGE;
            default -> throw new IllegalStateException("Utviklerfeil forsøker lagre klage med utfall " + utfall);
        };
    }

    private KlageVurderingOmgjør klageVurderingOmgjørFraUtfall(KabalUtfall utfall) {
        return switch (utfall) {
            case MEDHOLD -> KlageVurderingOmgjør.GUNST_MEDHOLD_I_KLAGE;
            case DELVIS_MEDHOLD -> KlageVurderingOmgjør.DELVIS_MEDHOLD_I_KLAGE;
            case UGUNST -> KlageVurderingOmgjør.UGUNST_MEDHOLD_I_KLAGE;
            default -> KlageVurderingOmgjør.UDEFINERT;
        };
    }

    private AnkeVurdering ankeVurderingFraUtfall(KabalUtfall utfall) {
        return switch (utfall) {
            case STADFESTELSE -> AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK;
            case AVVIST -> AnkeVurdering.ANKE_AVVIS;
            case OPPHEVET -> AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE;
            case MEDHOLD, DELVIS_MEDHOLD, UGUNST -> AnkeVurdering.ANKE_OMGJOER;
            default -> throw new IllegalStateException("Utviklerfeil forsøker lagre klage med utfall " + utfall);
        };
    }

    private AnkeVurderingOmgjør ankeVurderingOmgjørFraUtfall(KabalUtfall utfall) {
        return switch (utfall) {
            case MEDHOLD -> AnkeVurderingOmgjør.ANKE_TIL_GUNST;
            case DELVIS_MEDHOLD -> AnkeVurderingOmgjør.ANKE_DELVIS_OMGJOERING_TIL_GUNST;
            case UGUNST -> AnkeVurderingOmgjør.ANKE_TIL_UGUNST;
            default -> AnkeVurderingOmgjør.UDEFINERT;
        };
    }

    private KabalUtfall utfallFraAnkeVurdering(AnkeVurdering utfall, AnkeVurderingOmgjør omgjør) {
        return switch (utfall) {
            case ANKE_STADFESTE_YTELSESVEDTAK -> KabalUtfall.STADFESTELSE;
            case ANKE_AVVIS -> KabalUtfall.AVVIST;
            case ANKE_OMGJOER -> switch (omgjør) {
                case ANKE_TIL_GUNST -> KabalUtfall.MEDHOLD;
                case ANKE_DELVIS_OMGJOERING_TIL_GUNST -> KabalUtfall.DELVIS_MEDHOLD;
                case ANKE_TIL_UGUNST -> KabalUtfall.UGUNST;
                case UDEFINERT -> throw new IllegalStateException("Utviklerfeil forsøker lagre klage med utfall " + utfall + " " + omgjør);
            };
            default -> throw new IllegalStateException("Utviklerfeil forsøker lagre klage med utfall " + utfall);
        };
    }

    public void lagHistorikkinnslagForHenleggelse(Long behandlingsId, BehandlingResultatType aarsak) {
        var builder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.AVBRUTT_BEH)
            .medÅrsak(aarsak);
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.AVBRUTT_BEH);
        historikkinnslag.setBehandlingId(behandlingsId);
        builder.build(historikkinnslag);

        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkRepository.lagre(historikkinnslag);
    }

    public void lagHistorikkinnslagForBrevSendt(Behandling behandling, ArkivDokumentUtgående journalPost) {
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
