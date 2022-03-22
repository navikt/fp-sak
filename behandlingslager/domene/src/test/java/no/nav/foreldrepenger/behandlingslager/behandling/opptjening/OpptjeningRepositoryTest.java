package no.nav.foreldrepenger.behandlingslager.behandling.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;

public class OpptjeningRepositoryTest extends EntityManagerAwareTest {

    private OpptjeningRepository opptjeningRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        opptjeningRepository = new OpptjeningRepository(entityManager, new BehandlingRepository(entityManager));
    }

    @Test
    public void skal_lagre_opptjeningsperiode() {
        // Arrange
        var today = LocalDate.now();
        var tomorrow = today.plusDays(1);

        var behandling = opprettBehandling();

        // Act
        var opptjeningsperiode = opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);

        // Assert
        assertThat(opptjeningsperiode.getFom()).isEqualTo(today);
        assertThat(opptjeningsperiode.getTom()).isEqualTo(tomorrow);

        assertThat(opptjeningsperiode.getOpptjeningAktivitet()).isEmpty();
        assertThat(opptjeningsperiode.getOpptjentPeriode()).isNull();

        // Act
        var funnet = opptjeningRepository.finnOpptjening(behandling.getId()).orElseThrow();

        // Assert
        assertThat(funnet).isEqualTo(opptjeningsperiode);
    }

    @Test
    public void kopierGrunnlagFraEksisterendeBehandling() {
        // Arrange
        var today = LocalDate.now();
        var tomorrow = today.plusDays(1);
        var behandling = opprettBehandling();
        var revurdering = opprettBehandling();
        var opptjeningsperiode = opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);

        // Act
        opptjeningRepository.kopierGrunnlagFraEksisterendeBehandling(behandling, revurdering);
        var funnet = opptjeningRepository.finnOpptjening(revurdering.getId()).orElseThrow();

        // Assert
        assertThat(funnet).isEqualTo(opptjeningsperiode);
    }

    @Test
    public void deaktiverOpptjening() {
        // Arrange
        var today = LocalDate.now();
        var tomorrow = today.plusDays(1);
        var behandling = opprettBehandling();

        // Act
        opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);
        opptjeningRepository.deaktiverOpptjening(behandling);

        // Assert
        var funnetOpt = opptjeningRepository.finnOpptjening(behandling.getId());
        assertThat(funnetOpt).isEmpty();
    }

    @Test
    public void lagreOpptjeningResultat() {
        // Arrange
        var today = LocalDate.now();
        var tomorrow = today.plusDays(1);
        var behandling = opprettBehandling();
        List<OpptjeningAktivitet> aktiviteter = new ArrayList<>();
        var opptjeningAktivitet = new OpptjeningAktivitet(tomorrow.minusMonths(10),
            tomorrow,
            OpptjeningAktivitetType.ARBEID,
            OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT,
            "abc",
            ReferanseType.ORG_NR);
        aktiviteter.add(opptjeningAktivitet);

        // Act
        opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);
        opptjeningRepository.lagreOpptjeningResultat(behandling, Period.ofDays(100), aktiviteter);

        // Assert
        var funnet = opptjeningRepository.finnOpptjening(behandling.getId()).orElseThrow();
        assertThat(funnet.getOpptjeningAktivitet()).hasSize(1);
        var aktivitet = funnet.getOpptjeningAktivitet().get(0);
        assertThat(aktivitet.getFom()).isEqualTo(tomorrow.minusMonths(10));
        assertThat(aktivitet.getTom()).isEqualTo(tomorrow);
        assertThat(aktivitet.getAktivitetReferanseType()).isEqualTo(ReferanseType.ORG_NR);
        assertThat(aktivitet.getAktivitetType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
        assertThat(aktivitet.getAktivitetReferanse()).isEqualTo("abc");
    }

    private Behandling opprettBehandling() {
        var behandlingBuilder = new BasicBehandlingBuilder(getEntityManager());
        var behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var resultat = Behandlingsresultat.builder().build();
        behandlingBuilder.lagreBehandlingsresultat(behandling.getId(), resultat);

        var vilkårResultat = VilkårResultat.builder().build();
        behandlingBuilder.lagreVilkårResultat(behandling.getId(), vilkårResultat);
        return behandling;
    }

    @Test
    public void getOpptjeningAktivitetTypeForKode() {
        // Act
        var næringKode = OpptjeningAktivitetType.NÆRING.getKode();
        var næring = OpptjeningAktivitetType.fraKode(næringKode);

        // Assert
        assertThat(næring.getKode()).isEqualTo(næringKode);
        assertThat(næring.getNavn()).isNotEmpty();
    }

}
