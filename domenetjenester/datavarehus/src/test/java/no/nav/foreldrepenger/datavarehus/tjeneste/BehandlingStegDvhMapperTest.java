package no.nav.foreldrepenger.datavarehus.tjeneste;

import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLING_STEG_ID;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLING_STEG_STATUS;
import static no.nav.foreldrepenger.datavarehus.tjeneste.DvhTestDataUtil.BEHANDLING_STEG_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingStegDvh;
import no.nav.vedtak.felles.testutilities.Whitebox;

@SuppressWarnings("deprecation")
public class BehandlingStegDvhMapperTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();
    private BehandlingStegDvhMapper mapper = new BehandlingStegDvhMapper();

    @Test
    public void skal_mappe_til_behandling_steg_dvh() {

        BehandlingStegTilstandSnapshot behandlingStegTilstand = new BehandlingStegTilstandSnapshot(1000L,
            BEHANDLING_STEG_TYPE, BEHANDLING_STEG_STATUS);
        Whitebox.setInternalState(behandlingStegTilstand, "id", BEHANDLING_STEG_ID);
        long behandlingId = 1L;
        BehandlingStegDvh dvh = mapper.map(behandlingStegTilstand, behandlingId);

        assertThat(dvh).isNotNull();
        assertThat(dvh.getBehandlingId()).isEqualTo(behandlingId);
        assertThat(dvh.getBehandlingStegId()).isEqualTo(BEHANDLING_STEG_ID);
        assertThat(dvh.getBehandlingStegStatus()).isEqualTo(BEHANDLING_STEG_STATUS.getKode());
        assertThat(dvh.getBehandlingStegType()).isEqualTo(BEHANDLING_STEG_TYPE.getKode());
    }


}
