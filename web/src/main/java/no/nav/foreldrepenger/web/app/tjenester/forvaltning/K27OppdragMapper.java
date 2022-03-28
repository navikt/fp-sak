package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;

final class K27OppdragMapper {

    private K27OppdragMapper() {
    }

    public static void mapTil(Oppdragskontroll oppdragskontroll, Oppdrag110 gammelOppdrag, LocalDate maksDato) {
        var oppdrag110 = mapOppdrag110(gammelOppdrag, oppdragskontroll);
        for (var linje : gammelOppdrag.getOppdragslinje150Liste()) {
            mapOppdragslinje(oppdrag110, linje, maksDato);
        }
    }

    private static void mapOppdragslinje(Oppdrag110 oppdrag110, Oppdragslinje150 linje, LocalDate maksDato) {
        var builder = Oppdragslinje150.builder()
            .medOppdrag110(oppdrag110)
            .medVedtakFomOgTom(linje.getDatoVedtakFom(), linje.getDatoVedtakTom())
            .medSats(linje.getSats())
            .medTypeSats(linje.getTypeSats())
            .medDelytelseId(linje.getDelytelseId())
            .medRefDelytelseId(linje.getRefDelytelseId())
            .medRefFagsystemId(linje.getRefFagsystemId())
            .medVedtakId(linje.getVedtakId())
            .medKodeEndringLinje(linje.getKodeEndringLinje())
            .medKodeKlassifik(linje.getKodeKlassifik())
            .medUtbetalingsgrad(linje.getUtbetalingsgrad())
            .medUtbetalesTilId(linje.getUtbetalesTilId());

        if (linje.getDatoStatusFom() != null) {
            builder.medDatoStatusFom(linje.getDatoStatusFom());
            builder.medKodeStatusLinje(linje.getKodeStatusLinje());
        }

        var oppdragslinje = builder.build();

        if (linje.getRefusjonsinfo156() != null) {
            Refusjonsinfo156.builder()
                .medOppdragslinje150(oppdragslinje)
                .medRefunderesId(linje.getRefusjonsinfo156().getRefunderesId())
                .medDatoFom(linje.getRefusjonsinfo156().getDatoFom())
                .medMaksDato(maksDato)
                .build();
        }
    }

    private static Oppdrag110 mapOppdrag110(Oppdrag110 oppdrag110, Oppdragskontroll oppdragskontroll) {
        var nyOppdrag110 = Oppdrag110.builder()
            .medOppdragskontroll(oppdragskontroll)
            .medAvstemming(oppdrag110.getAvstemming())
            .medKodeEndring(KodeEndring.ENDR)
            .medKodeFagomrade(oppdrag110.getKodeFagomrade())
            .medOppdragGjelderId(oppdrag110.getOppdragGjelderId())
            .medFagSystemId(oppdrag110.getFagsystemId())
            .medSaksbehId(oppdrag110.getSaksbehId());
        oppdrag110.getOmpostering116().ifPresent(ompostering -> nyOppdrag110.medOmpostering116(ompostering));

        return nyOppdrag110.build();
    }
}
