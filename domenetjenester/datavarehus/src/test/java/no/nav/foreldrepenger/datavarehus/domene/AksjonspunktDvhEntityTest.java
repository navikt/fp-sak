package no.nav.foreldrepenger.datavarehus.domene;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AksjonspunktDvhEntityTest {

    @Test
    void skal_bygge_instans_av_aksjonspunktDvh() {
        var aksjonspunktDvh = DatavarehusTestUtils.byggAksjonspunktDvh();

        assertThat(aksjonspunktDvh.getAksjonspunktDef()).isEqualTo(DatavarehusTestUtils.AKSJONSPUNKT_DEF);
        assertThat(aksjonspunktDvh.getAksjonspunktId()).isEqualTo(DatavarehusTestUtils.AKSJONSPUNKT_ID);
        assertThat(aksjonspunktDvh.getAksjonspunktStatus()).isEqualTo(DatavarehusTestUtils.AKSJONSPUNKT_STATUS);
        assertThat(aksjonspunktDvh.getAnsvarligBeslutter()).isEqualTo(DatavarehusTestUtils.ANSVARLIG_BESLUTTER);
        assertThat(aksjonspunktDvh.getAnsvarligSaksbehandler()).isEqualTo(DatavarehusTestUtils.ANSVARLIG_SAKSBEHANDLER);
        assertThat(aksjonspunktDvh.getBehandlingId()).isEqualTo(DatavarehusTestUtils.BEHANDLING_ID);
        assertThat(aksjonspunktDvh.getBehandlendeEnhetKode()).isEqualTo(DatavarehusTestUtils.BEHANDLENDE_ENHET);
        assertThat(aksjonspunktDvh.getBehandlingStegId()).isEqualTo(DatavarehusTestUtils.BEHANDLING_STEG_ID);
        assertThat(aksjonspunktDvh.getEndretAv()).isEqualTo(DatavarehusTestUtils.ENDRET_AV);
        assertThat(aksjonspunktDvh.getFunksjonellTid()).isEqualTo(DatavarehusTestUtils.FUNKSJONELL_TID);
        assertThat(aksjonspunktDvh.isToTrinnsBehandling()).isTrue();
        assertThat(aksjonspunktDvh.getToTrinnsBehandlingGodkjent()).isTrue();
    }
}
