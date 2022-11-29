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
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.simulering.klient.OppdragsKontrollDtoMapper;

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
        var oppdragskontroll = OppdragTestDataHelper.oppdragskontrollMedOppdrag(saksnummer, 1L);
        when(oppdragInputTjeneste.lagSimuleringInput(anyLong())).thenReturn(mockInput(saksnummer));
        when(oppdragskontrollTjeneste.simulerOppdrag(any(OppdragInput.class))).thenReturn(Optional.ofNullable(oppdragskontroll));

        var simulerOppdragTjeneste = new SimulerOppdragTjeneste(oppdragskontrollTjeneste, oppdragInputTjeneste);

        // Act
        var oppdragskontrollReturnert = simulerOppdragTjeneste.hentOppdragskontrollForBehandling(1L);
        var resultat = OppdragsKontrollDtoMapper.tilDto(oppdragskontrollReturnert.get());

        // Assert
        assertThat(resultat.oppdrag()).hasSize(1);
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
}
