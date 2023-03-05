package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class EndringsdatoRevurderingUtlederTest {

    private static final LocalDate FØRSTE_DAG = LocalDate.now();
    private static final LocalDate SISTE_DAG = LocalDate.now().plusMonths(3);

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final BehandlingsresultatRepository behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    private final SvangerskapspengerUttakResultatRepository uttakRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
    private final EndringsdatoRevurderingUtlederImpl utleder = new EndringsdatoRevurderingUtlederImpl(repositoryProvider);

    @Test
    void skal_utlede_endringsdato_fra_uttak_resultat() {
        // Arrange
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandlingresultatBuilder = Behandlingsresultat.builder();
        behandlingresultatBuilder.medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        scenario.medBehandlingsresultat(behandlingresultatBuilder.build());
        var behandling = scenario.lagre(repositoryProvider);

        var uttakPeriode = new SvangerskapspengerUttakResultatPeriodeEntitet
            .Builder(FØRSTE_DAG, SISTE_DAG)
            .medRegelInput("{}")
            .medRegelEvaluering("{}")
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak._8308_SØKT_FOR_SENT)
            .medPeriodeResultatType(PeriodeResultatType.AVSLÅTT)
            .build();

        var uttakArbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.person(AktørId.dummy()), null)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medPeriode(uttakPeriode)
            .build();

        var uttakResultat = new SvangerskapspengerUttakResultatEntitet
            .Builder(behandlingsresultatRepository.hent(behandling.getId()))
            .medUttakResultatArbeidsforhold(uttakArbeidsforhold)
            .build();

        uttakRepository.lagre(behandling.getId(), uttakResultat);

        // Act
        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, null);
        var resultat = utleder.utledEndringsdato(input);

        // Assert
        assertThat(resultat).isEqualTo(FØRSTE_DAG);
    }
}
