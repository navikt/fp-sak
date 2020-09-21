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
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

public class OverlappVedtakRepositoryTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();

    private OverlappVedtakRepository overlappVedtakRepository;

    private BasicBehandlingBuilder behandlingBuilder = new BasicBehandlingBuilder(entityManager);

    @Before
    public void setup() {
        overlappVedtakRepository = new OverlappVedtakRepository(entityManager);
    }

    @Test
    public void lagre() {
        // Arrange
        Behandling behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        ÅpenDatoIntervallEntitet periodeVL = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 1));
        String ytelseInfotrygd = "BS";
        OverlappVedtak.Builder builder = OverlappVedtak.builder()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medBehandlingId(behandling.getId())
            .medPeriode(periodeVL)
            .medHendelse("TEST")
            .medUtbetalingsprosent(100L)
            .medFagsystem(Fagsystem.INFOTRYGD.getKode())
            .medYtelse(ytelseInfotrygd);

        // Act
        overlappVedtakRepository.lagre(builder);

        // Assert
        OverlappVedtak hentet = overlappVedtakRepository.hentForSaksnummer(behandling.getFagsak().getSaksnummer()).get(0);
        assertThat(hentet.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(hentet.getSaksnummer()).isEqualTo(behandling.getFagsak().getSaksnummer());
        assertThat(hentet.getPeriode()).isEqualTo(periodeVL);
        assertThat(hentet.getYtelse()).isEqualTo(ytelseInfotrygd);

    }
}
