package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.*;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import java.time.LocalDate;


public class OppdragTestDataHelper {

    static Oppdragslinje150 buildOppdragslinje150(Oppdrag110 oppdrag110) {
        var oppdrLinje150Builder = Oppdragslinje150.builder();

        return oppdrLinje150Builder
            .medKodeEndringLinje(KodeEndringLinje.ENDR)
            .medKodeStatusLinje(KodeStatusLinje.OPPH)
            .medDatoStatusFom(LocalDate.now())
            .medVedtakId("456")
            .medDelytelseId(64L)
            .medKodeKlassifik(KodeKlassifik.ES_FØDSEL)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(Sats.på(61122L))
            .medTypeSats(TypeSats.DAG)
            .medUtbetalesTilId("123456789")
            .medOppdrag110(oppdrag110)
            .medUtbetalingsgrad(Utbetalingsgrad._100)
            .build();

    }

    public static Oppdrag110 buildOppdrag110ES(Oppdragskontroll oppdragskontroll, Long fagsystemId) {
        return buildOppdrag110(oppdragskontroll, fagsystemId, KodeFagområde.REFUTG);
    }

    private static Oppdrag110 buildOppdrag110(Oppdragskontroll oppdragskontroll, Long fagsystemId, KodeFagområde økonomiKodeFagområde) {
        var oppdr110Builder = Oppdrag110.builder();

        var oppdrag110Builder = oppdr110Builder
            .medKodeEndring(KodeEndring.NY)
            .medKodeFagomrade(økonomiKodeFagområde)
            .medFagSystemId(fagsystemId)
            .medOppdragGjelderId("12345678901")
            .medSaksbehId("J5624215")
            .medAvstemming(Avstemming.ny())
            .medOppdragskontroll(oppdragskontroll);
        return oppdrag110Builder
            .build();
    }

    static void buildRefusjonsinfo156(Oppdragslinje150 oppdragslinje150) {
        var refusjonsinfo156Builder = Refusjonsinfo156.builder();

        refusjonsinfo156Builder
            .medMaksDato(LocalDate.now())
            .medDatoFom(LocalDate.now())
            .medRefunderesId("123456789")
            .medOppdragslinje150(oppdragslinje150)
            .build();
    }

    public static Oppdragskontroll buildOppdragskontroll() {
        return buildOppdragskontroll(new Saksnummer("999999"), 128L);
    }

    static Oppdragskontroll buildOppdragskontroll(Saksnummer saksnummer, long behandlingId) {
        return buildOppdragskontroll(saksnummer, behandlingId, 56L);
    }

    static Oppdragskontroll buildOppdragskontroll(Saksnummer saksnummer, long behandlingId, long prosessTaskId) {
        var oppdrkontrollBuilder = Oppdragskontroll.builder();

        return oppdrkontrollBuilder
            .medBehandlingId(behandlingId)
            .medSaksnummer(saksnummer)
            .medVenterKvittering(Boolean.TRUE)
            .medProsessTaskId(prosessTaskId)
            .build();
    }
}
