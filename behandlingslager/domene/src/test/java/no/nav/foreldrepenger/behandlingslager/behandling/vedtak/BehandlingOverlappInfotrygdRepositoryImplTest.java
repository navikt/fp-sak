package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class BehandlingOverlappInfotrygdRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private Repository repository = repoRule.getRepository();

    private BehandlingOverlappInfotrygdRepository behandlingOverlappInfotrygdRepository;

    private BasicBehandlingBuilder behandlingBuilder = new BasicBehandlingBuilder(entityManager);

    @Before
    public void setup() {
        behandlingOverlappInfotrygdRepository = new BehandlingOverlappInfotrygdRepository(entityManager);
    }

    @Test
    public void lagre() {
        // Arrange
        Behandling behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        ÅpenDatoIntervallEntitet periodeInfotrygd = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2018, 12, 1), LocalDate.of(2019, 1, 1));
        ÅpenDatoIntervallEntitet periodeVL = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 1));
        BehandlingOverlappInfotrygd behandlingOverlappInfotrygd = BehandlingOverlappInfotrygd.builder()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medBehandlingId(behandling.getId())
            .medPeriodeInfotrygd(periodeInfotrygd)
            .medPeriodeVL(periodeVL)
            .build();

        // Act
        Long id = behandlingOverlappInfotrygdRepository.lagre(behandlingOverlappInfotrygd);
        repository.clear();

        // Assert
        BehandlingOverlappInfotrygd hentet = repository.hent(BehandlingOverlappInfotrygd.class, id);
        assertThat(hentet.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(hentet.getSaksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer());
        assertThat(hentet.getPeriodeInfotrygd()).isEqualTo(periodeInfotrygd);
        assertThat(hentet.getPeriodeVL()).isEqualTo(periodeVL);

    }
}
