package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.BerørtBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;

public class BerørtBehandlingTjenesteImplTest {

    private BerørtBehandlingTjeneste berørtBehandlingTjeneste;
    @Mock
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    @Mock
    private HistorikkRepository historikkRepository;
    @Mock
    BehandlingRepositoryProvider repositoryProvider;
    @Mock
    UttakRepository uttakRepository;
    @Mock
    private UttakInputTjeneste uttakInputTjeneste;

    private Behandling revurdering;

    @Before
    public void fellesOppsett() {
        stønadskontoSaldoTjeneste = mock(StønadskontoSaldoTjeneste.class);
        historikkRepository = mock(HistorikkRepository.class);
        uttakRepository = mock(UttakRepository.class);
        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        uttakInputTjeneste = mock(UttakInputTjeneste.class);
        when(repositoryProvider.getHistorikkRepository()).thenReturn(historikkRepository);
        when(repositoryProvider.getUttakRepository()).thenReturn(uttakRepository);
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, new NavBrukerBuilder().medAktørId(AktørId.dummy()).build());
        var førsteBehandling = Behandling.forFørstegangssøknad(fagsak).build();
        revurdering = Behandling.fraTidligereBehandling(førsteBehandling, BehandlingType.REVURDERING).build();
        revurdering.getOriginalBehandling();
        berørtBehandlingTjeneste = new BerørtBehandlingTjeneste(stønadskontoSaldoTjeneste, repositoryProvider, uttakInputTjeneste,
            new ForeldrepengerUttakTjeneste(repositoryProvider.getUttakRepository()));
    }

    //Scenarie 1 - Opphør
    @Test
    public void skal_opprette_berørt_behandling_dersom_behandlingsresultater_er_opphør() {
        var behandlingsresultat = lagBehandlingsresultat(BehandlingResultatType.OPPHØR, KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER, false);
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandlingsresultat.get().getBehandling()), null, new ForeldrepengerGrunnlag());
        var behandling = behandlingsresultat.get().getBehandling();
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);

        assertThat(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(behandlingsresultat,
            lagUttakResultatPerioder(LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), false, false),
            lagUttakResultatPerioder(LocalDate.now().plusDays(6), LocalDate.now().plusDays(12), false, false))).isTrue();
    }

    //Scenarie 2a - endring i beregning
    @Test
    public void skal_opprette_berørt_behandling_dersom_revudering_er_innvilget_med_endring_i_stønadskonto() {
        var behandlingsresultat = lagBehandlingsresultat(BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.ENDRING_I_BEREGNING, true);
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandlingsresultat.get().getBehandling()), null, new ForeldrepengerGrunnlag());
        var behandling = behandlingsresultat.get().getBehandling();
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);

        assertThat(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(behandlingsresultat, Optional.empty(), Optional.empty())).isTrue();
    }

    //Scenarie 2b - endring i uttak
    @Test
    public void skal_opprette_berørt_behandling_dersom_revudering_er_innvilget_med_endring_i_uttak() {
        var behandlingsresultat = lagBehandlingsresultat(BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.ENDRING_I_UTTAK, true);
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandlingsresultat.get().getBehandling()), null, new ForeldrepengerGrunnlag());
        var behandling = behandlingsresultat.get().getBehandling();
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);

        assertThat(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(behandlingsresultat, Optional.empty(), Optional.empty())).isTrue();
    }

    //Scenarie 4a - foreldrepenger er endret og uttak har overlappende perioder
    @Test
    public void skal_opprette_berørt_behandling_dersom_revudering_er_endring_i_foreldrepenger_med_endring_i_uttak_overlappende_perioder() {
        var behandlingsresultat = lagBehandlingsresultat(BehandlingResultatType.FORELDREPENGER_ENDRET, KonsekvensForYtelsen.ENDRING_I_UTTAK, false);

        var behandling = behandlingsresultat.get().getBehandling();
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag());
        when(stønadskontoSaldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)).thenReturn(false);
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);

        assertThat(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(behandlingsresultat,
            lagUttakResultatPerioder(LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), false, false),
            lagUttakResultatPerioder(LocalDate.now().minusDays(12), LocalDate.now().plusDays(2), false, false))).isTrue();
    }

    //foreldrepenger er endret og uttak har ikke overlappende perioder
    @Test
    public void skal_ikke_opprette_berørt_behandling_dersom_revudering_er_endring_i_foreldrepenger_med_endring_i_uttak_og_ikke_overlappende_perioder() {
        var behandlingsresultat = lagBehandlingsresultat(BehandlingResultatType.FORELDREPENGER_ENDRET, KonsekvensForYtelsen.ENDRING_I_UTTAK, false);

        var behandling = behandlingsresultat.get().getBehandling();
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag());
        when(stønadskontoSaldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)).thenReturn(false);
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);

        assertThat(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(behandlingsresultat,
            lagUttakResultatPerioder(LocalDate.now().minusDays(12), LocalDate.now().plusDays(2), true, false),
            lagUttakResultatPerioder(LocalDate.now().plusDays(3), LocalDate.now().plusDays(5), false, false))).isFalse();
    }

    //foreldrepenger er endret og uttak har overlappende avslått periode
    @Test
    public void skal_ikke_opprette_berørt_behandling_dersom_revudering_er_endring_i_foreldrepenger_med_endring_i_uttak_og_overlappende_avslått_perioder() {
        var behandlingsresultat = lagBehandlingsresultat(BehandlingResultatType.FORELDREPENGER_ENDRET, KonsekvensForYtelsen.ENDRING_I_UTTAK, false);

        var behandling = behandlingsresultat.get().getBehandling();
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag());
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);
        when(stønadskontoSaldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)).thenReturn(false);

        assertThat(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(behandlingsresultat,
            lagUttakResultatPerioder(LocalDate.now().minusDays(12), LocalDate.now().plusDays(2), false, true),
            lagUttakResultatPerioder(LocalDate.now().plusDays(3), LocalDate.now().plusDays(5), false, false))).isFalse();
    }

    //Scenarie 4b - foreldrepenger er innvilget og uttak har negativ saldo
    @Test
    public void skal_opprette_berørt_behandling_dersom_revudering_er_innvilget_med_endring_i_uttak_negativ_saldo() {
        var behandlingsresultat = lagBehandlingsresultat(BehandlingResultatType.INNVILGET, KonsekvensForYtelsen.ENDRING_I_UTTAK, false);
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandlingsresultat.get().getBehandling()), null, new ForeldrepengerGrunnlag());
        var behandling = behandlingsresultat.get().getBehandling();
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);

        when(stønadskontoSaldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)).thenReturn(true);
        assertThat(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(behandlingsresultat, Optional.empty(), Optional.empty())).isTrue();
    }

    @Test
    public void skal_ikke_opprette_berørt_behandling_dersom_revudering_er_berørt_behandling() {
        revurdering.getBehandlingÅrsaker().add(BehandlingÅrsak.builder(BehandlingÅrsakType.BERØRT_BEHANDLING).buildFor(revurdering).get(0));
        var behandlingsresultat = lagBehandlingsresultat(BehandlingResultatType.FORELDREPENGER_ENDRET, KonsekvensForYtelsen.ENDRING_I_UTTAK, false);

        var behandling = behandlingsresultat.get().getBehandling();
        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, new ForeldrepengerGrunnlag());
        when(stønadskontoSaldoTjeneste.erNegativSaldoPåNoenKonto(uttakInput)).thenReturn(false);
        when(uttakInputTjeneste.lagInput(behandling.getId())).thenReturn(uttakInput);

        assertThat(berørtBehandlingTjeneste.skalBerørtBehandlingOpprettes(behandlingsresultat,
            lagUttakResultatPerioder(LocalDate.now().minusDays(12), LocalDate.now().plusDays(2), true, false),
            lagUttakResultatPerioder(LocalDate.now().plusDays(3), LocalDate.now().plusDays(5), false, false))).isFalse();
    }

    private Optional<Behandlingsresultat> lagBehandlingsresultat(BehandlingResultatType behandlingResultatType, KonsekvensForYtelsen konsekvensForYtelsen,
                                                                 boolean harEndretStønadskonto) {
        return Optional.of(Behandlingsresultat.builder()
            .medBehandlingResultatType(behandlingResultatType)
            .leggTilKonsekvensForYtelsen(konsekvensForYtelsen)
            .medEndretStønadskonto(harEndretStønadskonto)
            .buildFor(revurdering));
    }

    private Optional<ForeldrepengerUttak> lagUttakResultatPerioder(LocalDate fom, LocalDate tom, boolean medPeriodeForan, boolean medAvslåttPeriode) {
        List<ForeldrepengerUttakPeriode> perioder = new ArrayList<>();
        if (medPeriodeForan) {
            var periode = new ForeldrepengerUttakPeriode.Builder()
                .medTidsperiode(fom.minusWeeks(3), fom.minusDays(1))
                .medResultatType(PeriodeResultatType.INNVILGET)
                .build();
            perioder.add(periode);
        }
        var periode = new ForeldrepengerUttakPeriode.Builder()
            .medTidsperiode(fom, tom)
            .medResultatType(PeriodeResultatType.INNVILGET)
            .build();
        perioder.add(periode);
        if (medAvslåttPeriode) {
            var avslåttPeriode = new ForeldrepengerUttakPeriode.Builder()
                .medTidsperiode(tom.plusDays(1), tom.plusDays(7))
                .medResultatType(PeriodeResultatType.AVSLÅTT)
                .build();
            perioder.add(avslåttPeriode);
        }
        return Optional.of(new ForeldrepengerUttak(perioder, List.of()));
    }
}
