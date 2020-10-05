package no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class EndringsdatoRevurderingUtlederTest extends EntityManagerAwareTest {

    private static final LocalDate FØRSTE_DAG = LocalDate.now();
    private static final LocalDate SISTE_DAG = LocalDate.now().plusMonths(3);

    private UttakRepositoryProvider repositoryProvider;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SvangerskapspengerUttakResultatRepository uttakRepository;
    private EndringsdatoRevurderingUtlederImpl utleder;

    @BeforeEach
    public void setup() {
        var entityManager = getEntityManager();
        repositoryProvider = new UttakRepositoryProvider(entityManager);
        behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
        uttakRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
        utleder = new EndringsdatoRevurderingUtlederImpl(repositoryProvider);
    }

    @Test
    public void skal_utlede_endringsdato_fra_uttak_resultat() {
        // Arrange
        ScenarioMorSøkerSvangerskapspenger scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        Behandlingsresultat.Builder behandlingresultatBuilder = Behandlingsresultat.builder();
        behandlingresultatBuilder.medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        scenario.medBehandlingsresultat(behandlingresultatBuilder);
        Behandling behandling = scenario.lagre(repositoryProvider);

        SvangerskapspengerUttakResultatPeriodeEntitet uttakPeriode = new SvangerskapspengerUttakResultatPeriodeEntitet
            .Builder(FØRSTE_DAG, SISTE_DAG)
            .medRegelInput("{}")
            .medRegelEvaluering("{}")
            .medUtbetalingsgrad(BigDecimal.valueOf(100L))
            .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak._8308_SØKT_FOR_SENT)
            .medPeriodeResultatType(PeriodeResultatType.AVSLÅTT)
            .build();

        SvangerskapspengerUttakResultatArbeidsforholdEntitet uttakArbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.person(AktørId.dummy()), null)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medPeriode(uttakPeriode)
            .build();

        SvangerskapspengerUttakResultatEntitet uttakResultat = new SvangerskapspengerUttakResultatEntitet
            .Builder(behandlingsresultatRepository.hent(behandling.getId()))
            .medUttakResultatArbeidsforhold(uttakArbeidsforhold)
            .build();

        uttakRepository.lagre(behandling.getId(), uttakResultat);

        // Act
        UttakInput input = new UttakInput(BehandlingReferanse.fra(behandling), null, null);
        LocalDate resultat = utleder.utledEndringsdato(input);

        // Assert
        assertThat(resultat).isEqualTo(FØRSTE_DAG);
    }
}
