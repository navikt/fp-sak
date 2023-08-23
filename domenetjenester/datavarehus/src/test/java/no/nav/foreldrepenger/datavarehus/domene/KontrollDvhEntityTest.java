package no.nav.foreldrepenger.datavarehus.domene;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KontrollDvhEntityTest {

    @Test
    void skal_bygge_instans_av_kontrollDvh() {
        var kontrollDvh = DatavarehusTestUtils.byggKontrollDvh();

        assertThat(kontrollDvh.getBehandlingAksjonTransIdMax()).isEqualTo(DatavarehusTestUtils.BEHANDLING_AKSJON_TRANS_ID_MAX);
        assertThat(kontrollDvh.getBehandlingTransIdMax()).isEqualTo(DatavarehusTestUtils.BEHANDLING_TRANS_ID_MAX);
        assertThat(kontrollDvh.getBehandlingVedtakTransIdMax()).isEqualTo(DatavarehusTestUtils.BEHANDLING_VEDTAK_TRANS_ID_MAX);
        assertThat(kontrollDvh.getBehandllingStegTransIdMax()).isEqualTo(DatavarehusTestUtils.BEHANDLLING_STEG_TRANS_ID_MAX);
        assertThat(kontrollDvh.getFagsakTransIdMax()).isEqualTo(DatavarehusTestUtils.FAGSAK_TRANS_ID_MAX);
        assertThat(kontrollDvh.getLastFlagg()).isEqualTo(DatavarehusTestUtils.LAST_FLAGG);
    }
}
