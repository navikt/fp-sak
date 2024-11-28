package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.SøknadUtsettelseUttakDato;
import no.nav.foreldrepenger.skjæringstidspunkt.TomtUttakTjeneste;

@CdiDbAwareTest
class DokumentmottakerFellesTest {


    @Inject
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    @Inject
    private TomtUttakTjeneste tomtUttakTjeneste;


    @Test
    void uttak_fom_lørdag_og_utsettelse_fra_mandag_skal_gi_annulleringsbehandling() {
        var mottatteDokumentTjeneste = mock(MottatteDokumentTjeneste.class);
        var utsettelseFom = LocalDate.of(2024, 11, 18);
        var mottattDokument = mock(MottattDokument.class);
        when(mottatteDokumentTjeneste.finnUtsettelseUttakForSøknad(mottattDokument)).thenReturn(new SøknadUtsettelseUttakDato(
            utsettelseFom, LocalDate.of(2025, 10, 10)));
        var dokumentmottakerFelles = new DokumentmottakerFelles(behandlingRepositoryProvider, null, null, null, null, mottatteDokumentTjeneste, null,
            tomtUttakTjeneste);

        var originalUttakFom = LocalDate.of(2024, 11, 16);
        var uttaksperiode = new UttakResultatPeriodeEntitet.Builder(originalUttakFom, originalUttakFom.plusMonths(2))
            .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.KVOTE_ELLER_OVERFØRT_KVOTE)
            .build();

        new UttakResultatPeriodeAktivitetEntitet.Builder(uttaksperiode,
            new UttakAktivitetEntitet.Builder().medUttakArbeidType(UttakArbeidType.FRILANS).build())
            .medTrekkdager(new Trekkdager(10))
            .medUtbetalingsgrad(Utbetalingsgrad.HUNDRED)
            .medArbeidsprosent(BigDecimal.ZERO)
            .build();

        var førstegangsbehandling = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medUttak(new UttakResultatPerioderEntitet().leggTilPeriode(uttaksperiode))
            .lagre(behandlingRepositoryProvider);
        førstegangsbehandling.avsluttBehandling();
        behandlingRepositoryProvider.getBehandlingRepository().lagre(førstegangsbehandling, behandlingRepositoryProvider.getBehandlingRepository()
            .taSkriveLås(førstegangsbehandling));
        var revurdering = ScenarioFarSøkerForeldrepenger.forFødsel()
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
            .lagre(behandlingRepositoryProvider);
        var endringSomUtsetterStartdato = dokumentmottakerFelles.endringSomUtsetterStartdato(mottattDokument, revurdering.getFagsak());

        assertThat(endringSomUtsetterStartdato).isTrue();
    }

}
