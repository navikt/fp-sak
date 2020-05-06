package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeAksjon;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeStatusLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodekomponent;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiTypeSats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiUtbetFrekvens;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;

public class OpphørUtilTest {

    Saksnummer saksnummer = new Saksnummer("100000000");

    ØkonomiKodeKlassifik FPATORD = ØkonomiKodeKlassifik.fraKode("FPATORD");
    ØkonomiKodeKlassifik FPATAL = ØkonomiKodeKlassifik.fraKode("FPATAL");

    LocalDate fom = LocalDate.of(2020, 1, 1);
    LocalDate tom = LocalDate.of(2020, 2, 1);

    Long HENVISNING_1 = 1L;
    Long HENVISNING_2 = 2L;

    @Test
    public void skal_si_at_iverksatte_linjer_er_opphørbare() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        long fagsystemId = 100000000100L;
        Oppdrag110 oppdrag110 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.NY, fagsystemId);
        oppdrag110.setOpprettetTidspunkt(LocalDateTime.now());

        lagOppdragslinje(oppdrag110, FPATORD, fom, tom, fagsystemId * 1000 + 101, HENVISNING_1);
        lagOppdragslinje(oppdrag110, FPATAL, fom, tom, fagsystemId * 1000 + 102, HENVISNING_1);

        Set<ØkonomiKodeKlassifik> ikkeOpphørteKlassekoder = OpphørUtil.finnKlassekoderSomIkkeErOpphørt(Arrays.asList(oppdrag110));
        Assertions.assertThat(ikkeOpphørteKlassekoder).containsOnly(FPATORD, FPATAL);
    }

    @Test
    public void skal_si_at_allrede_opphørt_klassekode_ikke_er_opphørbar() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        long fagsystemId = 100000000100L;
        Oppdrag110 oppdrag1 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.NY, fagsystemId);
        oppdrag1.setOpprettetTidspunkt(LocalDateTime.now());

        lagOppdragslinje(oppdrag1, FPATORD, fom, tom, fagsystemId * 1000 + 101, HENVISNING_1);
        lagOppdragslinje(oppdrag1, FPATAL, fom, tom, fagsystemId * 1000 + 102, HENVISNING_1);

        Oppdrag110 oppdrag2 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.NY, fagsystemId);
        oppdrag2.setOpprettetTidspunkt(oppdrag1.getOpprettetTidspunkt().plusMinutes(1));
        lagOpphørslinje(oppdrag2, FPATAL, fom, tom, fagsystemId * 1000 + 102, fom, HENVISNING_2);

        Set<ØkonomiKodeKlassifik> ikkeOpphørteKlassekoder = OpphørUtil.finnKlassekoderSomIkkeErOpphørt(Arrays.asList(oppdrag1, oppdrag2));
        Assertions.assertThat(ikkeOpphørteKlassekoder).containsOnly(FPATORD);
    }

    @Test
    public void skal_si_at_klassekode_som_ikke_er_fullstendig_opphørt_fortsatt_er_opphørbar() {
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        long fagsystemId = 100000000100L;
        Oppdrag110 oppdrag1 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.NY, fagsystemId);
        oppdrag1.setOpprettetTidspunkt(LocalDateTime.now());
        lagOppdragslinje(oppdrag1, FPATORD, fom, tom, fagsystemId * 1000 + 101, HENVISNING_1);

        Oppdrag110 oppdrag2 = lagOppdrag110(oppdragskontroll, ØkonomiKodeFagområde.FP, ØkonomiKodeEndring.NY, fagsystemId);
        oppdrag2.setOpprettetTidspunkt(oppdrag1.getOpprettetTidspunkt().plusMinutes(1));
        lagOpphørslinje(oppdrag2, FPATORD, fom, tom, fagsystemId * 1000 + 101, fom.plusDays(1), HENVISNING_2);

        Set<ØkonomiKodeKlassifik> ikkeOpphørteKlassekoder = OpphørUtil.finnKlassekoderSomIkkeErOpphørt(Arrays.asList(oppdrag1, oppdrag2));
        Assertions.assertThat(ikkeOpphørteKlassekoder).containsOnly(FPATORD);
    }

    private Oppdragslinje150 lagOpphørslinje(Oppdrag110 oppdrag110, ØkonomiKodeKlassifik klassekode, LocalDate fom, LocalDate tom, Long delytelseId, LocalDate opphørsdatao, Long henvisning) {
        return lagOppdragslinjeBuilder(oppdrag110, klassekode, fom, tom, delytelseId, henvisning)
            .medKodeEndringLinje(ØkonomiKodeEndringLinje.ENDR.name())
            .medKodeStatusLinje(ØkonomiKodeStatusLinje.OPPH.name())
            .medDatoStatusFom(opphørsdatao)
            .build();
    }

    private Oppdragslinje150 lagOppdragslinje(Oppdrag110 oppdrag110, ØkonomiKodeKlassifik klassekode, LocalDate fom, LocalDate tom, Long delytelseId, Long henvisning) {
        return lagOppdragslinjeBuilder(oppdrag110, klassekode, fom, tom, delytelseId, henvisning)
            .build();
    }

    private Oppdragslinje150.Builder lagOppdragslinjeBuilder(Oppdrag110 oppdrag110, ØkonomiKodeKlassifik klassekode, LocalDate fom, LocalDate tom, Long delytelseId, Long henvisning) {
        return Oppdragslinje150.builder()
            .medKodeEndringLinje(ØkonomiKodeEndringLinje.NY.name())
            .medFradragTillegg(TfradragTillegg.T.value())
            .medKodeKlassifik(klassekode.getKodeKlassifik())
            .medVedtakFomOgTom(fom, tom)
            .medSats(1L)
            .medTypeSats(ØkonomiTypeSats.DAG.name())
            .medBrukKjoreplan("N")
            .medSaksbehId("Z11111")
            .medHenvisning(henvisning)
            .medOppdrag110(oppdrag110)
            .medDelytelseId(delytelseId);
    }

    private Oppdrag110 lagOppdrag110(Oppdragskontroll oppdragskontroll, ØkonomiKodeFagområde fagområde, ØkonomiKodeEndring status, long fagsystemId) {
        return Oppdrag110.builder()
            .medKodeAksjon(ØkonomiKodeAksjon.EN.getKodeAksjon())
            .medKodeFagomrade(fagområde.name())
            .medKodeEndring(status.name())
            .medFagSystemId(fagsystemId)
            .medUtbetFrekvens(ØkonomiUtbetFrekvens.MÅNED.getUtbetFrekvens())
            .medOppdragGjelderId("11111111111")
            .medDatoOppdragGjelderFom(LocalDate.of(2020, 1, 1))
            .medSaksbehId("Z111111")
            .medOppdragskontroll(oppdragskontroll)
            .medAvstemming115(Avstemming115.builder()
                .medNokkelAvstemming("foo")
                .medKodekomponent(ØkonomiKodekomponent.VLFP.getKodekomponent())
                .medTidspnktMelding("nå")
                .build())
            .build();
    }

    private Oppdragskontroll lagOppdragskontroll(Saksnummer saksnummer) {
        return Oppdragskontroll.builder()
            .medBehandlingId(1L)
            .medSaksnummer(saksnummer)
            .medVenterKvittering(false)
            .build();
    }


}
