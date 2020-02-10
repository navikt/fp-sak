package no.nav.foreldrepenger.domene.vedtak.repo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.lagretvedtak.LagretVedtak;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class LagretVedtakRepositoryTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private final Repository repository = repoRule.getRepository();
    private final LagretVedtakRepository lagretVedtakRepository = new LagretVedtakRepository(repoRule.getEntityManager());

    private LagretVedtak.Builder lagretVedtakBuilder;

    private static final Long FAGSAK_ID = 62L;
    private static final Long BEHANDLING_ID = 265L;
    private static final String STRING_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><element>test av xml</element>";

    @Before
    public void setup() {
        lagretVedtakBuilder = LagretVedtak.builder();
    }

    @Test
    public void skal_lagre_ny_lagretVedtak() {
        LagretVedtak lagretVedtak = lagLagretVedtakMedPaakrevdeFelter();

        lagretVedtakRepository.lagre(lagretVedtak);

        Long id = lagretVedtak.getId();
        assertThat(id).isNotNull();

        repository.flushAndClear();
        LagretVedtak lagretVedtakLest = repository.hent(LagretVedtak.class, id);
        assertThat(lagretVedtakLest).isNotNull();
    }

    @Test
    public void skal_finne_lagretVedtak_med_id() {
        LagretVedtak lagretVedtakLagret = lagLagretVedtakMedPaakrevdeFelter();
        repository.lagre(lagretVedtakLagret);
        repository.flushAndClear();
        long idLagret = lagretVedtakLagret.getId();

        LagretVedtak lagretVedtak = lagretVedtakRepository.hentLagretVedtak(idLagret);
        assertThat(lagretVedtak).isNotNull();
        assertThat(lagretVedtak.getFagsakId()).isEqualTo(FAGSAK_ID);
        assertThat(lagretVedtak.getBehandlingId()).isEqualTo(BEHANDLING_ID);
        assertThat(lagretVedtak.getXmlClob()).isEqualTo(STRING_XML);
    }

    // -----------------

    private LagretVedtak lagLagretVedtakMedPaakrevdeFelter() {
        return lagretVedtakBuilder.medFagsakId(FAGSAK_ID)
                .medBehandlingId(BEHANDLING_ID)
                .medXmlClob(STRING_XML).build();
    }
}
