package no.nav.foreldrepenger.behandlingslager.behandling.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class OpptjeningRepositoryImplTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    private final EntityManager em = repoRule.getEntityManager();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(em);
    private BasicBehandlingBuilder behandlingBuilder = new BasicBehandlingBuilder(em);
    private final OpptjeningRepository opptjeningRepository = new OpptjeningRepository(em, repositoryProvider.getBehandlingRepository());

    @Test
    public void skal_lagre_opptjeningsperiode() throws Exception {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        Behandling behandling = opprettBehandling();

        // Act
        Opptjening opptjeningsperiode = opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);

        // Assert
        assertThat(opptjeningsperiode.getFom()).isEqualTo(today);
        assertThat(opptjeningsperiode.getTom()).isEqualTo(tomorrow);

        assertThat(opptjeningsperiode.getOpptjeningAktivitet()).isEmpty();
        assertThat(opptjeningsperiode.getOpptjentPeriode()).isNull();

        // Act
        Opptjening funnet = opptjeningRepository.finnOpptjening(behandling.getId()).orElseThrow();

        // Assert
        assertThat(funnet).isEqualTo(opptjeningsperiode);
    }

    @Test
    public void kopierGrunnlagFraEksisterendeBehandling() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        Behandling behandling = opprettBehandling();
        Behandling revurdering = opprettBehandling();
        Opptjening opptjeningsperiode = opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);

        // Act
        opptjeningRepository.kopierGrunnlagFraEksisterendeBehandling(behandling, revurdering);
        Opptjening funnet = opptjeningRepository.finnOpptjening(revurdering.getId()).orElseThrow();

        // Assert
        assertThat(funnet).isEqualTo(opptjeningsperiode);
    }

    @Test
    public void deaktiverOpptjening() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        Behandling behandling = opprettBehandling();

        // Act
        opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);
        opptjeningRepository.deaktiverOpptjening(behandling);

        // Assert
        Optional<Opptjening> funnetOpt = opptjeningRepository.finnOpptjening(behandling.getId());
        assertThat(funnetOpt).isEmpty();
    }

    @Test
    public void lagreOpptjeningResultat() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        Behandling behandling = opprettBehandling();
        List<OpptjeningAktivitet> aktiviteter = new ArrayList<>();
        OpptjeningAktivitet opptjeningAktivitet = new OpptjeningAktivitet(tomorrow.minusMonths(10),
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
        Opptjening funnet = opptjeningRepository.finnOpptjening(behandling.getId()).orElseThrow();
        assertThat(funnet.getOpptjeningAktivitet()).hasSize(1);
        OpptjeningAktivitet aktivitet = funnet.getOpptjeningAktivitet().get(0);
        assertThat(aktivitet.getFom()).isEqualTo(tomorrow.minusMonths(10));
        assertThat(aktivitet.getTom()).isEqualTo(tomorrow);
        assertThat(aktivitet.getAktivitetReferanseType()).isEqualTo(ReferanseType.ORG_NR);
        assertThat(aktivitet.getAktivitetType()).isEqualTo(OpptjeningAktivitetType.ARBEID);
        assertThat(aktivitet.getAktivitetReferanse()).isEqualTo("abc");
    }

    @Test
    public void finnAktivGrunnlagId() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        Behandling behandling = opprettBehandling();
        Opptjening opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, today, tomorrow, false);

        // Act
        EndringsresultatSnapshot endringsresultatSnapshot = opptjeningRepository.finnAktivGrunnlagId(behandling);

        // Assert
        assertThat(endringsresultatSnapshot.getGrunnlagId()).isEqualTo(opptjening.getId());
    }

    private Behandling opprettBehandling() {
        Behandling behandling = behandlingBuilder.opprettOgLagreFørstegangssøknad(FagsakYtelseType.FORELDREPENGER);
        var resultat = Behandlingsresultat.builder().build();
        behandlingBuilder.lagreBehandlingsresultat(behandling.getId(), resultat);
        
        var vilkårResultat = VilkårResultat.builder().build();
        behandlingBuilder.lagreVilkårResultat(behandling.getId(), vilkårResultat);
        return behandling;
    }

    @Test
    public void getOpptjeningAktivitetTypeForKode() {
        // Act
        String næringKode = OpptjeningAktivitetType.NÆRING.getKode();
        OpptjeningAktivitetType næring = OpptjeningAktivitetType.fraKode(næringKode);

        // Assert
        assertThat(næring.getKode()).isEqualTo(næringKode);
        assertThat(næring.getNavn()).isNotJavaBlank();
    }

    @Test
    public void getOpptjeningAktivitetKlassifisering() {
        // Act
        String kode = OpptjeningAktivitetKlassifisering.BEKREFTET_GODKJENT.getKode();
        OpptjeningAktivitetKlassifisering resultat = OpptjeningAktivitetKlassifisering.fraKode(kode);

        // Assert
        assertThat(resultat.getKode()).isEqualTo(kode);
        assertThat(resultat.getNavn()).isNotJavaBlank();
    }
}
