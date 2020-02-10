package no.nav.foreldrepenger.behandling.klage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;

public class KlageHistorikkTjenesteTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    HistorikkRepository historikkRepository;

    private KlageHistorikkTjeneste klageHistorikkTjeneste;

    @Before
    public void oppsett() {
        klageHistorikkTjeneste = new KlageHistorikkTjeneste(historikkRepository);
    }

    @Test
    public void skal_lagre_klage_historikk(){
        // Arrange
        Behandling klageBehandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.KLAGE).lagMocked();

        // Act
        klageHistorikkTjeneste.opprettHistorikkinnslag(klageBehandling);

        //Assert
        ArgumentCaptor<Historikkinnslag> captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepository, times(1)).lagre(captor.capture());
        Historikkinnslag historikkinnslag = captor.getValue();
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SØKER);
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.BEH_STARTET);
        assertThat(historikkinnslag.getBehandlingId()).isEqualTo(klageBehandling.getId());
        assertThat(historikkinnslag.getFagsakId()).isEqualTo(klageBehandling.getFagsakId());
    }
}
