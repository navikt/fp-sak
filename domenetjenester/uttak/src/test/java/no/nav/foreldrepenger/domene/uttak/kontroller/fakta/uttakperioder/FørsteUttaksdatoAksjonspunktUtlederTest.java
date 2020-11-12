package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryProviderForTest;

public class FørsteUttaksdatoAksjonspunktUtlederTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryProviderForTest();

    private final FørsteUttaksdatoAksjonspunktUtleder avklarFørsteUttaksdato = new FørsteUttaksdatoAksjonspunktUtleder(
        repositoryProvider);
    private final UttakRevurderingTestUtil testUtil = new UttakRevurderingTestUtil(repositoryProvider,
        mock(InntektArbeidYtelseTjeneste.class));

    @Test
    public void aksjonspunkt_dersom_endringsdato_diff_fra_original_behandling_vedtak() {
        Behandling revurdering = testUtil.opprettRevurdering(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);

        LocalDate manuellSattDato = getFørsteUttakDatoIGjeldendeBehandling(revurdering).plusDays(3);
        Long revurderingBehandlingId = revurdering.getId();
        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            manuellSattDato).medOpprinneligEndringsdato(manuellSattDato).build();
        repositoryProvider.getYtelsesFordelingRepository().lagre(revurderingBehandlingId, avklarteUttakDatoer);
        OppgittPeriodeEntitet periode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(manuellSattDato.minusDays(1), manuellSattDato.plusWeeks(7))
            .build();
        repositoryProvider.getYtelsesFordelingRepository()
            .lagre(revurderingBehandlingId, new OppgittFordelingEntitet(Collections.singletonList(periode), true));

        // Act
        var input = lagInput(revurdering).medBehandlingÅrsaker(Set.of(BehandlingÅrsakType.RE_KLAGE_UTEN_END_INNTEKT))
            .medBehandlingManueltOpprettet(false);
        var aksjonspunkter = avklarFørsteUttaksdato.utledAksjonspunkterFor(input);

        // Assert
        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO);

    }

    @Test
    public void aksjonspunkt_dersom_endringsdato_diff_fra_søknaden_for_førstgangs() {
        LocalDate dato = LocalDate.of(2019, 3, 22);
        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            dato.plusDays(1)).build();
        ScenarioMorSøkerForeldrepenger scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .medPeriode(dato, dato.plusWeeks(12))
                .build()), true));

        Behandling behandling = scenarioMorSøkerForeldrepenger.lagre(repositoryProvider);

        // Act
        var aksjonspunkter = avklarFørsteUttaksdato.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO);

    }

    private UttakInput lagInput(Behandling behandling) {
        return new UttakInput(BehandlingReferanse.fra(behandling), null, null);
    }

    @Test
    public void ikke_aksjonspunkt_dersom_endringsdato_diff_fra_søknaden_for_førstgangs() {
        LocalDate dato = LocalDate.of(2019, 3, 22);
        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            dato).medOpprinneligEndringsdato(dato).build();
        ScenarioMorSøkerForeldrepenger scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .medPeriode(dato, dato.plusWeeks(12))
                .build()), true));

        Behandling behandling = scenarioMorSøkerForeldrepenger.lagre(repositoryProvider);

        // Act
        var aksjonspunkter = avklarFørsteUttaksdato.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunkter).doesNotContain(AksjonspunktDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO);

    }

    @Test
    public void aksjonspunkt_dersom_første_uttaksdato_diff_fra_original_første_dato_søknad() {
        LocalDate dato = LocalDate.of(2019, 3, 21);
        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            dato.plusDays(1)).medOpprinneligEndringsdato(dato.plusDays(2)).build();
        ScenarioMorSøkerForeldrepenger scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .medPeriode(avklarteUttakDatoer.getGjeldendeEndringsdato(), dato.plusWeeks(12))
                .build()), true));

        Behandling behandling = scenarioMorSøkerForeldrepenger.lagre(repositoryProvider);

        // Act
        var aksjonspunkter = avklarFørsteUttaksdato.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO);
    }

    @Test
    public void ikke_aksjonspunkt_selvom_diff_mellom_første_uttaksdato_og_original_første_dato_søknad_pga_helg_ignoreres() {
        LocalDate dato = LocalDate.of(2019, 3, 22);
        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            dato.plusDays(1)).medOpprinneligEndringsdato(dato.plusDays(2)).build();
        ScenarioMorSøkerForeldrepenger scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .medPeriode(avklarteUttakDatoer.getGjeldendeEndringsdato(), dato.plusWeeks(12))
                .build()), true));

        Behandling behandling = scenarioMorSøkerForeldrepenger.lagre(repositoryProvider);

        // Act
        var aksjonspunkter = avklarFørsteUttaksdato.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunkter).doesNotContain(AksjonspunktDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO);
    }

    @Test
    public void ikke_aksjonspunkt_dersom_første_uttaksdato_er_lik_første_dato_søknad_men_annen_endringsdato() {
        LocalDate dato = LocalDate.of(2019, 3, 22);
        AvklarteUttakDatoerEntitet avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            dato.plusDays(1)).medOpprinneligEndringsdato(dato.plusDays(2)).build();
        ScenarioMorSøkerForeldrepenger scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .medPeriode(avklarteUttakDatoer.getFørsteUttaksdato(), dato.plusWeeks(12))
                .build()), true));

        Behandling behandling = scenarioMorSøkerForeldrepenger.lagre(repositoryProvider);

        // Act
        var aksjonspunkter = avklarFørsteUttaksdato.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunkter).isEmpty();

    }

    private LocalDate getFørsteUttakDatoIGjeldendeBehandling(Behandling behandling) {
        Optional<Long> originalBehandlingId = behandling.getOriginalBehandlingId();
        if (originalBehandlingId.isPresent()) {
            Optional<UttakResultatEntitet> gjeldendeUttakResultat = repositoryProvider.getFpUttakRepository()
                .hentUttakResultatHvisEksisterer(originalBehandlingId.get());
            if (gjeldendeUttakResultat.isPresent()) {
                Optional<UttakResultatPeriodeEntitet> førsteUttaksdatoGjeldendeVedtak = gjeldendeUttakResultat.get()
                    .getGjeldendePerioder()
                    .getPerioder()
                    .stream()
                    .min(Comparator.comparing(UttakResultatPeriodeEntitet::getFom));
                return førsteUttaksdatoGjeldendeVedtak.get().getFom();
            }
        }
        return LocalDate.of(2019, 3, 22);
    }

}
