package no.nav.foreldrepenger.behandling.kabal;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.anke.AnkeVurderingTjeneste;
import no.nav.foreldrepenger.behandling.klage.KlageVurderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;

@ApplicationScoped
public class KabalTjeneste {

    private AnkeVurderingTjeneste ankeVurderingTjeneste;
    private KlageVurderingTjeneste klageVurderingTjeneste;
    private BehandlingRepository behandlingRepository;
    private KabalDokumenter kabalDokumenter;
    private VergeRepository vergeRepository;
    private PersoninfoAdapter personinfoAdapter;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private KabalKlient kabalKlient;

    KabalTjeneste() {
        // for CDI proxy
    }

    @Inject
    public KabalTjeneste(PersoninfoAdapter personinfoAdapter,
                         KabalKlient kabalKlient,
                         BehandlingRepository behandlingRepository,
                         KabalDokumenter kabalDokumenter,
                         VergeRepository vergeRepository,
                         AnkeVurderingTjeneste ankeVurderingTjeneste,
                         KlageVurderingTjeneste klageVurderingTjeneste,
                         BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                         HistorikkinnslagRepository historikkinnslagRepository) {
        this.personinfoAdapter = personinfoAdapter;
        this.ankeVurderingTjeneste = ankeVurderingTjeneste;
        this.klageVurderingTjeneste = klageVurderingTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.kabalDokumenter = kabalDokumenter;
        this.vergeRepository = vergeRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
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
        var klageMottattDato  = kabalDokumenter.utledDokumentMottattDato(klageBehandling);
        var sakenGjelder = utledSakenGjelder(klageBehandling);
        var fullmektig = utledFullmektig(klageBehandling, Optional.of(resultat.getKlageResultat()));
        var dokumentReferanser = kabalDokumenter.finnDokumentReferanserForKlage(klageBehandling.getId(),
            klageBehandling.getSaksnummer(), resultat.getKlageResultat(), brukHjemmel);
        var request = TilKabalDto.klage(klageBehandling, sakenGjelder, fullmektig, enhet, dokumentReferanser,
            klageMottattDato, List.of(brukHjemmel.getKabal()), resultat.getBegrunnelse());
        kabalKlient.sendTilKabal(request);
    }

    public void lagreKlageUtfallFraKabal(Behandling behandling, BehandlingLås lås, KabalUtfall utfall) {
        var builder = klageVurderingTjeneste.hentKlageVurderingResultatBuilder(behandling, KlageVurdertAv.NK)
            .medKlageVurdering(klageVurderingFraUtfall(utfall))
            .medKlageVurderingOmgjør(klageVurderingOmgjørFraUtfall(utfall));
        klageVurderingTjeneste.oppdaterBekreftetVurderingAksjonspunkt(behandling, lås, builder, KlageVurdertAv.NK);
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
        var builder = ankeVurderingTjeneste.hentAnkeVurderingResultatBuilder(behandling);
        // OBS det kan forekomme flere oversendelser til Trygderetten (retur-tilfelle). Oppdater vurdering. Behold tidligste oversendelsesdato
        // Denne er essensiell for AnkeMerknaderSteg
        if (sendtTrygderetten != null && (gjeldendeSendtTrygderetten.isEmpty() || sendtTrygderetten.isBefore(gjeldendeSendtTrygderetten.get()))) {
            builder.medSendtTrygderettDato(sendtTrygderetten);
        }
        if (sendtTrygderetten == null && gjeldendeAnkeVurdering.isPresent() && gjeldendeSendtTrygderetten.isPresent()) {
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
            // Knoteri fra Kabal
            // - AnkeOpprettet kommer med en kabalref og en direkte AnkeAvsluttet har samme kabalRef
            // - AnkeOpprettet kommer med en kabalref og AnkeTrygderett vil komme med en ny kabalRef - som presumptivt kommer i AnkeAvsluttet
            var alleÅpneAnker = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(behandling.getFagsakId()).stream()
                .filter(b -> BehandlingType.ANKE.equals(b.getType()))
                .filter(b -> !b.erSaksbehandlingAvsluttet())
                .toList();
            return alleÅpneAnker.stream()
                .filter(b -> ankeVurderingTjeneste.hentAnkeResultat(b).getPåAnketKlageBehandlingId().filter(k -> k.equals(behandlingId)).isPresent())
                .findFirst()
                .or(() -> alleÅpneAnker.stream()
                    .filter(b -> {
                        var resultatReferanse = ankeVurderingTjeneste.hentAnkeResultat(b).getKabalReferanse();
                        return resultatReferanse == null || Objects.equals(kabalReferanse, resultatReferanse);
                    })
                    .findFirst());
        } else if (BehandlingType.ANKE.equals(behandling.getType())) {
            return Optional.of(behandling);
        } else {
            return Optional.empty();
        }
    }

    public boolean gjelderÅpenAnkeDenneKlagen(Behandling ankeBehandling, Behandling klageBehandling) {
        var påAnketKlageForAnke = ankeVurderingTjeneste.hentAnkeResultatHvisEksisterer(ankeBehandling)
            .flatMap(AnkeResultatEntitet::getPåAnketKlageBehandlingId).orElse(null);
        return Objects.equals(påAnketKlageForAnke, klageBehandling.getId());
    }

