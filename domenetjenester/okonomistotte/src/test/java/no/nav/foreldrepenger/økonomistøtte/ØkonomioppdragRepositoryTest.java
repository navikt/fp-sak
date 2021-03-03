package no.nav.foreldrepenger.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class ØkonomioppdragRepositoryTest extends EntityManagerAwareTest {

    private EntityManager entityManager;
    private ØkonomioppdragRepository økonomioppdragRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        this.entityManager = entityManager;
        økonomioppdragRepository = new ØkonomioppdragRepository(entityManager);
    }

    @Test
    public void lagreOgHenteOppdragskontroll() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert
        Long id = oppdrkontroll.getId();
        assertThat(id).isNotNull();

        Oppdragskontroll oppdrkontrollLest = økonomioppdragRepository.hentOppdragskontroll(id);

        assertThat(oppdrkontrollLest).isNotNull();
    }

    @Test
    public void lagreOgSøkeOppOppdragskontrollForPeriode() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        Long behandlingId = oppdrkontroll.getBehandlingId();
        OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert

        List<Oppdrag110> oppdragListe = økonomioppdragRepository.hentOppdrag110ForPeriodeOgFagområde(LocalDate.now(),
            LocalDate.now(), KodeFagområde.ENGANGSSTØNAD);
        assertThat(oppdragListe.stream().map(o -> o.getOppdragskontroll().getBehandlingId())).contains(behandlingId);
    }

    @Test
    public void finnAlleOppdragUtenKvittering() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        Oppdrag110 oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        OppdragTestDataHelper.buildOppdragslinje150(oppdr110);

        økonomioppdragRepository.lagre(oppdrkontroll);

        var oppdrag110 = økonomioppdragRepository.hentOppdragUtenKvittering(oppdr110.getFagsystemId(), oppdrkontroll.getBehandlingId());

        assertThat(oppdrag110).isNotNull();
    }

    @Test
    public void kastExceptionHvisFlereOppdragUtenKvitteringFinnes() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        Oppdrag110 oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        OppdragTestDataHelper.buildOppdragslinje150(oppdr110);
        Oppdrag110 oppdr110_2 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        OppdragTestDataHelper.buildOppdragslinje150(oppdr110_2);

        økonomioppdragRepository.lagre(oppdrkontroll);
        assertThatThrownBy(() ->
            økonomioppdragRepository.hentOppdragUtenKvittering(oppdr110.getFagsystemId(), oppdrkontroll.getBehandlingId()))
            .hasMessageContaining("returnerte mer enn eksakt ett resultat");
    }

    @Test
    public void okHvisFlereOppdragFinnesMenKunEnnUtenKvittering() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        Oppdrag110 oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        OppdragTestDataHelper.buildOppdragslinje150(oppdr110);
        Oppdrag110 oppdr110_2 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
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
    public void lagreOgSøkeOppOppdragskontrollForPeriodeUtenResultat() {
        //Testene kjøres ikke mot tom db
        var førSize = økonomioppdragRepository.hentOppdrag110ForPeriodeOgFagområde(LocalDate.now().minusDays(1),
            LocalDate.now().minusDays(1), KodeFagområde.ENGANGSSTØNAD).size();
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert

        var oppdragListe = økonomioppdragRepository.hentOppdrag110ForPeriodeOgFagområde(LocalDate.now().minusDays(1),
            LocalDate.now().minusDays(1), KodeFagområde.ENGANGSSTØNAD).size();
        assertThat(oppdragListe).isEqualTo(førSize);
    }

    @Test
    public void lagreOppdrag110() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);

        // Act
        long id = økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert

        Oppdragskontroll oppdrkontrollLest = økonomioppdragRepository.hentOppdragskontroll(id);
        assertThat(oppdrkontrollLest.getOppdrag110Liste()).hasSize(1);
        Oppdrag110 oppdr110Lest = oppdrkontrollLest.getOppdrag110Liste().get(0);
        assertThat(oppdr110Lest).isNotNull();
        assertThat(oppdr110Lest.getId()).isNotEqualTo(0);
    }

    @Test
    public void lagreOppdragKvittering() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        Oppdrag110 oppdrag110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        long id = økonomioppdragRepository.lagre(oppdrkontroll);

        var oppdragKvittering = OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag110);

        // Act
        økonomioppdragRepository.lagre(oppdragKvittering);

        // Assert

        Oppdragskontroll oppdrkontrollLest = økonomioppdragRepository.hentOppdragskontroll(id);
        assertThat(oppdrkontrollLest.getOppdrag110Liste()).hasSize(1);
        Oppdrag110 oppdr110Lest = oppdrkontrollLest.getOppdrag110Liste().get(0);
        assertThat(oppdr110Lest).isNotNull();
        assertThat(oppdr110Lest.getId()).isNotEqualTo(0);
        OppdragKvittering oppdrKvittering = oppdr110Lest.getOppdragKvittering();
        assertThat(oppdrKvittering).isNotNull();
        assertThat(oppdrKvittering.getId()).isNotEqualTo(0);
    }

    @Test
    public void finnerAlleOppdragForSak() {
        Saksnummer saksnr = new Saksnummer("1234");
        long nyesteFagsystemId = 101L;

        Oppdragskontroll oppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll(saksnr, 1L);
        OppdragTestDataHelper.buildOppdrag110ES(oppdragskontroll, 100L);
        økonomioppdragRepository.lagre(oppdragskontroll);

        Oppdragskontroll nyesteOppdragskontroll = OppdragTestDataHelper.buildOppdragskontroll(saksnr, 2L);
        OppdragTestDataHelper.buildOppdrag110ES(nyesteOppdragskontroll, nyesteFagsystemId);
        økonomioppdragRepository.lagre(nyesteOppdragskontroll);


        List<Oppdragskontroll> oppdragListe = økonomioppdragRepository.finnAlleOppdragForSak(saksnr);
        assertThat(oppdragListe).hasSize(2);
        assertThat(oppdragListe).containsExactlyInAnyOrder(oppdragskontroll, nyesteOppdragskontroll);

    }

    @Test
    public void lagreOppdragslinje150() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        Oppdrag110 oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        Oppdragslinje150 oppdrLinje150 = OppdragTestDataHelper.buildOppdragslinje150(oppdr110);

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert
        Long id = oppdrLinje150.getId();
        assertThat(id).isNotNull();

        Oppdragslinje150 oppdrLinje150Lest = entityManager.find(Oppdragslinje150.class, id);
        assertThat(oppdrLinje150Lest).isNotNull();
    }

    @Test
    public void lagreRefusjonsinfo156() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        Oppdrag110 oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        Oppdragslinje150 oppdrLinje150 = OppdragTestDataHelper.buildOppdragslinje150(oppdr110);

        OppdragTestDataHelper.buildRefusjonsinfo156(oppdrLinje150);

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert
        Long id = oppdrLinje150.getId();
        assertThat(id).isNotNull();

        Oppdragslinje150 oppdrLinje150Lest = entityManager.find(Oppdragslinje150.class, id);
        assertThat(oppdrLinje150Lest).isNotNull();
        Refusjonsinfo156 refusjonsinfo156Lest = oppdrLinje150Lest.getRefusjonsinfo156();
        assertThat(refusjonsinfo156Lest).isNotNull();
        assertThat(refusjonsinfo156Lest.getId()).isNotEqualTo(0);
    }
}
