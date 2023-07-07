package no.nav.foreldrepenger.økonomistøtte.simulering.klient;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Ompostering116;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Utbetalingsgrad;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.KodeEndring;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.KodeEndringLinje;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.KodeFagområde;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.KodeKlassifik;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.KodeStatusLinje;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.LukketPeriode;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.Ompostering116Dto;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.Oppdrag110Dto;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.OppdragskontrollDto;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.Oppdragslinje150Dto;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.Refusjonsinfo156Dto;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.SatsDto;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.TypeSats;
import no.nav.foreldrepenger.kontrakter.fpwsproxy.simulering.request.UtbetalingsgradDto;

public class OppdragsKontrollDtoMapper {

    private OppdragsKontrollDtoMapper() {
    }

    public static OppdragskontrollDto tilDto(@NotNull Oppdragskontroll oppdragskontroll) {
        return new OppdragskontrollDto(oppdragskontroll.getBehandlingId(), tilOppdragListe(oppdragskontroll.getOppdrag110Liste()));
    }

    private static List<Oppdrag110Dto> tilOppdragListe(List<Oppdrag110> oppdrag110Liste) {
        return safeStream(oppdrag110Liste)
            .map(OppdragsKontrollDtoMapper::tilOppdrag110Dto)
            .toList();
    }

    private static Oppdrag110Dto tilOppdrag110Dto(Oppdrag110 oppdrag110s) {
        return new Oppdrag110Dto(
            KodeEndring.valueOf(oppdrag110s.getKodeEndring().name()),
            KodeFagområde.valueOf(oppdrag110s.getKodeFagomrade().name()),
            oppdrag110s.getFagsystemId(),
            oppdrag110s.getOppdragGjelderId(),
            oppdrag110s.getSaksbehId(),
            tilOmpostering116Dto(oppdrag110s.getOmpostering116()),
            tilOppdragLinje150Dto(oppdrag110s.getOppdragslinje150Liste())
        );
    }

    private static List<Oppdragslinje150Dto> tilOppdragLinje150Dto(List<Oppdragslinje150> oppdragslinje150Liste) {
        return safeStream(oppdragslinje150Liste)
            .map(OppdragsKontrollDtoMapper::tilOppdragLinje150Dto)
            .toList();
    }

    private static Oppdragslinje150Dto tilOppdragLinje150Dto(Oppdragslinje150 oppdragslinje150) {
        return new Oppdragslinje150Dto(
            KodeEndringLinje.valueOf(oppdragslinje150.getKodeEndringLinje().name()),
            oppdragslinje150.getVedtakId(),
            oppdragslinje150.getDelytelseId(),
            KodeKlassifik.valueOf(oppdragslinje150.getKodeKlassifik().name()),
            new LukketPeriode(oppdragslinje150.getDatoVedtakFom(), oppdragslinje150.getDatoVedtakTom()),
            tilSatsDto(oppdragslinje150.getSats()),
            TypeSats.valueOf(oppdragslinje150.getTypeSats().name()),
            tilUtbetalingsgradDto(oppdragslinje150.getUtbetalingsgrad()),
            oppdragslinje150.getKodeStatusLinje() != null ? KodeStatusLinje.valueOf(oppdragslinje150.getKodeStatusLinje().name()) : null,
            oppdragslinje150.getDatoStatusFom(),
            oppdragslinje150.getUtbetalesTilId(),
            oppdragslinje150.getRefDelytelseId(),
            oppdragslinje150.getRefFagsystemId(),
            tilRefusjonsinfo156Dto(oppdragslinje150.getRefusjonsinfo156())
        );
    }

    private static Refusjonsinfo156Dto tilRefusjonsinfo156Dto(Refusjonsinfo156 refusjonsinfo156) {
        if (refusjonsinfo156 == null) {
            return null;
        }
        return new Refusjonsinfo156Dto(
            refusjonsinfo156.getMaksDato(),
            refusjonsinfo156.getRefunderesId(),
            refusjonsinfo156.getDatoFom()
        );
    }

    private static UtbetalingsgradDto tilUtbetalingsgradDto(Utbetalingsgrad utbetalingsgrad) {
        if (utbetalingsgrad == null) {
            return null;
        }
        return new UtbetalingsgradDto(utbetalingsgrad.getVerdi());
    }

    private static SatsDto tilSatsDto(Sats sats) {
        return new SatsDto(sats.getVerdi());
    }

    private static Ompostering116Dto tilOmpostering116Dto(Optional<Ompostering116> ompostering116Opt) {
        if (ompostering116Opt.isEmpty()) {
            return null;
        }
        var ompostering116 = ompostering116Opt.get();
        return new Ompostering116Dto(
            ompostering116.getOmPostering(),
            ompostering116.getDatoOmposterFom(),
            ompostering116.getTidspktReg()
        );
    }

    public static <T> Stream<T> safeStream(List<T> list) {
        return ((List)Optional.ofNullable(list).orElseGet(List::of)).stream();
    }
}
