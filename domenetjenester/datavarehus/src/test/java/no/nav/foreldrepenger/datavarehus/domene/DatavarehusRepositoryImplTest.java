package no.nav.foreldrepenger.datavarehus.domene;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;

public class DatavarehusRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private final DatavarehusRepository datavarehusRepository = new DatavarehusRepository(repoRule.getEntityManager());

    @Test
    public void skal_laste_opp_test_konfig() throws Exception {

    }

    @Test
    public void skal_lagre_fagsak_dvh() throws Exception {
        FagsakDvh fagsakDvh = DatavarehusTestUtils.byggFagsakDvhForTest();

        long id = datavarehusRepository.lagre(fagsakDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void skal_lagre_behandling_dvh() throws Exception {
        BehandlingDvh behandlingDvh = DatavarehusTestUtils.byggBehandlingDvh();

        long id = datavarehusRepository.lagre(behandlingDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void skal_lagre_behandling_steg_dvh() throws Exception {
        BehandlingStegDvh behandlingStegDvh = DatavarehusTestUtils.byggBehandlingStegDvh();

        long id = datavarehusRepository.lagre(behandlingStegDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void skal_lagre_behandling_vedtak_dvh() throws Exception {
        BehandlingVedtakDvh behandlingVedtakDvh = DatavarehusTestUtils.byggBehandlingVedtakDvh();

        long id = datavarehusRepository.lagre(behandlingVedtakDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void skal_lagre_aksjonspunkt_dvh() throws Exception {
        AksjonspunktDvh aksjonspunktDvh = DatavarehusTestUtils.byggAksjonspunktDvh();

        long id = datavarehusRepository.lagre(aksjonspunktDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }


    @Test
    public void skal_lagre_behandling_kontroll_dvh() throws Exception {
        KontrollDvh kontrollDvh = DatavarehusTestUtils.byggKontrollDvh();

        long id = datavarehusRepository.lagre(kontrollDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void skal_lagre_vedtak_utbetaling_dvh() {
        VedtakUtbetalingDvh vedtakUtbetalingDvh = DatavarehusTestUtils.byggVedtakUtbetalingDvh();
        long id = datavarehusRepository.lagre(vedtakUtbetalingDvh);
        repoRule.getEntityManager().flush();
        final String oppdatertXml = vedtakUtbetalingDvh.getXmlClob()+vedtakUtbetalingDvh.getXmlClob();
        long idOppdatert = datavarehusRepository.oppdater(vedtakUtbetalingDvh.getBehandlingId(), vedtakUtbetalingDvh.getVedtakId(), oppdatertXml);
        assertThat(id).isEqualTo(idOppdatert);
    }

    @Test
    public void skal_lagre_fagsakRelasjon_dvh() throws Exception {
        FagsakRelasjonDvh fagsakRelasjonDvh = DatavarehusTestUtils.byggFagsakRelasjonDvhForTest();

        long id = datavarehusRepository.lagre(fagsakRelasjonDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }
}
