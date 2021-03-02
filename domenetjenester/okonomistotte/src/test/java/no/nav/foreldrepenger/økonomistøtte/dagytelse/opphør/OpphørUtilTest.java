package no.nav.foreldrepenger.økonomistøtte.dagytelse.opphør;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class OpphørUtilTest {

    Saksnummer saksnummer = new Saksnummer("100000000");

    KodeKlassifik FPATORD = KodeKlassifik.fraKode("FPATORD");
    KodeKlassifik FPATAL = KodeKlassifik.fraKode("FPATAL");

    LocalDate fom = LocalDate.of(2020, 1, 1);
    LocalDate tom = LocalDate.of(2020, 2, 1);

    Long HENVISNING_1 = 1L;
    Long HENVISNING_2 = 2L;

    @Test
    public void skal_kunne_håndtere_flere_feriepenge_koder_gjennom_flere_år() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        long fagsystemId = 100000000100L;
        var maien = LocalDate.of(2020,5,1);
        Oppdrag110 oppdrag1 = lagOppdrag110(oppdragskontroll, KodeFagområde.FORELDREPENGER_BRUKER, KodeEndring.NY, fagsystemId);
        oppdrag1.setOpprettetTidspunkt(LocalDateTime.now().minusDays(1));
        lagOppdragslinje(oppdrag1, KodeKlassifik.FERIEPENGER_BRUKER, maien, maien.withDayOfMonth(31), fagsystemId * 1000 + 101, HENVISNING_1, null, fagsystemId);
        lagOppdragslinje(oppdrag1, KodeKlassifik.FERIEPENGER_BRUKER, maien.plusYears(1), maien.plusYears(1).withDayOfMonth(31), fagsystemId * 1000 + 102, HENVISNING_1, null, fagsystemId);
        lagOppdragslinje(oppdrag1, KodeKlassifik.FPF_FERIEPENGER_AG, maien.plusYears(1), maien.plusYears(1).withDayOfMonth(31), fagsystemId * 1000 + 103, HENVISNING_1, null, fagsystemId);
        lagOppdragslinje(oppdrag1, KodeKlassifik.FPF_FERIEPENGER_AG, maien, maien.withDayOfMonth(31), fagsystemId * 1000 + 104, HENVISNING_1, null, fagsystemId);
        lagOppdragslinje(oppdrag1, KodeKlassifik.SVP_FERIEPENGER_AG, maien.plusYears(1), maien.plusYears(1).withDayOfMonth(31), fagsystemId * 1000 + 105, HENVISNING_1, null, fagsystemId);

        Oppdrag110 oppdrag2 = lagOppdrag110(oppdragskontroll, KodeFagområde.FORELDREPENGER_BRUKER, KodeEndring.ENDRING, fagsystemId);
        oppdrag2.setOpprettetTidspunkt(LocalDateTime.now());
        lagOppdragslinje(oppdrag2, KodeKlassifik.FERIEPENGER_BRUKER, maien, maien.withDayOfMonth(31), fagsystemId * 1000 + 106, HENVISNING_1, fagsystemId * 1000 + 101, fagsystemId);
        lagOppdragslinje(oppdrag2, KodeKlassifik.FERIEPENGER_BRUKER, maien.plusYears(1), maien.plusYears(1).withDayOfMonth(31), fagsystemId * 1000 + 107, HENVISNING_1, fagsystemId * 1000 + 103, fagsystemId);

        Oppdrag110 oppdrag3 = lagOppdrag110(oppdragskontroll, KodeFagområde.FORELDREPENGER_BRUKER, KodeEndring.ENDRING, fagsystemId);
        oppdrag3.setOpprettetTidspunkt(LocalDateTime.now().plusDays(1));
        lagOpphørslinje(oppdrag3, KodeKlassifik.FERIEPENGER_BRUKER, maien, maien.withDayOfMonth(31), fagsystemId * 1000 + 106, maien, HENVISNING_2, fagsystemId * 1000 + 101, fagsystemId);

        Set<KodeKlassifik> ikkeOpphørteKlassekoder = OpphørUtil.finnKlassekoderSomIkkeErOpphørt(Arrays.asList(oppdrag1, oppdrag2, oppdrag3));
        Assertions.assertThat(ikkeOpphørteKlassekoder).containsAll(Arrays.asList(KodeKlassifik.FPF_FERIEPENGER_AG, KodeKlassifik.SVP_FERIEPENGER_AG, KodeKlassifik.FERIEPENGER_BRUKER));
    }

    @Test
    public void skal_si_at_iverksatte_linjer_er_opphørbare() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        long fagsystemId = 100000000100L;
        Oppdrag110 oppdrag110 = lagOppdrag110(oppdragskontroll, KodeFagområde.FORELDREPENGER_BRUKER, KodeEndring.NY, fagsystemId);
        oppdrag110.setOpprettetTidspunkt(LocalDateTime.now());

        lagOppdragslinje(oppdrag110, FPATORD, fom, tom, fagsystemId * 1000 + 101, HENVISNING_1, null, fagsystemId);
        lagOppdragslinje(oppdrag110, FPATAL, fom, tom, fagsystemId * 1000 + 102, HENVISNING_1, null, fagsystemId);

        Set<KodeKlassifik> ikkeOpphørteKlassekoder = OpphørUtil.finnKlassekoderSomIkkeErOpphørt(Arrays.asList(oppdrag110));
        Assertions.assertThat(ikkeOpphørteKlassekoder).containsOnly(FPATORD, FPATAL);
    }

    @Test
    public void skal_si_at_allrede_opphørt_klassekode_ikke_er_opphørbar() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        long fagsystemId = 100000000100L;
        Oppdrag110 oppdrag1 = lagOppdrag110(oppdragskontroll, KodeFagområde.FORELDREPENGER_BRUKER, KodeEndring.NY, fagsystemId);
        oppdrag1.setOpprettetTidspunkt(LocalDateTime.now());

        lagOppdragslinje(oppdrag1, FPATORD, fom, tom, fagsystemId * 1000 + 101, HENVISNING_1, null, fagsystemId);
        lagOppdragslinje(oppdrag1, FPATAL, fom, tom, fagsystemId * 1000 + 102, HENVISNING_1, null, fagsystemId);

        Oppdrag110 oppdrag2 = lagOppdrag110(oppdragskontroll, KodeFagområde.FORELDREPENGER_BRUKER, KodeEndring.NY, fagsystemId);
        oppdrag2.setOpprettetTidspunkt(oppdrag1.getOpprettetTidspunkt().plusMinutes(1));
        lagOpphørslinje(oppdrag2, FPATAL, fom, tom, fagsystemId * 1000 + 102, fom, HENVISNING_2, null, fagsystemId);

        Set<KodeKlassifik> ikkeOpphørteKlassekoder = OpphørUtil.finnKlassekoderSomIkkeErOpphørt(Arrays.asList(oppdrag1, oppdrag2));
        Assertions.assertThat(ikkeOpphørteKlassekoder).containsOnly(FPATORD);
    }

    @Test
    public void skal_si_at_klassekode_som_ikke_er_fullstendig_opphørt_fortsatt_er_opphørbar() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        long fagsystemId = 100000000100L;
        Oppdrag110 oppdrag1 = lagOppdrag110(oppdragskontroll, KodeFagområde.FORELDREPENGER_BRUKER, KodeEndring.NY, fagsystemId);
        oppdrag1.setOpprettetTidspunkt(LocalDateTime.now());
        lagOppdragslinje(oppdrag1, FPATORD, fom, tom, fagsystemId * 1000 + 101, HENVISNING_1, null, fagsystemId);

        Oppdrag110 oppdrag2 = lagOppdrag110(oppdragskontroll, KodeFagområde.FORELDREPENGER_BRUKER, KodeEndring.NY, fagsystemId);
        oppdrag2.setOpprettetTidspunkt(oppdrag1.getOpprettetTidspunkt().plusMinutes(1));
        lagOpphørslinje(oppdrag2, FPATORD, fom, tom, fagsystemId * 1000 + 101, fom.plusDays(1), HENVISNING_2, null, fagsystemId);

        Set<KodeKlassifik> ikkeOpphørteKlassekoder = OpphørUtil.finnKlassekoderSomIkkeErOpphørt(Arrays.asList(oppdrag1, oppdrag2));
        Assertions.assertThat(ikkeOpphørteKlassekoder).containsOnly(FPATORD);
    }

    private Oppdragslinje150 lagOpphørslinje(Oppdrag110 oppdrag110, KodeKlassifik klassekode, LocalDate fom, LocalDate tom, Long delytelseId,
                                             LocalDate opphørsdatao, Long henvisning, Long refDelytelseId, Long refFagsystemId) {
        return lagOppdragslinjeBuilder(oppdrag110, klassekode, fom, tom, delytelseId, henvisning, refDelytelseId, refFagsystemId)
                .medKodeEndringLinje(KodeEndringLinje.ENDRING)
                .medKodeStatusLinje(KodeStatusLinje.OPPHØR)
                .medDatoStatusFom(opphørsdatao)
                .build();
    }

    private Oppdragslinje150 lagOppdragslinje(Oppdrag110 oppdrag110, KodeKlassifik klassekode, LocalDate fom, LocalDate tom, Long delytelseId,
                                              Long henvisning, Long refDelytelseId, Long refFagsystemId) {
        return lagOppdragslinjeBuilder(oppdrag110, klassekode, fom, tom, delytelseId, henvisning, refDelytelseId, refFagsystemId)
                .build();
    }

    private Oppdragslinje150.Builder lagOppdragslinjeBuilder(Oppdrag110 oppdrag110, KodeKlassifik klassekode, LocalDate fom, LocalDate tom,
                                                             Long delytelseId, Long henvisning, Long refDelyteseId, Long refFagsystemId) {
        return Oppdragslinje150.builder()
                .medKodeEndringLinje(KodeEndringLinje.NY)
                .medKodeKlassifik(klassekode)
                .medVedtakFomOgTom(fom, tom)
                .medSats(Sats.på(1L))
                .medTypeSats(TypeSats.DAGLIG)
                .medOppdrag110(oppdrag110)
                .medDelytelseId(delytelseId)
                .medRefDelytelseId(refDelyteseId)
                .medRefFagsystemId(refFagsystemId);
    }

    private Oppdrag110 lagOppdrag110(Oppdragskontroll oppdragskontroll, KodeFagområde fagområde, KodeEndring kodeEndring, long fagsystemId) {
        return Oppdrag110.builder()
                .medKodeFagomrade(fagområde)
                .medKodeEndring(kodeEndring)
                .medFagSystemId(fagsystemId)
                .medOppdragGjelderId("11111111111")
                .medSaksbehId("Z111111")
                .medAvstemming(Avstemming.ny())
                .medOppdragskontroll(oppdragskontroll)
                .build();
    }

    private Oppdragskontroll lagOppdragskontroll(Saksnummer saksnummer) {
        return Oppdragskontroll.builder()
                .medBehandlingId(1L)
                .medSaksnummer(saksnummer)
                .medVenterKvittering(false)
                .medProsessTaskId(-1L)
                .build();
    }

}