    private String utledEnhet(Fagsak fagsak) {
        return behandlendeEnhetTjeneste.finnBehandlendeEnhetFra(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow()).enhetId();
    }

    private TilKabalDto.Klager utledSakenGjelder(Behandling behandling) {
        var klagerPart = utledPartId(behandling.getAktørId());
        return new TilKabalDto.Klager(klagerPart);
    }

    private TilKabalDto.Fullmektig utledFullmektig(Behandling behandling, Optional<KlageResultatEntitet> resultat) {
        var verge = vergeRepository.hentAggregat(behandling.getId())
            .or(() -> resultat.flatMap(KlageResultatEntitet::getPåKlagdBehandlingId).flatMap(b -> vergeRepository.hentAggregat(b)))
            .flatMap(VergeAggregat::getVerge);
        if (verge.flatMap(VergeEntitet::getVergeOrganisasjon).isPresent()) {
            var vergeOrganisasjon = verge.flatMap(VergeEntitet::getVergeOrganisasjon).orElseThrow();
            var partId = new TilKabalDto.PartId(TilKabalDto.PartsType.VIRKSOMHET, vergeOrganisasjon.getOrganisasjonsnummer());
            return new TilKabalDto.Fullmektig(partId, vergeOrganisasjon.getNavn());
        } else if (verge.flatMap(VergeEntitet::getBruker).isPresent()) {
            var vergeBruker = verge.flatMap(VergeEntitet::getBruker).orElseThrow();
            return new TilKabalDto.Fullmektig(utledPartId(vergeBruker.getAktørId()), null);
        } else {
            return null;
        }
    }

    private TilKabalDto.PartId utledPartId(AktørId aktørId) {
        return new TilKabalDto.PartId(TilKabalDto.PartsType.PERSON,
            personinfoAdapter.hentFnr(aktørId).map(PersonIdent::getIdent).orElseThrow());
    }

    private void opprettHistorikkinnslagKlage(Behandling behandling, KabalUtfall utfall) {
        var klageVurdering = klageVurderingFraUtfall(utfall);
        var klageVurderingOmgjør = klageVurderingOmgjørFraUtfall(utfall);
        var resultat = KlageVurderingTjeneste.historikkResultatForKlageVurdering(klageVurdering, KlageVurdertAv.NK, klageVurderingOmgjør);

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medTittel("Klagebehandling KA")
            .addLinje(fraTilEquals("Ytelsesvedtak", null, resultat))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private void opprettHistorikkinnslagAnke(Behandling behandling, KabalUtfall utfall) {
        var ankeVurdering = ankeVurderingFraUtfall(utfall);
        var ankeVurderingOmgjør = ankeVurderingOmgjørFraUtfall(utfall);
        var resultat = AnkeVurderingTjeneste.konverterAnkeVurderingTilResultatTekst(ankeVurdering, ankeVurderingOmgjør);

        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .medBehandlingId(behandling.getId())
            .medFagsakId(behandling.getFagsakId())
            .medTittel("Ankebehandling")
            .addLinje(fraTilEquals("Anke resultat", null, resultat))
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private KlageVurdering klageVurderingFraUtfall(KabalUtfall utfall) {
        return switch (utfall) {
            case STADFESTELSE, INNSTILLING_STADFESTELSE -> KlageVurdering.STADFESTE_YTELSESVEDTAK;
            case AVVIST, INNSTILLING_AVVIST -> KlageVurdering.AVVIS_KLAGE;
            case OPPHEVET -> KlageVurdering.OPPHEVE_YTELSESVEDTAK;
            case MEDHOLD, DELVIS_MEDHOLD, UGUNST -> KlageVurdering.MEDHOLD_I_KLAGE;
            case RETUR, TRUKKET, HEVET, MEDHOLD_ETTER_FVL_35 -> throw new IllegalStateException("Utviklerfeil forsøker lagre klage med utfall " + utfall);
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
            case STADFESTELSE, INNSTILLING_STADFESTELSE -> AnkeVurdering.ANKE_STADFESTE_YTELSESVEDTAK;
            case AVVIST, INNSTILLING_AVVIST -> AnkeVurdering.ANKE_AVVIS;
            case OPPHEVET -> AnkeVurdering.ANKE_OPPHEVE_OG_HJEMSENDE;
            case MEDHOLD, DELVIS_MEDHOLD, UGUNST -> AnkeVurdering.ANKE_OMGJOER;
            case RETUR, TRUKKET, HEVET, MEDHOLD_ETTER_FVL_35 -> throw new IllegalStateException("Utviklerfeil forsøker lagre anke med utfall " + utfall);
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

    public void lagHistorikkinnslagForBrevSendt(Behandling behandling, JournalpostId journalpostId) {
        kabalDokumenter.lagHistorikkinnslagForBrevSendt(behandling, journalpostId);
    }
}
