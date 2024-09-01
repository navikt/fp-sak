package no.nav.foreldrepenger.domene.uttak;

import static no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType.AVSLÅTT;
import static no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType.INNVILGET;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class OpphørUttakTjenesteTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final OpphørUttakTjeneste opphørUttakTjeneste = new OpphørUttakTjeneste(repositoryProvider);

    private Behandling opprettOriginalBehandling(UttakRepositoryProvider repositoryProvider) {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void skal_kun_komme_når_behandlingsresultatet_er_av_typen_opphør() {
        var skjæringstidspunkt = LocalDate.now();
        var originalBehandling = opprettOriginalBehandling(repositoryProvider);
        var revurdering = opprettOpphørtRevurdering(originalBehandling);

        lagreSkjæringstidspunkt(revurdering, skjæringstidspunkt);
        var ref = BehandlingReferanse.fra(revurdering);
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(skjæringstidspunkt).build();

        var kallPåOpphørTjenesteForOpphørBehandling = opphørUttakTjeneste.getOpphørsdato(ref, stp,
            getBehandlingsresultat(revurdering.getId()));
        assertThat(kallPåOpphørTjenesteForOpphørBehandling).isNotEmpty();

        var kallPåOpphørTjenesteForInnvilgetBehandling = opphørUttakTjeneste.getOpphørsdato(ref, stp,
            getBehandlingsresultat(originalBehandling.getId()));
        assertThat(kallPåOpphørTjenesteForInnvilgetBehandling).isEmpty();
    }

    private Behandling opprettOpphørtRevurdering(Behandling originalBehandling) {
        var behandlingsresultat = Behandlingsresultat.builder()
            .medBehandlingResultatType(BehandlingResultatType.OPPHØR);
        return opprettRevurdering(originalBehandling, behandlingsresultat);
    }

    private Behandling opprettRevurdering(Behandling originalBehandling, Behandlingsresultat.Builder behandlingsresultat) {
        return ScenarioMorSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(originalBehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .medBehandlingsresultat(behandlingsresultat.build())
            .lagre(repositoryProvider);
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return repositoryProvider.getBehandlingsresultatRepository().hent(behandlingId);
    }

    @Test
    void skal_returnere_fom_dato_til_tidligste_opphørte_periode_etter_seneste_innvilgede_periode() {
        // arrange
        var skjæringstidspunkt = LocalDate.now();
        var originalBehandling = opprettOriginalBehandling(repositoryProvider);
        var revurdering = opprettOpphørtRevurdering(originalBehandling);
        lagreSkjæringstidspunkt(revurdering, skjæringstidspunkt);
        var ref = BehandlingReferanse.fra(revurdering);
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(skjæringstidspunkt).build();
        var opphørsÅrsaker = PeriodeResultatÅrsak.opphørsAvslagÅrsaker().iterator();
        new MockUttakResultatBuilder(skjæringstidspunkt.plusDays(10))
            .medInnvilgetPeriode(PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER, 10)
            .medInnvilgetPeriode(PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER, 10)
            .medAvslåttPeriode(opphørsÅrsaker.next(), 10)
            .medInnvilgetPeriode(PeriodeResultatÅrsak.FELLESPERIODE_ELLER_FORELDREPENGER, 10)
            .medAvslåttPeriode(opphørsÅrsaker.next(), 10)
            .medAvslåttPeriode(opphørsÅrsaker.next(), 10)
            .buildFor(revurdering.getId());
        // act
        var opphørsdato = opphørUttakTjeneste.getOpphørsdato(ref, stp, getBehandlingsresultat(revurdering.getId()));
        // assert
        assertThat(opphørsdato.orElseThrow()).isEqualTo(skjæringstidspunkt.plusDays(54));
    }

    @Test
    void skal_bruke_skjæringstidspunkt_hvis_alle_perioder_har_fått_opphør_eller_avslagsårsak() {
        // arrange
        var originalBehandling = opprettOriginalBehandling(repositoryProvider);
        var revurdering = opprettOpphørtRevurdering(originalBehandling);
        var skjæringstidspunkt = lagreSkjæringstidspunkt(revurdering, LocalDate.now());
        var ref = BehandlingReferanse.fra(revurdering);
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(skjæringstidspunkt).build();
        var opphørsÅrsaker = PeriodeResultatÅrsak.opphørsAvslagÅrsaker().iterator();
        new MockUttakResultatBuilder(skjæringstidspunkt.plusDays(7))
            .medAvslåttPeriode(opphørsÅrsaker.next(), 14).medAvslåttPeriode(opphørsÅrsaker.next(), 61)
            .medAvslåttPeriode(opphørsÅrsaker.next(), 14).medAvslåttPeriode(opphørsÅrsaker.next(), 62)
            .buildFor(revurdering.getId());
        // act
        var opphørsdato = opphørUttakTjeneste.getOpphørsdato(ref, stp, getBehandlingsresultat(revurdering.getId()));
        // assert
        assertThat(opphørsdato.orElseThrow()).isEqualTo(skjæringstidspunkt);
    }

    private LocalDate lagreSkjæringstidspunkt(Behandling behandling, LocalDate skjæringstidspunkt) {
        var avklarteUttakDatoerEntitet = new AvklarteUttakDatoerEntitet.Builder()
            .medFørsteUttaksdato(skjæringstidspunkt)
            .build();

        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandling.getId())
            .medAvklarteDatoer(avklarteUttakDatoerEntitet);
        ytelsesFordelingRepository.lagre(behandling.getId(), yfBuilder.build());

        return skjæringstidspunkt;
    }

    private class MockUttakResultatBuilder {
        private final UttakResultatPerioderEntitet uttakResultatPerioder;
        private LocalDate fom;

        MockUttakResultatBuilder(LocalDate fom) {
            this.fom = fom;
            uttakResultatPerioder = new UttakResultatPerioderEntitet();
        }

        MockUttakResultatBuilder medInnvilgetPeriode(PeriodeResultatÅrsak innvilgetårsak, int varighetIDager) {
            return this.medPeriode(INNVILGET, innvilgetårsak, varighetIDager);
        }

        MockUttakResultatBuilder medAvslåttPeriode(PeriodeResultatÅrsak opphørEllerAvslagsårsak, int varighetIDager) {
            return this.medPeriode(AVSLÅTT, opphørEllerAvslagsårsak, varighetIDager);
        }

        private MockUttakResultatBuilder medPeriode(PeriodeResultatType type, PeriodeResultatÅrsak innvilgetårsak, int varighetIDager) {
            leggTilPeriode(varighetIDager, type, innvilgetårsak);
            oppdaterFom(varighetIDager);
            return this;
        }

        void buildFor(Long behandlingId) {
            repositoryProvider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandlingId, uttakResultatPerioder);
        }

        private void oppdaterFom(int varighetDager) {
            fom = fom.plusDays(varighetDager + 1);
        }

        private void leggTilPeriode(int varighetDager, PeriodeResultatType resultatType, PeriodeResultatÅrsak årsak) {
            var periode = new UttakResultatPeriodeEntitet.Builder(fom, fom.plusDays(varighetDager))
                .medResultatType(resultatType, årsak).build();
            uttakResultatPerioder.leggTilPeriode(periode);
        }
    }
}
