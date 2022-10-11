package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeSøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;

public class OppgittPeriodeTidligstMottattDatoTjenesteFilterTest extends EntityManagerAwareTest {

    private BehandlingRepositoryProvider repositoryProvider;

    private OppgittPeriodeTidligstMottattDatoTjeneste tjeneste;

    @BeforeEach
    public void before() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var ytelseFordelingTjeneste = new YtelseFordelingTjeneste(new YtelsesFordelingRepository(getEntityManager()));
        var uttakRepository = new FpUttakRepository(getEntityManager());
        tjeneste = new OppgittPeriodeTidligstMottattDatoTjeneste(
            ytelseFordelingTjeneste, uttakRepository);
    }

    @Test
    public void skalFiltrereVekkTidligSøknadsperiodeDersomHeltLikUttak() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef());
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1.build())
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.TEN).build();
        perioder.leggTilPeriode(uttakPeriode1);
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medUttak(perioder)
            .lagre(repositoryProvider);

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .lagre(repositoryProvider);

        // Lik eksisterende vedtak
        var søknad1 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();
        // Ny periode
        var søknad2 = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(tom.plusDays(1), tom.plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();

        var filtrert = tjeneste.filtrerVekkPerioderSomErLikeInnvilgetUttak(behandling, List.of(søknad1, søknad2));
        assertThat(filtrert).hasSize(1);
        assertThat(filtrert.get(0)).isEqualTo(søknad2);
    }

    @Test
    public void skalBeholdeSøknadsperiodeDersomHeltLikUttak() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef());
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1.build())
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.TEN).build();
        perioder.leggTilPeriode(uttakPeriode1);
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medUttak(perioder)
                .lagre(repositoryProvider);

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                .lagre(repositoryProvider);

        var søknad = OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medPeriode(fom, tom)
                .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
                .build();

        var filtrert = tjeneste.filtrerVekkPerioderSomErLikeInnvilgetUttak(behandling, List.of(søknad));
        assertThat(filtrert).hasSize(1);
        assertThat(filtrert.get(0)).isEqualTo(søknad);
    }

    @Test
    public void skalBeholdeSøknadsperiodeDersomVedtakErLengerEnnSøknad() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef());
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom.plusWeeks(4))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1.build())
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.TEN).build();
        perioder.leggTilPeriode(uttakPeriode1);
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medUttak(perioder)
            .lagre(repositoryProvider);

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .lagre(repositoryProvider);

        var søknad = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom)
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();

        var filtrert = tjeneste.filtrerVekkPerioderSomErLikeInnvilgetUttak(behandling, List.of(søknad));
        assertThat(filtrert).hasSize(1);
        assertThat(filtrert.get(0)).isEqualTo(søknad);
    }

    @Test
    public void skalAvkorteSøknadsperiodeDersomStrekkerSegForbiInnvilgetUttak() {
        var fom = LocalDate.of(2020, 10, 9);
        var tom = LocalDate.of(2020, 11, 9);

        var perioder = new UttakResultatPerioderEntitet();
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var arbeidsforhold1 = new UttakAktivitetEntitet.Builder()
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforhold(arbeidsgiver, InternArbeidsforholdRef.nyRef());
        var uttakPeriode1 = new UttakResultatPeriodeEntitet.Builder(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
            .medPeriodeSoknad(new UttakResultatPeriodeSøknadEntitet.Builder().medUttakPeriodeType(UttakPeriodeType.FORELDREPENGER).build())
            .build();
        UttakResultatPeriodeAktivitetEntitet.builder(uttakPeriode1, arbeidsforhold1.build())
            .medTrekkdager(new Trekkdager(42))
            .medTrekkonto(StønadskontoType.FORELDREPENGER)
            .medArbeidsprosent(BigDecimal.TEN).build();
        perioder.leggTilPeriode(uttakPeriode1);
        var originalBehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medUttak(perioder)
            .lagre(repositoryProvider);

        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .lagre(repositoryProvider);

        // Utvider søknadsperioden med 4 uker ift vedtak
        var søknad = OppgittPeriodeBuilder.ny()
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriode(fom, tom.plusWeeks(4))
            .medPeriodeType(UttakPeriodeType.FORELDREPENGER)
            .build();

        var filtrert = tjeneste.filtrerVekkPerioderSomErLikeInnvilgetUttak(behandling, List.of(søknad));
        assertThat(filtrert).hasSize(1);
        assertThat(filtrert.get(0).getFom()).isEqualTo(tom.plusDays(1));
    }

}
