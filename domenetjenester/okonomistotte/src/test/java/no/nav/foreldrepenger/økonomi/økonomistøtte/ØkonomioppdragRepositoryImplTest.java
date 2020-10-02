package no.nav.foreldrepenger.økonomi.økonomistøtte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Attestant180;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Avstemming115;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Grad170;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.OppdragKvittering;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragsenhet120;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Refusjonsinfo156;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class ØkonomioppdragRepositoryImplTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final Repository repository = repoRule.getRepository();

    private final EntityManager entityManager = repoRule.getEntityManager();
    private final ØkonomioppdragRepository økonomioppdragRepository = new ØkonomioppdragRepository( entityManager);


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
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert

        List<Oppdrag110> oppdragListe = økonomioppdragRepository.hentOppdrag110ForPeriodeOgFagområde(LocalDate.now().minusDays(1), LocalDate.now().minusDays(1), "REFUTG");
        assertThat(oppdragListe).isEmpty();
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

        try {
            økonomioppdragRepository.finnVentendeOppdrag(behandlingId);
            fail("Ventet exception");
        } catch (TekniskException te) {
            assertThat(te.getMessage()).contains("F-650018");
        }
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
    public void lagreAvstemming115() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();

        Avstemming115 avstemming115 = OppdragTestDataHelper.buildAvstemming115();

        OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L, avstemming115);

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert
        Long id = avstemming115.getId();
        assertThat(id).isNotNull();

        Avstemming115 avst115Lest = repository.hent(Avstemming115.class, id);
        assertThat(avst115Lest).isNotNull();
    }

    @Test
    public void lagreOppdragsenhet120() {
        // Arrange
        Oppdragskontroll oppdrkontroll = OppdragTestDataHelper.buildOppdragskontroll();
        Oppdrag110 oppdr110 = OppdragTestDataHelper.buildOppdrag110ES(oppdrkontroll, 44L);
        Oppdragsenhet120 oppdrsEnhet120 = OppdragTestDataHelper.buildOppdragsEnhet120(oppdr110);

        // Act
        økonomioppdragRepository.lagre(oppdrkontroll);

        // Assert
        Long id = oppdrsEnhet120.getId();
        assertThat(id).isNotNull();

        Oppdragsenhet120 oppdrsEnhet120Lest = repository.hent(Oppdragsenhet120.class, id);
        assertThat(oppdrsEnhet120Lest).isNotNull();
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

        Oppdragslinje150 oppdrLinje150Lest = repository.hent(Oppdragslinje150.class, id);
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

        Oppdragslinje150 oppdrLinje150Lest = repository.hent(Oppdragslinje150.class, id);
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

        Oppdragslinje150 oppdrLinje150Lest = repository.hent(Oppdragslinje150.class, id);
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

        Attestant180 attestant180Lest = repository.hent(Attestant180.class, id);
        assertThat(attestant180Lest).isNotNull();
    }

}
