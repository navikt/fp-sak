package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.UttakRevurderingTestUtil;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

public class FørsteUttaksdatoAksjonspunktUtlederTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    private final FørsteUttaksdatoAksjonspunktUtleder avklarFørsteUttaksdato = new FørsteUttaksdatoAksjonspunktUtleder(
        repositoryProvider);
    private final UttakRevurderingTestUtil testUtil = new UttakRevurderingTestUtil(repositoryProvider,
        mock(InntektArbeidYtelseTjeneste.class));

    @Test
    public void aksjonspunkt_dersom_endringsdato_diff_fra_original_behandling_vedtak() {
        var revurdering = testUtil.opprettRevurdering(BehandlingÅrsakType.RE_HENDELSE_FØDSEL);

        var manuellSattDato = getFørsteUttakDatoIGjeldendeBehandling(revurdering).plusDays(3);
        var revurderingBehandlingId = revurdering.getId();
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            manuellSattDato).medOpprinneligEndringsdato(manuellSattDato).build();
        var periode = OppgittPeriodeBuilder.ny()
            .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
            .medPeriode(manuellSattDato.minusDays(1), manuellSattDato.plusWeeks(7))
            .build();

        var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(revurderingBehandlingId)
            .medAvklarteDatoer(avklarteUttakDatoer)
            .medOppgittFordeling(new OppgittFordelingEntitet(List.of(periode), true));
        ytelsesFordelingRepository.lagre(revurderingBehandlingId, yfBuilder.build());

        // Act
        var input = lagInput(revurdering).medBehandlingÅrsaker(Set.of(BehandlingÅrsakType.RE_KLAGE_UTEN_END_INNTEKT))
            .medBehandlingManueltOpprettet(false);
        var aksjonspunkter = avklarFørsteUttaksdato.utledAksjonspunkterFor(input);

        // Assert
        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO);

    }

    @Test
    public void aksjonspunkt_dersom_endringsdato_diff_fra_søknaden_for_førstgangs() {
        var dato = LocalDate.of(2019, 3, 22);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            dato.plusDays(1)).build();
        var scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .medPeriode(dato, dato.plusWeeks(12))
                .build()), true));

        var behandling = scenarioMorSøkerForeldrepenger.lagre(repositoryProvider);

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
        var dato = LocalDate.of(2019, 3, 22);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            dato).medOpprinneligEndringsdato(dato).build();
        var scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .medPeriode(dato, dato.plusWeeks(12))
                .build()), true));

        var behandling = scenarioMorSøkerForeldrepenger.lagre(repositoryProvider);

        // Act
        var aksjonspunkter = avklarFørsteUttaksdato.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunkter).doesNotContain(AksjonspunktDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO);

    }

    @Test
    public void aksjonspunkt_dersom_første_uttaksdato_diff_fra_original_første_dato_søknad() {
        var dato = LocalDate.of(2019, 3, 21);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            dato.plusDays(1)).medOpprinneligEndringsdato(dato.plusDays(2)).build();
        var scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .medPeriode(avklarteUttakDatoer.getGjeldendeEndringsdato(), dato.plusWeeks(12))
                .build()), true));

        var behandling = scenarioMorSøkerForeldrepenger.lagre(repositoryProvider);

        // Act
        var aksjonspunkter = avklarFørsteUttaksdato.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunkter).contains(AksjonspunktDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO);
    }

    @Test
    public void ikke_aksjonspunkt_selvom_diff_mellom_første_uttaksdato_og_original_første_dato_søknad_pga_helg_ignoreres() {
        var dato = LocalDate.of(2019, 3, 22);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            dato.plusDays(1)).medOpprinneligEndringsdato(dato.plusDays(2)).build();
        var scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .medPeriode(avklarteUttakDatoer.getGjeldendeEndringsdato(), dato.plusWeeks(12))
                .build()), true));

        var behandling = scenarioMorSøkerForeldrepenger.lagre(repositoryProvider);

        // Act
        var aksjonspunkter = avklarFørsteUttaksdato.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunkter).doesNotContain(AksjonspunktDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO);
    }

    @Test
    public void ikke_aksjonspunkt_dersom_første_uttaksdato_er_lik_første_dato_søknad_men_annen_endringsdato() {
        var dato = LocalDate.of(2019, 3, 22);
        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(
            dato.plusDays(1)).medOpprinneligEndringsdato(dato.plusDays(2)).build();
        var scenarioMorSøkerForeldrepenger = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(avklarteUttakDatoer)
            .medFordeling(new OppgittFordelingEntitet(Collections.singletonList(OppgittPeriodeBuilder.ny()
                .medPeriodeType(UttakPeriodeType.FEDREKVOTE)
                .medPeriode(avklarteUttakDatoer.getFørsteUttaksdato(), dato.plusWeeks(12))
                .build()), true));

        var behandling = scenarioMorSøkerForeldrepenger.lagre(repositoryProvider);

        // Act
        var aksjonspunkter = avklarFørsteUttaksdato.utledAksjonspunkterFor(lagInput(behandling));

        // Assert
        assertThat(aksjonspunkter).isEmpty();

    }

    private LocalDate getFørsteUttakDatoIGjeldendeBehandling(Behandling behandling) {
        var originalBehandlingId = behandling.getOriginalBehandlingId();
        if (originalBehandlingId.isPresent()) {
            var gjeldendeUttakResultat = repositoryProvider.getFpUttakRepository()
                .hentUttakResultatHvisEksisterer(originalBehandlingId.get());
            if (gjeldendeUttakResultat.isPresent()) {
                var førsteUttaksdatoGjeldendeVedtak = gjeldendeUttakResultat.get()
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
