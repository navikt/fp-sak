package no.nav.foreldrepenger.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Attestant180;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Grad170;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
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
    public void lagreOgSøkeOppOppdragskontroll() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        Long behandlingId = oppdrkontroll.getBehandlingId();

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert
        Long id = oppdrkontroll.getId();
        assertThat(id).isNotNull();

        Oppdragskontroll oppdrkontrollLest = økonomioppdragRepository.finnVentendeOppdrag(behandlingId);

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
            LocalDate.now(), "REFUTG");
        assertThat(oppdragListe.stream().map(o -> o.getOppdragskontroll().getBehandlingId())).contains(behandlingId);
    }

    @Test
    public void lagreOgSøkeOppOppdragskontrollForPeriodeUtenResultat() {
        //Testene kjøres ikke mot tom db
        var førSize = økonomioppdragRepository.hentOppdrag110ForPeriodeOgFagområde(LocalDate.now().minusDays(1),
            LocalDate.now().minusDays(1), "REFUTG").size();
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert

        var oppdragListe = økonomioppdragRepository.hentOppdrag110ForPeriodeOgFagområde(LocalDate.now().minusDays(1),
            LocalDate.now().minusDays(1), "REFUTG").size();
        assertThat(oppdragListe).isEqualTo(førSize);
    }

    @Test
    public void lagreOgSøkeOppOppdragskontrollDerKvitteringErMottatt() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        oppdrkontroll.setVenterKvittering(false);
        Long behandlingId = oppdrkontroll.getBehandlingId();

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert
        Long id = oppdrkontroll.getId();
        assertThat(id).isNotNull();
        assertThatThrownBy(() -> økonomioppdragRepository.finnVentendeOppdrag(behandlingId))
            .hasMessageContaining("F-650018");
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
        OppdragKvitteringTestUtil.lagPositivKvitting(oppdrag110);

        // Act
        long id = økonomioppdragRepository.lagre(oppdrkontroll);

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
    public void lagreGrad170() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        Oppdrag110 oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        Oppdragslinje150 oppdrLinje150 = OppdragTestDataHelper.buildOppdragslinje150(oppdr110);

        OppdragTestDataHelper.buildGrad170(oppdrLinje150);

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert
        Long id = oppdrLinje150.getId();
        assertThat(id).isNotNull();

        Oppdragslinje150 oppdrLinje150Lest = entityManager.find(Oppdragslinje150.class, id);
        assertThat(oppdrLinje150Lest).isNotNull();
        Grad170 grad170Lest = oppdrLinje150Lest.getGrad170Liste().get(0);
        assertThat(grad170Lest).isNotNull();
        assertThat(grad170Lest.getId()).isNotEqualTo(0);
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

    @Test
    public void lagreAttestant180() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        Oppdrag110 oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        Oppdragslinje150 oppdrLinje150 = OppdragTestDataHelper.buildOppdragslinje150(oppdr110);

        Attestant180.Builder attestant180Builder = Attestant180.builder();
        Attestant180 attestant180 = attestant180Builder
            .medAttestantId("E8798765")
            .medOppdragslinje150(oppdrLinje150)
            .build();

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert
        Long id = attestant180.getId();
        assertThat(id).isNotNull();

        Attestant180 attestant180Lest = entityManager.find(Attestant180.class, id);
        assertThat(attestant180Lest).isNotNull();
    }

}
