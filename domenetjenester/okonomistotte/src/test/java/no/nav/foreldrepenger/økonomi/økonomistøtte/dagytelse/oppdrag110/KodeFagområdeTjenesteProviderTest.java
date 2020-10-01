package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdrag110;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelse;

public class KodeFagområdeTjenesteProviderTest {

    private OppdragInput behandlingInfo = mock(OppdragInput.class);
    private Oppdrag110 oppdrag110 = mock(Oppdrag110.class);
    private TilkjentYtelse tilkjentYtelse = mock(TilkjentYtelse.class);

    @Before
    public void setUp() {
        when(behandlingInfo.getAlleTidligereOppdrag110()).thenReturn(Collections.singletonList(oppdrag110));
    }

    @Test
    public void skal_returnere_kode_fagområde_tjeneste_basert_på_behandling_info_oppdrag() {
        //Act
        when(behandlingInfo.getTilkjentYtelse()).thenReturn(Optional.of(tilkjentYtelse));
        var kodeFagområdeTjeneste = KodeFagområdeTjenesteProvider.getKodeFagområdeTjeneste(behandlingInfo);

        //Assert
        assertThat(kodeFagområdeTjeneste).isExactlyInstanceOf(KodeFagområdeTjeneste.class);
    }

    @Test
    public void skal_returnere_kode_fagområde_tjeneste_basert_på_tidligere_oppdrag() {
        //Arrange
        when(behandlingInfo.getTilkjentYtelse()).thenReturn(Optional.empty());
        when(behandlingInfo.getAlleTidligereOppdrag110()).thenReturn(Collections.singletonList(oppdrag110));
        when(oppdrag110.getKodeFagomrade()).thenReturn(ØkonomiKodeFagområde.FP.name());

        //Act
        var kodeFagområdeTjeneste = KodeFagområdeTjenesteProvider.getKodeFagområdeTjeneste(behandlingInfo);

        //Assert
        assertThat(kodeFagområdeTjeneste).isExactlyInstanceOf(KodeFagområdeTjeneste.class);
    }
}
