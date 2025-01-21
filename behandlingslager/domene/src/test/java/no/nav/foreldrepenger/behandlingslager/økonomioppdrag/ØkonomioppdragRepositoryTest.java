package no.nav.foreldrepenger.behandlingslager.økonomioppdrag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@ExtendWith(JpaExtension.class)
class ØkonomioppdragRepositoryTest extends EntityManagerAwareTest {

    private EntityManager entityManager;
    private ØkonomioppdragRepository økonomioppdragRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        this.entityManager = entityManager;
        økonomioppdragRepository = new ØkonomioppdragRepository(entityManager);
    }

    @Test
    void lagreOgHenteOppdragskontroll() {
        // Arrange
        var oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert
        var id = oppdrkontroll.getId();
        Assertions.assertThat(id).isNotNull();

        var oppdrkontrollLest = økonomioppdragRepository.hentOppdragskontroll(id);

        assertThat(oppdrkontrollLest).isNotNull();
    }

    @Test
    void lagreOgSøkeOppOppdragskontrollForPeriode() {
        // Arrange
        var oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var behandlingId = oppdrkontroll.getBehandlingId();
        OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert

        var oppdragListe = økonomioppdragRepository.hentOppdrag110ForPeriodeOgFagområde(LocalDate.now(),
            LocalDate.now(), KodeFagområde.REFUTG);
        assertThat(oppdragListe.stream().map(o -> o.getOppdragskontroll().getBehandlingId())).contains(behandlingId);
    }

    @Test
    void finnAlleOppdragUtenKvittering() {
        // Arrange
        var oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        OppdragTestDataHelper.buildOppdragslinje150(oppdr110);

        økonomioppdragRepository.lagre(oppdrkontroll);

        var oppdrag110 = økonomioppdragRepository.hentOppdragUtenKvittering(oppdr110.getFagsystemId(), oppdrkontroll.getBehandlingId());

        assertThat(oppdrag110).isNotNull();
    }

    @Test
    void kastExceptionHvisFlereOppdragUtenKvitteringFinnes() {
        // Arrange
        var oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        OppdragTestDataHelper.buildOppdragslinje150(oppdr110);
        var oppdr110_2 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        OppdragTestDataHelper.buildOppdragslinje150(oppdr110_2);

        økonomioppdragRepository.lagre(oppdrkontroll);
        assertThatThrownBy(() ->
            økonomioppdragRepository.hentOppdragUtenKvittering(oppdr110.getFagsystemId(), oppdrkontroll.getBehandlingId()))
            .hasMessageContaining("returnerte mer enn eksakt ett resultat");
    }

    @Test
    void okHvisFlereOppdragFinnesMenKunEnnUtenKvittering() {
        // Arrange
        var oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        OppdragTestDataHelper.buildOppdragslinje150(oppdr110);
        var oppdr110_2 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        OppdragTestDataHelper.buildOppdragslinje150(oppdr110_2);
        økonomioppdragRepository.lagre(oppdrkontroll);

        var oppdragKvittering = OppdragKvitteringTestUtil.lagPositivKvitting(oppdr110_2);
        økonomioppdragRepository.lagre(oppdragKvittering);

        // Act
        var oppdrag110 = økonomioppdragRepository.hentOppdragUtenKvittering(oppdr110.getFagsystemId(), oppdrkontroll.getBehandlingId());

        assertThat(oppdrag110).isNotNull();
        assertThat(oppdrag110.getOppdragKvittering()).isNull();
        assertThat(oppdrag110.getFagsystemId()).isEqualTo(44L);
    }

    @Test
    void lagreOgSøkeOppOppdragskontrollForPeriodeUtenResultat() {
        //Testene kjøres ikke mot tom db
        var førSize = økonomioppdragRepository.hentOppdrag110ForPeriodeOgFagområde(LocalDate.now().minusDays(1),
            LocalDate.now().minusDays(1), KodeFagområde.REFUTG).size();
        // Arrange
        var oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert

        var oppdragListe = økonomioppdragRepository.hentOppdrag110ForPeriodeOgFagområde(LocalDate.now().minusDays(1),
            LocalDate.now().minusDays(1), KodeFagområde.REFUTG).size();
        assertThat(oppdragListe).isEqualTo(førSize);
    }

    @Test
    void lagreOppdrag110() {
        // Arrange
        var oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);

        // Act
        var id = økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert

        var oppdrkontrollLest = økonomioppdragRepository.hentOppdragskontroll(id);
        assertThat(oppdrkontrollLest.getOppdrag110Liste()).hasSize(1);
        var oppdr110Lest = oppdrkontrollLest.getOppdrag110Liste().get(0);
        assertThat(oppdr110Lest).isNotNull();
        assertThat(oppdr110Lest.getId()).isNotZero();
    }

    @Test
    void lagreOppdragKvittering() {
        // Arrange
        var oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdrag110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        var id = økonomioppdragRepository.lagre(oppdrkontroll);

        var oppdragKvittering = OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag110);

        // Act
        økonomioppdragRepository.lagre(oppdragKvittering);

        // Assert

        var oppdrkontrollLest = økonomioppdragRepository.hentOppdragskontroll(id);
        assertThat(oppdrkontrollLest.getOppdrag110Liste()).hasSize(1);
        var oppdr110Lest = oppdrkontrollLest.getOppdrag110Liste().get(0);
        assertThat(oppdr110Lest).isNotNull();
        assertThat(oppdr110Lest.getId()).isNotZero();
        var oppdrKvittering = oppdr110Lest.getOppdragKvittering();
        assertThat(oppdrKvittering).isNotNull();
        assertThat(oppdrKvittering.getId()).isNotZero();
    }

    @Test
    void finnerAlleOppdragForSak() {
        var saksnr = new Saksnummer("1234");
        var nyesteFagsystemId = 101L;

        var oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll(saksnr, 1L);
        OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 100L);
        økonomioppdragRepository.lagre(oppdragskontroll);

        var nyesteOppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll(saksnr, 2L);
        OppdragTestDataHelper.buildOppdrag110ES(nyesteOppdragskontroll, nyesteFagsystemId);
        økonomioppdragRepository.lagre(nyesteOppdragskontroll);


        var oppdragListe = økonomioppdragRepository.finnAlleOppdragForSak(saksnr);
        assertThat(oppdragListe).hasSize(2);
        assertThat(oppdragListe).containsExactlyInAnyOrder(oppdragskontroll, nyesteOppdragskontroll);

    }

    @Test
    void lagreOppdragslinje150() {
        // Arrange
        var oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        var oppdrLinje150 = OppdragTestDataHelper.buildOppdragslinje150(oppdr110);

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert
        var id = oppdrLinje150.getId();
        Assertions.assertThat(id).isNotNull();

        var oppdrLinje150Lest = entityManager.find(Oppdragslinje150.class, id);
        assertThat(oppdrLinje150Lest).isNotNull();
    }

    @Test
    void lagreRefusjonsinfo156() {
        // Arrange
        var oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        var oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        var oppdrLinje150 = OppdragTestDataHelper.buildOppdragslinje150(oppdr110);

        OppdragTestDataHelper.buildRefusjonsinfo156(oppdrLinje150);

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert
        var id = oppdrLinje150.getId();
        Assertions.assertThat(id).isNotNull();

        var oppdrLinje150Lest = entityManager.find(Oppdragslinje150.class, id);
        assertThat(oppdrLinje150Lest).isNotNull();
        var refusjonsinfo156Lest = oppdrLinje150Lest.getRefusjonsinfo156();
        assertThat(refusjonsinfo156Lest).isNotNull();
        assertThat(refusjonsinfo156Lest.getId()).isNotZero();
    }
}
