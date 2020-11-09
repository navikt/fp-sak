package no.nav.foreldrepenger.datavarehus.domene;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class DatavarehusRepositoryTest {


    private DatavarehusRepository datavarehusRepository;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        datavarehusRepository = new DatavarehusRepository(entityManager);
    }

    @Test
    public void skal_lagre_fagsak_dvh() {
        FagsakDvh fagsakDvh = DatavarehusTestUtils.byggFagsakDvhForTest();

        long id = datavarehusRepository.lagre(fagsakDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void skal_lagre_behandling_dvh() {
        BehandlingDvh behandlingDvh = DatavarehusTestUtils.byggBehandlingDvh();

        long id = datavarehusRepository.lagre(behandlingDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void skal_lagre_behandling_steg_dvh() {
        BehandlingStegDvh behandlingStegDvh = DatavarehusTestUtils.byggBehandlingStegDvh();

        long id = datavarehusRepository.lagre(behandlingStegDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void skal_lagre_behandling_vedtak_dvh() {
        BehandlingVedtakDvh behandlingVedtakDvh = DatavarehusTestUtils.byggBehandlingVedtakDvh();

        long id = datavarehusRepository.lagre(behandlingVedtakDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void skal_lagre_aksjonspunkt_dvh() {
        AksjonspunktDvh aksjonspunktDvh = DatavarehusTestUtils.byggAksjonspunktDvh();

        long id = datavarehusRepository.lagre(aksjonspunktDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }


    @Test
    public void skal_lagre_behandling_kontroll_dvh() {
        KontrollDvh kontrollDvh = DatavarehusTestUtils.byggKontrollDvh();

        long id = datavarehusRepository.lagre(kontrollDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void skal_lagre_vedtak_utbetaling_dvh(EntityManager entityManager) {
        VedtakUtbetalingDvh vedtakUtbetalingDvh = DatavarehusTestUtils.byggVedtakUtbetalingDvh();
        long id = datavarehusRepository.lagre(vedtakUtbetalingDvh);
        entityManager.flush();
        final String oppdatertXml = vedtakUtbetalingDvh.getXmlClob() + vedtakUtbetalingDvh.getXmlClob();
        long idOppdatert = datavarehusRepository.oppdater(vedtakUtbetalingDvh.getBehandlingId(),
            vedtakUtbetalingDvh.getVedtakId(), oppdatertXml);
        assertThat(id).isEqualTo(idOppdatert);
    }

    @Test
    public void skal_lagre_fagsakRelasjon_dvh() {
        FagsakRelasjonDvh fagsakRelasjonDvh = DatavarehusTestUtils.byggFagsakRelasjonDvhForTest();

        long id = datavarehusRepository.lagre(fagsakRelasjonDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }
}
