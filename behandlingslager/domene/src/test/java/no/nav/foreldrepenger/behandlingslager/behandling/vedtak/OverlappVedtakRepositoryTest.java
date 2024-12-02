package no.nav.foreldrepenger.behandlingslager.behandling.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

class OverlappVedtakRepositoryTest extends EntityManagerAwareTest {

    private OverlappVedtakRepository overlappVedtakRepository;

    private BasicBehandlingBuilder behandlingBuilder;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        overlappVedtakRepository = new OverlappVedtakRepository(entityManager);
        behandlingBuilder = new BasicBehandlingBuilder(entityManager);
    }

    @Test
    void lagre() {
        // Arrange
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var periodeVL = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2019, 1, 1),
            LocalDate.of(2019, 5, 1));
        var ytelseInfotrygd = OverlappVedtak.OverlappYtelseType.BS;
        var builder = OverlappVedtak.builder()
            .medSaksnummer(behandling.getSaksnummer())
            .medBehandlingId(behandling.getId())
            .medPeriode(periodeVL)
            .medHendelse("TEST")
            .medUtbetalingsprosent(100L)
            .medFagsystem(Fagsystem.INFOTRYGD)
            .medYtelse(ytelseInfotrygd);

        // Act
        overlappVedtakRepository.lagre(builder);

        // Assert
        var hentet = overlappVedtakRepository.hentForSaksnummer(behandling.getSaksnummer()).get(0);
        assertThat(hentet.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(hentet.getSaksnummer()).isEqualTo(behandling.getSaksnummer());
        assertThat(hentet.getPeriode()).isEqualTo(periodeVL);
        assertThat(hentet.getYtelse()).isEqualTo(ytelseInfotrygd);

    }
}
