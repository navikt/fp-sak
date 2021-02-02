package no.nav.foreldrepenger.økonomi.økonomistøtte;

import static no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragTestDataHelper.buildOppdragskontroll;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class FinnNyesteOppdragForSakTest extends EntityManagerAwareTest {

    private EntityManager entityManager;
    private ØkonomioppdragRepository økonomioppdragRepository;
    private FinnNyesteOppdragForSak tjeneste;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        økonomioppdragRepository = new ØkonomioppdragRepository(entityManager);
        tjeneste = new FinnNyesteOppdragForSak(økonomioppdragRepository);
        this.entityManager = entityManager;
    }

    @Test
    public void finnerSisteOppdragForSak() {
        Saksnummer saksnr = new Saksnummer("1234");
        long fagsystemId = 123100L;

        Oppdragskontroll oppdragskontroll = buildOppdragskontroll(saksnr, 1L);
        OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, fagsystemId);
        økonomioppdragRepository.lagre(oppdragskontroll);

        Oppdragskontroll nyesteOppdragskontroll = buildOppdragskontroll(saksnr, 2L);
        OppdragTestDataHelper.buildOppdrag110ES(nyesteOppdragskontroll, fagsystemId);
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(nyesteOppdragskontroll);

        long nyesteId = økonomioppdragRepository.lagre(nyesteOppdragskontroll);

        entityManager.flush();
        entityManager.clear();

        // Act
        List<Oppdrag110> oppdrag110List = tjeneste.finnNyesteOppdragForSak(saksnr);

        // Assert
        assertThat(oppdrag110List).isNotEmpty();
        Oppdragskontroll hentetOppdrag = oppdrag110List.get(0).getOppdragskontroll();
        assertThat(hentetOppdrag.getId()).isEqualTo(nyesteId);
        assertThat(hentetOppdrag.getBehandlingId()).isEqualTo(nyesteOppdragskontroll.getBehandlingId());
        assertThat(hentetOppdrag.getOppdrag110Liste()).hasSize(1);
        assertThat(hentetOppdrag.getOppdrag110Liste().get(0).getFagsystemId()).isEqualTo(fagsystemId);
    }

    @Test
    public void skal_ikke_finne_siste_oppdrag_uten_kvittering() {
        Saksnummer saksnr = new Saksnummer("1234");
        long fagsystemId = 123100L;

        Oppdragskontroll oppdragskontroll = buildOppdragskontroll(saksnr, 1L);
        OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, fagsystemId);
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);
        long førsteOppdragId = økonomioppdragRepository.lagre(oppdragskontroll);

        Oppdragskontroll nyesteOppdragskontroll = buildOppdragskontroll(saksnr, 2L);
        OppdragTestDataHelper.buildOppdrag110ES(nyesteOppdragskontroll, fagsystemId);
        økonomioppdragRepository.lagre(nyesteOppdragskontroll);
        entityManager.flush();
        entityManager.clear();

        // Act
        List<Oppdrag110> oppdrag110List = tjeneste.finnNyesteOppdragForSak(saksnr);

        // Assert
        assertThat(oppdrag110List).hasSize(1);
        Oppdragskontroll hentetOppdrag = oppdrag110List.get(0).getOppdragskontroll();
        assertThat(hentetOppdrag.getId()).isEqualTo(førsteOppdragId);
        assertThat(hentetOppdrag.getBehandlingId()).isEqualTo(oppdragskontroll.getBehandlingId());
        assertThat(hentetOppdrag.getOppdrag110Liste()).hasSize(1);
        assertThat(hentetOppdrag.getOppdrag110Liste().get(0).getFagsystemId()).isEqualTo(fagsystemId);
    }

    @Test
    public void skal_ikke_finne_siste_oppdrag_negativ_kvittering() {
        Saksnummer saksnr = new Saksnummer("1234");
        long fagsystemId = 123100L;

        Oppdragskontroll oppdragskontroll = buildOppdragskontroll(saksnr, 1L);
        OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, fagsystemId);
        OppdragKvitteringTestUtil.lagPositiveKvitteringer(oppdragskontroll);
        long førsteOppdragId = økonomioppdragRepository.lagre(oppdragskontroll);

        Oppdragskontroll nyesteOppdragskontroll = buildOppdragskontroll(saksnr, 2L);
        OppdragTestDataHelper.buildOppdrag110ES(nyesteOppdragskontroll, fagsystemId);
        OppdragKvitteringTestUtil.lagNegativeKvitteringer(nyesteOppdragskontroll);
        økonomioppdragRepository.lagre(nyesteOppdragskontroll);
        entityManager.flush();
        entityManager.clear();

        // Act
        List<Oppdrag110> oppdrag110List = tjeneste.finnNyesteOppdragForSak(saksnr);

        // Assert
        assertThat(oppdrag110List).hasSize(1);
        Oppdragskontroll hentetOppdrag = oppdrag110List.get(0).getOppdragskontroll();
        assertThat(hentetOppdrag.getId()).isEqualTo(førsteOppdragId);
        assertThat(hentetOppdrag.getBehandlingId()).isEqualTo(oppdragskontroll.getBehandlingId());
        assertThat(hentetOppdrag.getOppdrag110Liste()).hasSize(1);
        assertThat(hentetOppdrag.getOppdrag110Liste().get(0).getFagsystemId()).isEqualTo(fagsystemId);
    }

    /**
     * Førstegangsbehandling:
     * <ul>
     * <li>2 mottakere (bruker og arbeidsgiver)</li>
     * <li>3 oppdrag110:
     * <ul>
     * <li>Oppdrag110-1: Positiv</li>
     * <li>Oppdrag110-2: Negativ</li>
     * <li>Oppdrag110-3: Positiv</li>
     * </ul>
     * </li>
     * </ul>
     * Revurdering: Skal kun bruke Oppdrag110 med positiv kvittering. Altså 1 og 3.
     */
    @Test
    public void skalKunHenteOppdrag110MedPositivKvitteringForSaksnummer() {
        // Arrange
        Saksnummer saksnr = new Saksnummer("1234");

        Oppdragskontroll oppdragskontroll = buildOppdragskontroll(saksnr, 1L);
        Oppdrag110 oppdrag1 = OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 1L);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag1);
        Oppdrag110 oppdrag2 = OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 2L);
        OppdragKvitteringTestUtil.lagNegativKvitting(oppdrag2);
        Oppdrag110 oppdrag3 = OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 3L);
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag3);
        økonomioppdragRepository.lagre(oppdragskontroll);

        // Act
        var resultater = tjeneste.finnNyesteOppdragForSak(saksnr);

        // Assert
        assertThat(resultater).hasSize(2);
        assertThat(resultater).anySatisfy(oppdrag110 ->
            assertThat(oppdrag110.getFagsystemId()).isEqualTo(1L));
        assertThat(resultater).anySatisfy(oppdrag110 ->
            assertThat(oppdrag110.getFagsystemId()).isEqualTo(3L));
    }
}
