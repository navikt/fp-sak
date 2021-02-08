package no.nav.foreldrepenger.økonomistøtte.kontantytelse.es;


import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinjeType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollManager;
import no.nav.foreldrepenger.økonomistøtte.OpprettOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.kontantytelse.es.adapter.MapBehandlingInfoES;
import no.nav.foreldrepenger.økonomistøtte.kontantytelse.es.wrapper.OppdragInputES;

@ApplicationScoped
public class OppdragskontrollEngangsstønad implements OppdragskontrollManager {

    private static final long INITIAL_VALUE = 99L;

    private static final String KODE_ENDRING_NY = ØkonomiKodeEndring.NY.name();
    private static final String KODE_ENDRING_UENDRET = ØkonomiKodeEndring.UEND.name();
    private static final TypeSats TYPE_SATS_ES = TypeSats.ENGANG;
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
        Oppdrag110 oppdrag110 = opprettOppdrag110ES(behandlingInfo, nyOppdragskontroll, forrigeOppdragOpt);
        opprettOppdragslinje150ES(behandlingInfo, oppdrag110, forrigeOppdragOpt);
        return nyOppdragskontroll;
    }

    private Oppdrag110 opprettOppdrag110ES(OppdragInputES behandlingInfo, Oppdragskontroll oppdragskontroll,
                                           Optional<Oppdrag110> forrigeOppdragOpt) {
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
            .medAvstemming(Avstemming.ny())
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
            .medKodeEndringLinje(KodeEndringLinjeType.NY)
            .medVedtakId(vedtaksdato.toString())
            .medDelytelseId(delytelseId)
            .medKodeKlassifik(kodeKlassifik)
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
        KodeEndringLinjeType kodeEndringLinje;
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
            kodeEndringLinje = KodeEndringLinjeType.ENDRING;
            kodeStatusLinje = KODE_STATUS_LINJE_OPPHØR;
            statusdato = tidligereDatoVedtakFom;
            refFagsystemId = null;
            refDelytelseId = null;
        } else { // Variant 2
            delytelseId = OpprettOppdragTjeneste.incrementInitialValue(forrigeOppdragslinje150.getDelytelseId());
            sats = behandlingInfo.getSats();
            kodeEndringLinje = KodeEndringLinjeType.NY;
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
            .medTypeSats(TYPE_SATS_ES)
            // FIXME (Tonic): Her brukes fnr, endres til aktørid ved ny versjon av oppdragsmelding
            .medUtbetalesTilId(behandlingInfo.getPersonIdent().getIdent())
            .medOppdrag110(oppdrag110)
            .medRefFagsystemId(refFagsystemId)
            .medRefDelytelseId(refDelytelseId)
            .build();
    }
}
