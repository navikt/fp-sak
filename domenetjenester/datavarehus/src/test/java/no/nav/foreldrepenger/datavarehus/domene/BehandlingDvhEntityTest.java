package no.nav.foreldrepenger.datavarehus.domene;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BehandlingDvhEntityTest {

    @Test
    void skal_bygge_instans_av_behandlingDvh() {
        var behandlingDvh = DatavarehusTestUtils.byggBehandlingDvh();

        assertThat(behandlingDvh.getAnsvarligBeslutter()).isEqualTo(DatavarehusTestUtils.ANSVARLIG_BESLUTTER);
        assertThat(behandlingDvh.getAnsvarligSaksbehandler()).isEqualTo(DatavarehusTestUtils.ANSVARLIG_SAKSBEHANDLER);
        assertThat(behandlingDvh.getBehandlendeEnhet()).isEqualTo(DatavarehusTestUtils.BEHANDLENDE_ENHET);
        assertThat(behandlingDvh.getBehandlingId()).isEqualTo(DatavarehusTestUtils.BEHANDLING_ID);
        assertThat(behandlingDvh.getBehandlingUuid()).isEqualTo(DatavarehusTestUtils.BEHANDLING_UUID);
        assertThat(behandlingDvh.getBehandlingResultatType()).isEqualTo(DatavarehusTestUtils.BEHANDLING_RESULTAT_TYPE);
        assertThat(behandlingDvh.getBehandlingStatus()).isEqualTo(DatavarehusTestUtils.BEHANDLING_STATUS);
        assertThat(behandlingDvh.getBehandlingType()).isEqualTo(DatavarehusTestUtils.BEHANDLING_TYPE);
        assertThat(behandlingDvh.getSaksnummer()).isEqualTo(String.valueOf(DatavarehusTestUtils.SAKSNUMMER));
        assertThat(behandlingDvh.getAktørId()).isEqualTo(DatavarehusTestUtils.BRUKER_AKTØR_ID);
        assertThat(behandlingDvh.getYtelseType()).isEqualTo(DatavarehusTestUtils.FAGSAK_YTELSE);
        assertThat(behandlingDvh.getFunksjonellTid()).isEqualTo(DatavarehusTestUtils.FUNKSJONELL_TID);
        assertThat(behandlingDvh.getUtlandstilsnitt()).isEqualTo(DatavarehusTestUtils.UTLANDSTILSNITT);
        assertThat(behandlingDvh.getFamilieHendelseType()).isEqualTo(DatavarehusTestUtils.FAMILIE_HENDELSE_TYPE);
        assertThat(behandlingDvh.getRelatertBehandlingUuid()).isEqualTo(DatavarehusTestUtils.BEHANDLING_UUID);
        assertThat(behandlingDvh.getPapirSøknad()).isFalse();
        assertThat(behandlingDvh.getBehandlingMetode()).isEqualTo(BehandlingMetode.AUTOMATISK.name());
        assertThat(behandlingDvh.getRevurderingÅrsak()).isEqualTo(RevurderingÅrsak.SØKNAD.name());
        assertThat(behandlingDvh.getMottattTid()).isEqualTo(DatavarehusTestUtils.MOTTATT_TID);
        assertThat(behandlingDvh.getRegistrertTid()).isEqualTo(DatavarehusTestUtils.FUNKSJONELL_TID);
        assertThat(behandlingDvh.getKanBehandlesTid()).isEqualTo(DatavarehusTestUtils.FUNKSJONELL_TID.plusSeconds(1));
        assertThat(behandlingDvh.getFerdigBehandletTid()).isEqualTo(DatavarehusTestUtils.FUNKSJONELL_TID.plusMinutes(1));
        assertThat(behandlingDvh.getFoersteStoenadsdag()).isEqualTo(DatavarehusTestUtils.OPPRETTET_DATE.plusDays(1));
        assertThat(behandlingDvh.getForventetOppstartTid()).isEqualTo(DatavarehusTestUtils.OPPRETTET_DATE.plusDays(2));
        assertThat(behandlingDvh.getVedtakResultatType()).isEqualTo(DatavarehusTestUtils.VEDTAK_RESULTAT_TYPE);
        assertThat(behandlingDvh.getVilkårIkkeOppfylt()).isNull();
        assertThat(behandlingDvh.getVedtakTid()).isEqualTo(DatavarehusTestUtils.VEDTAK_TID);
        assertThat(behandlingDvh.getUtbetaltTid()).isEqualTo(DatavarehusTestUtils.VEDTAK_DATO.plusWeeks(1));
    }
}
