package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

public class OppgittPeriodeMottattDatoTjenesteTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private OppgittPeriodeMottattDatoTjeneste tjeneste;

    @BeforeEach
    public void before() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        tjeneste = new OppgittPeriodeMottattDatoTjeneste(new YtelseFordelingTjeneste(new YtelsesFordelingRepository(getEntityManager())));
    }

    @Test
    public void skalFinneMottattDatoFraOriginalBehandlingHvisMatchendePeriode() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalMottattDato = LocalDate.of(2020, 10, 10);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriode(fom, tom)
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .medMottattDato(originalMottattDato)
                .build());
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medFordeling(new OppgittFordelingEntitet(originalBehandlingPerioder, true))
                .lagre(repositoryProvider);

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                .lagre(repositoryProvider);

        var periode = OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriode(fom, tom)
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .build();

        var mottattDatoForPeriode = tjeneste.finnMottattDatoForPeriode(behandling, periode);
        assertThat(mottattDatoForPeriode.orElseThrow()).isEqualTo(originalMottattDato);
    }

    @Test
    public void skalIkkeFinneMottattDatoFraOriginalBehandlingHvisIkkeMatchendePeriode() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalMottattDato = LocalDate.of(2020, 10, 10);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriode(fom, tom)
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medMottattDato(originalMottattDato)
                .build());
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medFordeling(new OppgittFordelingEntitet(originalBehandlingPerioder, true))
                .lagre(repositoryProvider);

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                .lagre(repositoryProvider);

        var periode = OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriode(fom, tom)
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .build();

        var mottattDatoForPeriode = tjeneste.finnMottattDatoForPeriode(behandling, periode);
        assertThat(mottattDatoForPeriode).isEmpty();
    }

    @Test
    public void skalFinneMottattDatoFraOriginalBehandlingHvisOrignalPeriodeOmslutter() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalMottattDato = LocalDate.of(2020, 10, 10);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriode(fom, tom)
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medMottattDato(originalMottattDato)
                .build());
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medFordeling(new OppgittFordelingEntitet(originalBehandlingPerioder, true))
                .lagre(repositoryProvider);

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                .lagre(repositoryProvider);

        var periode = OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriode(fom, tom.minusWeeks(1))
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .build();

        var mottattDatoForPeriode = tjeneste.finnMottattDatoForPeriode(behandling, periode);
        assertThat(mottattDatoForPeriode.orElseThrow()).isEqualTo(originalMottattDato);
    }

    @Test
    public void skalIkkeFinneMottattDatoFraOriginalBehandlingHvisOrignalPeriodeOverlapperMenIkkeOmslutter() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);
        var originalMottattDato = LocalDate.of(2020, 10, 10);
        var originalBehandlingPerioder = List.of(OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriode(fom, tom)
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medMottattDato(originalMottattDato)
                .build());
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medFordeling(new OppgittFordelingEntitet(originalBehandlingPerioder, true))
                .lagre(repositoryProvider);

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                .lagre(repositoryProvider);

        var periode = OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriode(fom.plusWeeks(1), tom.plusWeeks(1))
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .build();

        var mottattDatoForPeriode = tjeneste.finnMottattDatoForPeriode(behandling, periode);
        assertThat(mottattDatoForPeriode).isEmpty();
    }
}
