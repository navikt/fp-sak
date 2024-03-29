package no.nav.foreldrepenger.datavarehus.domene;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FagsakDvhEntityTest {

    @Test
    void skal_bygge_instans_av_fagsakDvh() {
        var fagsakDvh = DatavarehusTestUtils.byggFagsakDvhForTest();

        assertThat(fagsakDvh.getBrukerId()).isEqualTo(DatavarehusTestUtils.BRUKER_ID);
        assertThat(fagsakDvh.getBrukerAktørId()).isEqualTo(DatavarehusTestUtils.BRUKER_AKTØR_ID);
        assertThat(fagsakDvh.getEndretAv()).isEqualTo(DatavarehusTestUtils.ENDRET_AV);
        assertThat(fagsakDvh.getEpsAktørId()).isEqualTo(DatavarehusTestUtils.EPS_AKTØR_ID);
        assertThat(fagsakDvh.getFagsakId()).isEqualTo(DatavarehusTestUtils.FAGSAK_ID);
        assertThat(fagsakDvh.getFagsakStatus()).isEqualTo(DatavarehusTestUtils.FAGSAK_STATUS);
        assertThat(fagsakDvh.getFagsakYtelse()).isEqualTo(DatavarehusTestUtils.FAGSAK_YTELSE);
        assertThat(fagsakDvh.getFunksjonellTid()).isEqualTo(DatavarehusTestUtils.FUNKSJONELL_TID);
        assertThat(fagsakDvh.getOpprettetDato()).isEqualTo(DatavarehusTestUtils.OPPRETTET_DATE);
        assertThat(fagsakDvh.getSaksnummer()).isEqualTo(DatavarehusTestUtils.SAKSNUMMER);
    }
}
