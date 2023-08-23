package no.nav.foreldrepenger.økonomistøtte;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.*;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.*;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class OppdragTestDataHelper {

    private OppdragTestDataHelper() {
    }


    public static Oppdragskontroll oppdragskontrollMedOppdrag(Saksnummer saksnummer, Long behandlingsId) {
        var oppdragskontroll = oppdragskontrollUtenOppdrag(saksnummer, behandlingsId);
        lagOppdrag110FPArbeidsgiver(oppdragskontroll, 2L);
        return oppdragskontroll;
    }

    public static Oppdragskontroll oppdragskontrollUtenOppdrag() {
        return oppdragskontrollUtenOppdrag(new Saksnummer("3544"), 1L);
    }

    public static Oppdragskontroll oppdragskontrollUtenOppdrag(Saksnummer saksnummer, Long behandlingsId) {
        return Oppdragskontroll.builder()
            .medBehandlingId(behandlingsId)
            .medSaksnummer(saksnummer)
            .medVenterKvittering(true)
            .medProsessTaskId(52L)
            .build();
    }

    public static Oppdragskontroll oppdragskontrollUtenOppdrag(Saksnummer saksnummer, Long behandlingsId, Long prosesstaskId) {
        return Oppdragskontroll.builder()
            .medBehandlingId(behandlingsId)
            .medSaksnummer(saksnummer)
            .medVenterKvittering(true)
            .medProsessTaskId(prosesstaskId)
            .build();
    }


    public static Oppdrag110 lagOppdrag110ES(Oppdragskontroll oppdragskontroll, Long fagsystemId) {
        return lagOppdrag110(oppdragskontroll, fagsystemId, KodeFagområde.REFUTG, false, false, false);
    }

    public static Oppdrag110 lagOppdrag110FPBruker(Oppdragskontroll oppdragskontroll, Long fagsystemId) {
        return lagOppdrag110(oppdragskontroll, fagsystemId, KodeFagområde.FP, false, false, false);
    }

    public static Oppdrag110 lagOppdrag110FPArbeidsgiver(Oppdragskontroll oppdragskontroll, Long fagsystemId) {
        return lagOppdrag110(oppdragskontroll, fagsystemId, KodeFagområde.FPREF, true, true, false);
    }

    public static Oppdrag110 lagOppdrag110(Oppdragskontroll oppdragskontroll, Long fagsystemId, KodeFagområde kodeFagområde,
                                           boolean medOppdraglinje150, boolean refusjon, boolean feriepenger) {
        return lagOppdrag110(oppdragskontroll, fagsystemId, kodeFagområde, medOppdraglinje150, false, refusjon, feriepenger);
    }
    public static Oppdrag110 lagOppdrag110(Oppdragskontroll oppdragskontroll, Long fagsystemId, KodeFagområde kodeFagområde,
                                           boolean medOppdraglinje150, boolean medOmpostering, boolean refusjon, boolean feriepenger) {
        var oppdrag110 = Oppdrag110.builder()
            .medKodeEndring(KodeEndring.NY)
            .medKodeFagomrade(kodeFagområde)
            .medFagSystemId(fagsystemId)
            .medOppdragGjelderId("12345678901")
            .medSaksbehId("J5624215")
            .medAvstemming(Avstemming.ny())
            .medOppdragskontroll(oppdragskontroll)
            .medOmpostering116(medOmpostering ? new Ompostering116.Builder()
                .medOmPostering(true)
                .medDatoOmposterFom(LocalDate.now())
                .medTidspktReg(ØkonomistøtteUtils.tilSpesialkodetDatoOgKlokkeslett(LocalDateTime.now()))
                .build() : null)
            .build();
        if (medOppdraglinje150) {
            lagOppdragslinje150(oppdrag110, refusjon);
        }
        if (feriepenger) {
            lagOppdragslinje150FeriePenger(oppdrag110, refusjon);
        }
        return oppdrag110;
    }

    private static void lagOppdragslinje150FeriePenger(Oppdrag110 oppdrag110, boolean refusjon) {
        var kodeKlassifik = oppdrag110.getKodeFagomrade().equals(KodeFagområde.FP) ? KodeKlassifik.FERIEPENGER_BRUKER : KodeKlassifik.FPF_FERIEPENGER_AG;
        var oppdragslinje150 = lagOppdragsLinjeMinimal(oppdrag110, 100L)
            .medKodeKlassifik(kodeKlassifik)
            .medTypeSats(TypeSats.ENG)
            .build();
        if (refusjon && oppdrag110.getKodeFagomrade().equals(KodeFagområde.FPREF)) {
            lagRefusjonsinfo156(oppdragslinje150);
        }
    }

    public static void lagOppdragslinje150(Oppdrag110 oppdrag110, boolean refusjon) {
        lagOppdragslinje150(oppdrag110, 62L, refusjon);
    }

    public static void lagOppdragslinje150(Oppdrag110 oppdrag110, Long delytelseId, boolean refusjon) {
        var kodeKlassifik = switch (oppdrag110.getKodeFagomrade()) {
            case FP, FPREF -> refusjon ? KodeKlassifik.FPF_FERIEPENGER_AG : KodeKlassifik.FERIEPENGER_BRUKER;
            default -> KodeKlassifik.ES_FØDSEL;
        };
        var oppdragslinje150 = lagOppdragsLinjeMinimal(oppdrag110, delytelseId)
            .medKodeKlassifik(kodeKlassifik)
            .medTypeSats(TypeSats.DAG)
            .build();
        if (refusjon && oppdrag110.getKodeFagomrade().equals(KodeFagområde.FPREF)) {
            lagRefusjonsinfo156(oppdragslinje150);
        }
    }


    private static Oppdragslinje150.Builder lagOppdragsLinjeMinimal(Oppdrag110 oppdrag110, Long delytelseId) {
        return Oppdragslinje150.builder()
            .medKodeEndringLinje(KodeEndringLinje.ENDR)
            .medKodeStatusLinje(KodeStatusLinje.OPPH)
            .medDatoStatusFom(LocalDate.now())
            .medVedtakId("456")
            .medDelytelseId(delytelseId)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(Sats.på(61122L))
            .medUtbetalesTilId("123456789")
            .medUtbetalingsgrad(Utbetalingsgrad._100)
            .medOppdrag110(oppdrag110);
    }

    private static Refusjonsinfo156 lagRefusjonsinfo156(Oppdragslinje150 oppdragslinje150) {
        return Refusjonsinfo156.builder()
            .medMaksDato(LocalDate.now())
            .medDatoFom(LocalDate.now())
            .medRefunderesId("123456789")
            .medOppdragslinje150(oppdragslinje150)
            .build();
    }

}
