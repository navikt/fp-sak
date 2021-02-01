package no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiTypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManager;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OpprettOppdragTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.adapter.MapBehandlingInfoES;
import no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.wrapper.OppdragInputES;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomistøtteUtils;

@ApplicationScoped
public class OppdragskontrollEngangsstønad implements OppdragskontrollManager {

    private static final long INITIAL_VALUE = 99L;

    private static final String KODE_ENDRING_NY = ØkonomiKodeEndring.NY.name();
    private static final String KODE_ENDRING_UENDRET = ØkonomiKodeEndring.UEND.name();
    private static final String KODE_ENDRING_LINJE_NY = ØkonomiKodeEndringLinje.NY.name();
    private static final String KODE_ENDRING_LINJE_ENDRING = ØkonomiKodeEndringLinje.ENDR.name();
    private static final String FRADRAG_TILLEGG = TfradragTillegg.T.name();
    private static final String TYPE_SATS_ES = ØkonomiTypeSats.ENG.name();
    private static final String BRUK_KJOREPLAN = "N";
    private static final String KODE_STATUS_LINJE_OPPHØR = ØkonomiKodeStatusLinje.OPPH.name();

    private MapBehandlingInfoES mapBehandlingInfo;

    OppdragskontrollEngangsstønad() {
        // For CDI
    }

    @Inject
    public OppdragskontrollEngangsstønad(MapBehandlingInfoES mapBehandlingInfo) {
        this.mapBehandlingInfo = mapBehandlingInfo;
    }

