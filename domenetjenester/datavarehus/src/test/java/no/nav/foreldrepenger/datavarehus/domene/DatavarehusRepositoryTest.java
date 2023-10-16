package no.nav.foreldrepenger.datavarehus.domene;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.dbstoette.JpaExtension;

@ExtendWith(JpaExtension.class)
class DatavarehusRepositoryTest {


    private DatavarehusRepository datavarehusRepository;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        datavarehusRepository = new DatavarehusRepository(entityManager);
    }

    @Test
    void skal_lagre_fagsak_dvh() {
        var fagsakDvh = DatavarehusTestUtils.byggFagsakDvhForTest();

        var id = datavarehusRepository.lagre(fagsakDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    void skal_lagre_behandling_dvh() {
        var behandlingDvh = DatavarehusTestUtils.byggBehandlingDvh();

        var id = datavarehusRepository.lagre(behandlingDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    void skal_lagre_behandling_vedtak_dvh() {
        var behandlingVedtakDvh = DatavarehusTestUtils.byggBehandlingVedtakDvh();

        var id = datavarehusRepository.lagre(behandlingVedtakDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    void skal_lagre_aksjonspunkt_dvh() {
        var aksjonspunktDvh = DatavarehusTestUtils.byggAksjonspunktDvh();

        var id = datavarehusRepository.lagre(aksjonspunktDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }

    @Test
    void skal_lagre_vedtak_utbetaling_dvh(EntityManager entityManager) {
        var vedtakUtbetalingDvh = DatavarehusTestUtils.byggVedtakUtbetalingDvh();
        var id = datavarehusRepository.lagre(vedtakUtbetalingDvh);
        entityManager.flush();
        var oppdatertXml = vedtakUtbetalingDvh.getXmlClob() + vedtakUtbetalingDvh.getXmlClob();
        var idOppdatert = datavarehusRepository.oppdater(vedtakUtbetalingDvh.getBehandlingId(), vedtakUtbetalingDvh.getVedtakId(), oppdatertXml);
        assertThat(id).isEqualTo(idOppdatert);
    }

    @Test
    void skal_lagre_fagsakRelasjon_dvh() {
        var fagsakRelasjonDvh = DatavarehusTestUtils.byggFagsakRelasjonDvhForTest();

        var id = datavarehusRepository.lagre(fagsakRelasjonDvh);

        assertThat(id).isGreaterThanOrEqualTo(1);
    }
}
