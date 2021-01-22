package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Attestant180;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragsenhet120;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiTypeSats;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.integrasjon.økonomistøtte.oppdrag.TfradragTillegg;
import no.nav.foreldrepenger.økonomi.ny.toggle.OppdragKjerneimplementasjonToggle;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.SimulerOppdragTjeneste;

@CdiDbAwareTest
public class SimulerOppdragTjenesteTest {
    @Mock
    private OppdragskontrollTjeneste oppdragskontrollTjeneste;
    @Mock
    private OppdragKjerneimplementasjonToggle toggle;

    @Test
    public void simulerOppdrag_uten_behandling_vedtak_FP(EntityManager em) {
        // Arrange

        var saksnummer = new Saksnummer("100000001");
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        var o110 = lagOppdrag110(oppdragskontroll, saksnummer);
        buildOppdragsenhet120(o110);
        buildOppdragslinje150(o110);
        when(oppdragskontrollTjeneste.opprettOppdrag(anyLong(), anyLong())).thenReturn(Optional.ofNullable(oppdragskontroll));

        when(toggle.brukNyImpl(any())).thenReturn(false);

        var simulerOppdragTjeneste = new SimulerOppdragTjeneste(oppdragskontrollTjeneste, null, toggle);

        // Act
        var resultat = simulerOppdragTjeneste.simulerOppdrag(1L, 0L);

        // Assert
        assertThat(resultat).hasSize(1);
    }

    private Oppdragskontroll lagOppdragskontroll(Saksnummer saksnummer) {
        return Oppdragskontroll.builder()
            .medBehandlingId(1L)
            .medProsessTaskId(1000L)
            .medSaksnummer(saksnummer)
            .medVenterKvittering(true)
            .build();
    }

    private Oppdrag110 lagOppdrag110(Oppdragskontroll oppdragskontroll, Saksnummer saksnummer) {
        return Oppdrag110.builder()
            .medKodeAksjon("1")
            .medKodeEndring("NY")
            .medKodeFagomrade("FP")
            .medUtbetFrekvens("MND")
            .medOppdragGjelderId(saksnummer.getVerdi())
            .medDatoOppdragGjelderFom(LocalDate.MIN)
            .medSaksbehId("Z100000")
            .medOppdragskontroll(oppdragskontroll)
            .medFagSystemId(Long.parseLong(saksnummer.getVerdi()))
            .medAvstemming115(Avstemming115.builder()
                .medKodekomponent("FP")
                .medTidspnktMelding("nå")
                .medNokkelAvstemming("en nøkkel")
                .build())
            .build();
    }

    private Oppdragsenhet120 buildOppdragsenhet120(Oppdrag110 oppdrag110) {
        return Oppdragsenhet120.builder()
            .medTypeEnhet("BOS")
            .medEnhet("8020")
            .medDatoEnhetFom(LocalDate.now())
            .medOppdrag110(oppdrag110)
            .build();
    }

    private Oppdragslinje150 buildOppdragslinje150(Oppdrag110 oppdrag110) {
        var o150 = Oppdragslinje150.builder()
            .medKodeEndringLinje("NY")
            .medDatoStatusFom(LocalDate.now())
            .medVedtakId("345")
            .medDelytelseId(1L)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(1122L)
            .medFradragTillegg(TfradragTillegg.F.value())
            .medBrukKjoreplan("B")
            .medSaksbehId("F2365245")
            .medUtbetalesTilId("123456789")
            .medHenvisning(43L)
            .medKodeKlassifik(ØkonomiKodeKlassifik.FPATORD.getKodeKlassifik())
            .medTypeSats(ØkonomiTypeSats.DAG.name())
            .medOppdrag110(oppdrag110)
            .build();
        Attestant180.builder()
            .medOppdragslinje150(o150)
            .medAttestantId("1234")
            .build();
        return o150;
    }
}