    @Override
    public Oppdragskontroll opprettØkonomiOppdrag(Behandling behandling, Oppdragskontroll nyOppdragskontroll) {
        OppdragInputES behandlingInfo = mapBehandlingInfo.oppsettBehandlingInfo(behandling);
        Optional<Oppdrag110> forrigeOppdragOpt = behandlingInfo.getForrigeOppddragForSak();
        String nøkkelAvstemmingTidspunkt = ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now());
        Avstemming115 avstemming115 = OpprettOppdragTjeneste.opprettAvstemming115(nøkkelAvstemmingTidspunkt);
        Oppdrag110 oppdrag110 = opprettOppdrag110ES(behandlingInfo, nyOppdragskontroll, avstemming115, forrigeOppdragOpt, nøkkelAvstemmingTidspunkt);
        OpprettOppdragTjeneste.opprettOppdragsenhet120(oppdrag110);
        Oppdragslinje150 oppdragslinje150 = opprettOppdragslinje150ES(behandlingInfo, oppdrag110, forrigeOppdragOpt);
        OpprettOppdragTjeneste.opprettAttestant180(oppdragslinje150, behandlingInfo.getAnsvarligSaksbehandler());
        return nyOppdragskontroll;
    }

    private Oppdrag110 opprettOppdrag110ES(OppdragInputES behandlingInfo, Oppdragskontroll oppdragskontroll,
                                           Avstemming115 avstemming115, Optional<Oppdrag110> forrigeOppdragOpt, String nøkkelAvstemming) {
        long fagsystemId = OpprettOppdragTjeneste.genererFagsystemId(Long.parseLong(behandlingInfo.getSaksnummer().getVerdi()), INITIAL_VALUE);
        String kodeEndring = forrigeOppdragOpt.isPresent() ? KODE_ENDRING_UENDRET : KODE_ENDRING_NY;

        return Oppdrag110.builder()
            .medKodeAksjon(ØkonomiKodeAksjon.EN.getKodeAksjon())
            .medKodeEndring(kodeEndring)
            .medKodeFagomrade(ØkonomiKodeFagområde.REFUTG.name())
            .medFagSystemId(fagsystemId)
            .medUtbetFrekvens(ØkonomiUtbetFrekvens.MÅNED.getUtbetFrekvens())
            .medOppdragGjelderId(behandlingInfo.getPersonIdent().getIdent())
            .medDatoOppdragGjelderFom(LocalDate.of(2000, 1, 1))
            .medSaksbehId(behandlingInfo.getAnsvarligSaksbehandler())
            .medOppdragskontroll(oppdragskontroll)
            .medNøkkelAvstemming(nøkkelAvstemming)
            .medAvstemming115(avstemming115)
            .build();
    }

    private Oppdragslinje150 opprettOppdragslinje150ES(OppdragInputES behandlingInfo, Oppdrag110 oppdrag110, Optional<Oppdrag110> forrigeOppdragOpt) {
        String kodeKlassifik = behandlingInfo.getKodeKlassifik();

        // Her er det 3 varianter
        // 1) Første oppdrag i saken - alltid innvilgelse
        // 2) Endring med nytt innvilgelsesvedtak - ny linje som erstatter foregående
        // 3) Endring til avslag - sette opphørsstatus på foregående linje

        Oppdragslinje150 oppdrlinje150;
        if (forrigeOppdragOpt.isPresent()) {  // Endring, variant 2 eller 3
            Oppdrag110 forrigeOppdrag110 = forrigeOppdragOpt.get();
            oppdrlinje150 = opprettOppdragslinje150LinketTilForrigeOppdrag(behandlingInfo, oppdrag110, forrigeOppdrag110, kodeKlassifik);
        } else { // Variant 1
            oppdrlinje150 = opprettOppdragslinje150FørsteOppdragES(behandlingInfo, oppdrag110, kodeKlassifik);
        }
        return oppdrlinje150;
    }

    private Oppdragslinje150 opprettOppdragslinje150FørsteOppdragES(OppdragInputES behandlingInfo, Oppdrag110 oppdrag110, String kodeKlassifik) {
        LocalDate vedtaksdato = behandlingInfo.getVedtaksdato();
        long teller = OpprettOppdragTjeneste.incrementInitialValue(INITIAL_VALUE);
        long delytelseId = OpprettOppdragTjeneste.concatenateValues(oppdrag110.getFagsystemId(), teller);
        long satsEngangsstonad = behandlingInfo.getSats();

        Oppdragslinje150.Builder oppdragslinje150Builder = Oppdragslinje150.builder()
            .medKodeEndringLinje(KODE_ENDRING_LINJE_NY)
            .medVedtakId(vedtaksdato.toString())
            .medDelytelseId(delytelseId)
            .medKodeKlassifik(kodeKlassifik)
            .medFradragTillegg(FRADRAG_TILLEGG)
            .medBrukKjoreplan(BRUK_KJOREPLAN)
            .medSaksbehId(behandlingInfo.getAnsvarligSaksbehandler())
            .medHenvisning(behandlingInfo.getBehandling().getId())
            .medOppdrag110(oppdrag110)
            .medVedtakFomOgTom(vedtaksdato, vedtaksdato)
            .medSats(satsEngangsstonad)
            .medTypeSats(TYPE_SATS_ES)
            // FIXME (Tonic): Her brukes fnr, endres til aktørid ved ny versjon av oppdragsmelding
            .medUtbetalesTilId(behandlingInfo.getPersonIdent().getIdent());
        return oppdragslinje150Builder.build();
    }

    private Oppdragslinje150 opprettOppdragslinje150LinketTilForrigeOppdrag(OppdragInputES behandlingInfo, Oppdrag110 oppdrag110, Oppdrag110 forrigeOppdrag110, String kodeKlassifik) {
        Long delytelseId;
        long sats;
        String kodeEndringLinje;
        String kodeStatusLinje = null;
        Long refFagsystemId = forrigeOppdrag110.getFagsystemId();
        Oppdragslinje150 forrigeOppdragslinje150 = forrigeOppdrag110.getOppdragslinje150Liste()
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Det finnes ikke forrige oppdragslinje150 for behandling: " + behandlingInfo.getBehandling().getId()));
        Long refDelytelseId = forrigeOppdragslinje150.getDelytelseId();
        LocalDate tidligereDatoVedtakFom = forrigeOppdragslinje150.getDatoVedtakFom();
        LocalDate tidligereDatoVedtakTom = forrigeOppdragslinje150.getDatoVedtakTom();
        LocalDate vedtaksdato = behandlingInfo.getVedtaksdato();
        LocalDate statusdato = null;
        if (VedtakResultatType.AVSLAG.equals(behandlingInfo.getVedtakResultatType())) { // Variant 3
            delytelseId = forrigeOppdragslinje150.getDelytelseId();
            sats = behandlingInfo.getSatsFraTidligereBehandling();
            kodeEndringLinje = KODE_ENDRING_LINJE_ENDRING;
            kodeStatusLinje = KODE_STATUS_LINJE_OPPHØR;
            statusdato = tidligereDatoVedtakFom;
            refFagsystemId = null;
            refDelytelseId = null;
        } else { // Variant 2
            delytelseId = OpprettOppdragTjeneste.incrementInitialValue(forrigeOppdragslinje150.getDelytelseId());
            sats = behandlingInfo.getSats();
            kodeEndringLinje = KODE_ENDRING_LINJE_NY;
        }
        return Oppdragslinje150.builder()
            .medKodeEndringLinje(kodeEndringLinje)
            .medKodeStatusLinje(kodeStatusLinje)
            .medDatoStatusFom(statusdato)
            .medVedtakId(vedtaksdato.toString())
            .medDelytelseId(delytelseId)
            .medKodeKlassifik(kodeKlassifik)
            .medVedtakFomOgTom(tidligereDatoVedtakFom, tidligereDatoVedtakTom)
            .medSats(sats)
            .medFradragTillegg(FRADRAG_TILLEGG)
            .medTypeSats(TYPE_SATS_ES)
            .medBrukKjoreplan(BRUK_KJOREPLAN)
            .medSaksbehId(behandlingInfo.getAnsvarligSaksbehandler())
            // FIXME (Tonic): Fjern Fnr fra modellen.  Kan det slås opp ved oversending til Økonomi?
            // FIXME (Tonic): Her brukes fnr, endres til aktørid ved ny versjon av oppdragsmelding
            .medUtbetalesTilId(behandlingInfo.getPersonIdent().getIdent())
            .medHenvisning(behandlingInfo.getBehandling().getId())
            .medOppdrag110(oppdrag110)
            .medRefFagsystemId(refFagsystemId)
            .medRefDelytelseId(refDelytelseId)
            .build();
    }
}
