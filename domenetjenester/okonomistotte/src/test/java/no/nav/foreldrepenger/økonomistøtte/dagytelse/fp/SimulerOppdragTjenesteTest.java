package no.nav.foreldrepenger.økonomistøtte.dagytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Sats;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeEndringLinje;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.TypeSats;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.OppdragInputTjeneste;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomistøtte.SimulerOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.Input;
import no.nav.foreldrepenger.økonomistøtte.ny.tjeneste.NyOppdragskontrollTjenesteImpl;
import no.nav.foreldrepenger.økonomistøtte.ny.toggle.OppdragKjerneimplementasjonToggle;

@CdiDbAwareTest
public class SimulerOppdragTjenesteTest {
    @Mock
    private OppdragInputTjeneste oppdragInputTjeneste;
    @Mock
    private OppdragskontrollTjeneste oppdragskontrollTjeneste;
    @Mock
    NyOppdragskontrollTjenesteImpl nyOppdragskontrollTjeneste;
    @Mock
    private OppdragKjerneimplementasjonToggle toggle;

    @Test
    public void simulerOppdrag_uten_behandling_vedtak_FP() {
        // Arrange

        var saksnummer = new Saksnummer("100000001");
        Oppdragskontroll oppdragskontroll = lagOppdragskontroll(saksnummer);
        var o110 = lagOppdrag110(oppdragskontroll, saksnummer);
        buildOppdragslinje150(o110);
        lenient().when(oppdragskontrollTjeneste.simulerOppdrag(anyLong())).thenReturn(Optional.ofNullable(oppdragskontroll));

        when(toggle.brukNyImpl(any())).thenReturn(false);

        var simulerOppdragTjeneste = new SimulerOppdragTjeneste(oppdragskontrollTjeneste, null, nyOppdragskontrollTjeneste, oppdragInputTjeneste, toggle);

        // Act
        var resultat = simulerOppdragTjeneste.simulerOppdrag(1L, FagsakYtelseType.FORELDREPENGER);

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
            .medAvstemming(Avstemming.ny())
            .medOppdragskontroll(oppdragskontroll)
            .medFagSystemId(Long.parseLong(saksnummer.getVerdi()))
            .build();
    }

    private Oppdragslinje150 buildOppdragslinje150(Oppdrag110 oppdrag110) {
        var o150 = Oppdragslinje150.builder()
            .medKodeEndringLinje(KodeEndringLinje.NY)
            .medDatoStatusFom(LocalDate.now())
            .medVedtakId("345")
            .medDelytelseId(1L)
            .medVedtakFomOgTom(LocalDate.now(), LocalDate.now())
            .medSats(Sats.på(1122L))
            .medUtbetalesTilId("123456789")
            .medKodeKlassifik(KodeKlassifik.FPF_ARBEIDSTAKER)
            .medTypeSats(TypeSats.DAGLIG)
            .medOppdrag110(oppdrag110)
            .build();
        return o150;
    }
}
