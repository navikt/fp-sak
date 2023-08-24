package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CommonDvhMapperTest {

    private static final String OPPRETTET_AV = "OpprettetAv";
    private static final LocalDateTime OPPRETTET_TIDSPUNKT = LocalDateTime.now().minusDays(1);
    private static final String ENDRET_AV = "EndretAv";
    private static final LocalDateTime ENDRET_TIDSPUNKT = LocalDateTime.now();

    @Test
    void skal_mappe_til_opprettet_av() {
        assertThat(CommonDvhMapper.finnEndretAvEllerOpprettetAv(byggNyBehandling())).isEqualTo(OPPRETTET_AV);
    }

    @Test
    void skal_mappe_til_endret_av() {
        assertThat(CommonDvhMapper.finnEndretAvEllerOpprettetAv(byggOppdatertBehandling())).isEqualTo(ENDRET_AV);
    }

    private Behandling byggNyBehandling() {
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagMocked();
        behandling.setOpprettetAv(OPPRETTET_AV);
        behandling.setOpprettetTidspunkt(OPPRETTET_TIDSPUNKT);
        return behandling;
    }

    private Behandling byggOppdatertBehandling() {
        var behandling = byggNyBehandling();
        behandling.setEndretAv(ENDRET_AV);
        behandling.setEndretTidspunkt(ENDRET_TIDSPUNKT);
        return behandling;
    }
}
