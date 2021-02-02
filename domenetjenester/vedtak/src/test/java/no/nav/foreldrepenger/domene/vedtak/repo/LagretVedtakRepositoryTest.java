package no.nav.foreldrepenger.domene.vedtak.repo;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtak;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

public class LagretVedtakRepositoryTest extends EntityManagerAwareTest {

    private EntityManager entityManager;
    private LagretVedtakRepository lagretVedtakRepository;

    private LagretVedtak.Builder lagretVedtakBuilder;

    private static final Long FAGSAK_ID = 62L;
    private static final Long BEHANDLING_ID = 265L;
    private static final String STRING_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><element>test av xml</element>";

    @BeforeEach
    public void setup() {
        lagretVedtakBuilder = LagretVedtak.builder();
        entityManager = getEntityManager();
        lagretVedtakRepository = new LagretVedtakRepository(entityManager);
    }

    @Test
    public void skal_lagre_ny_lagretVedtak() {
        LagretVedtak lagretVedtak = lagLagretVedtakMedPaakrevdeFelter();

        lagretVedtakRepository.lagre(lagretVedtak);

        Long id = lagretVedtak.getId();
        assertThat(id).isNotNull();

        entityManager.flush();
        entityManager.clear();
        LagretVedtak lagretVedtakLest = entityManager.find(LagretVedtak.class, id);
        assertThat(lagretVedtakLest).isNotNull();
    }

    @Test
    public void skal_finne_lagretVedtak_med_id() {
        LagretVedtak lagretVedtakLagret = lagLagretVedtakMedPaakrevdeFelter();
        entityManager.persist(lagretVedtakLagret);
        entityManager.flush();
        entityManager.clear();
        long idLagret = lagretVedtakLagret.getId();

        LagretVedtak lagretVedtak = lagretVedtakRepository.hentLagretVedtak(idLagret);
        assertThat(lagretVedtak).isNotNull();
        assertThat(lagretVedtak.getFagsakId()).isEqualTo(FAGSAK_ID);
        assertThat(lagretVedtak.getBehandlingId()).isEqualTo(BEHANDLING_ID);
        assertThat(lagretVedtak.getXmlClob()).isEqualTo(STRING_XML);
    }

    private LagretVedtak lagLagretVedtakMedPaakrevdeFelter() {
        return lagretVedtakBuilder.medFagsakId(FAGSAK_ID)
                .medBehandlingId(BEHANDLING_ID)
                .medXmlClob(STRING_XML).build();
    }
}
