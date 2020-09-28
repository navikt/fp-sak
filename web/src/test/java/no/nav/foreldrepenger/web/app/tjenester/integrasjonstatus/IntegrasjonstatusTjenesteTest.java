package no.nav.foreldrepenger.web.app.tjenester.integrasjonstatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.web.app.healthchecks.SelftestResultat;
import no.nav.foreldrepenger.web.app.healthchecks.Selftests;

@ExtendWith(MockitoExtension.class)
public class IntegrasjonstatusTjenesteTest {

    @Mock
    private Selftests selftests;
    private IntegrasjonstatusTjeneste integrasjonstatusTjeneste;

    @BeforeEach
    public void before() {
        integrasjonstatusTjeneste = new IntegrasjonstatusTjeneste(selftests);
    }

    @Test
    public void skal_kalle_oppdatering_og_returnere_info_om_system_som_er_nede() {
        SelftestResultat selftestResultat = new SelftestResultat();
        selftestResultat.setApplication("fpsak");
        selftestResultat.leggTilResultatForKritiskTjeneste(true, "test oppe", "test");
        selftestResultat.leggTilResultatForKritiskTjeneste(false, "test nede", "test");
        when(selftests.run()).thenReturn(selftestResultat);

        List<SystemNedeDto> systemerSomErNede = integrasjonstatusTjeneste.finnSystemerSomErNede();

        assertThat(systemerSomErNede).hasSize(1);
        assertThat(systemerSomErNede.get(0).getSystemNavn()).isEqualTo("fpsak");
        assertThat(systemerSomErNede.get(0).getFeilmelding()).isEqualTo("test nede");
        assertThat(systemerSomErNede.get(0).getEndepunkt()).isEqualTo("test");
        assertThat(systemerSomErNede.get(0).getStackTrace()).isNull();
    }

    @Test
    public void skal_bruke_message_fra_result_hvis_throwable_ikke_er_oppgitt() {
        SelftestResultat selftestResultat = new SelftestResultat();
        selftestResultat.setApplication("fpsak");
        selftestResultat.leggTilResultatForKritiskTjeneste(false, "test", "test");
        when(selftests.run()).thenReturn(selftestResultat);

        List<SystemNedeDto> systemerSomErNede = integrasjonstatusTjeneste.finnSystemerSomErNede();

        assertThat(systemerSomErNede).hasSize(1);
        assertThat(systemerSomErNede.get(0).getSystemNavn()).isEqualTo("fpsak");
        assertThat(systemerSomErNede.get(0).getFeilmelding()).isEqualTo("test");
        assertThat(systemerSomErNede.get(0).getEndepunkt()).isEqualTo("test");
        assertThat(systemerSomErNede.get(0).getStackTrace()).isNull();
    }
}
