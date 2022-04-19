package no.nav.foreldrepenger.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndring;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.OppdragInput;

@ExtendWith(MockitoExtension.class)
public class SimulerOppdragTjenesteFPTest {
    @Mock
    private OppdragInputTjeneste oppdragInputTjeneste;
    @Mock
    private OppdragskontrollTjeneste oppdragskontrollTjeneste;

    @Test
    public void simulerOppdrag_uten_behandling_vedtak_FP() {
        // Arrange
        var saksnummer = new Saksnummer("100000001");
        var oppdragskontroll = lagOppdragskontroll(saksnummer);
        var o110 = lagOppdrag110(oppdragskontroll, saksnummer);
        buildOppdragslinje150(o110);
        when(oppdragInputTjeneste.lagSimuleringInput(anyLong())).thenReturn(mockInput(saksnummer));
        when(oppdragskontrollTjeneste.simulerOppdrag(any(OppdragInput.class))).thenReturn(Optional.ofNullable(oppdragskontroll));

        var simulerOppdragTjeneste = new SimulerOppdragTjeneste(oppdragskontrollTjeneste, oppdragInputTjeneste);

        // Act
        var resultat = simulerOppdragTjeneste.simulerOppdrag(1L);

        // Assert
        assertThat(resultat).hasSize(1);
    }

    private OppdragInput mockInput(final Saksnummer saksnummer) {
        return OppdragInput.builder()
            .medTilkjentYtelse(GruppertYtelse.TOM)
            .medTidligereOppdrag(OverordnetOppdragKjedeOversikt.TOM)
            .medSaksnummer(saksnummer)
            .medBehandlingId(1L)
            .medFagsakYtelseType(FagsakYtelseType.FORELDREPENGER)
            .medVedtaksdato(LocalDate.now())
            .medBrukerFnr("123456789")
            .build();
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
            .medKodeEndring(KodeEndring.NY)
            .medKodeFagomrade(KodeFagområde.FP)
            .medOppdragGjelderId(saksnummer.getVerdi())
            .medSaksbehId("Z100000")
            .medAvstemming(Avstemming.ny())
            .medOppdragskontroll(oppdragskontroll)
            .medFagSystemId(Long.parseLong(saksnummer.getVerdi()))
            .build();
    }

    private Oppdragslinje150 buildOppdragslinje150(Oppdrag110 oppdrag110) {
        return Oppdragslinje150.builder()
            .medKodeEndringLinje(KodeEndringLinje.NY)
            .medVedtakId("345")
            .medDelytelseId(1L)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(Sats.på(1122L))
            .medTypeSats(TypeSats.DAG)
            .medUtbetalesTilId("123456789")
            .medKodeKlassifik(KodeKlassifik.FPF_ARBEIDSTAKER)
            .medOppdrag110(oppdrag110)
            .build();
    }
}
