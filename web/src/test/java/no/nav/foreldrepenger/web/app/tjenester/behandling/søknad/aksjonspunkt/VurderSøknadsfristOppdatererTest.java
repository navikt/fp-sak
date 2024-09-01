package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Søknadsfrister;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
class VurderSøknadsfristOppdatererTest {

    @Inject
    private @Any Instance<VurderSøknadsfristOppdaterer> tjeneste;

    @Inject
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    @Inject
    private SøknadRepository søknadRepository;

    @Test
    void skal_oppdatere_behandlingsresultet_med_uttaksperiodegrense(EntityManager em) {
        // Arrange
        var nyMottattDato = LocalDate.of(2018, 1, 15);
        var førsteLovligeUttaksdag = LocalDate.of(2017, 10, 1);
        var dto = new VurderSøknadsfristDto("Begrunnelse", true);
        dto.setAnsesMottattDato(nyMottattDato);
        var behandling = byggBehandlingMedYf(em);

        // Act
        tjeneste.get().oppdater(dto, aksjonspunktParam(behandling, dto));

        // Assert
        var uttaksperiodegrense = uttaksperiodegrenseRepository.hent(behandling.getId());
        assertThat(uttaksperiodegrense.getErAktivt()).isTrue();
        assertThat(uttaksperiodegrense.getMottattDato()).isEqualTo(nyMottattDato);
        assertThat(Søknadsfrister.tidligsteDatoDagytelse(uttaksperiodegrense.getMottattDato())).isEqualTo(førsteLovligeUttaksdag);
    }

    private AksjonspunktOppdaterParameter aksjonspunktParam(Behandling behandling, BekreftetAksjonspunktDto dto) {
        var ap = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST);
        return new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto, ap);
    }

    @Test
    void skal_oppdatere_behandlingsresultat_med_eksisterende_uttaksperiodegrense(EntityManager em) {
        // Arrange
        var gammelMottatDato = LocalDate.of(2018, 3, 15);

        var behandling = byggBehandlingMedYf(em);
        var gammelUttaksperiodegrense = new Uttaksperiodegrense(gammelMottatDato);
        uttaksperiodegrenseRepository.lagre(behandling.getId(), gammelUttaksperiodegrense);

        var nyMottattDato = LocalDate.of(2018, 2, 28);
        var førsteLovligeUttaksdag = LocalDate.of(2017, 11, 1);
        var dto = new VurderSøknadsfristDto("Begrunnelse", true);
        dto.setAnsesMottattDato(nyMottattDato);

        // Act
        tjeneste.get().oppdater(dto, aksjonspunktParam(behandling, dto));

        // Assert
        var uttaksperiodegrense = uttaksperiodegrenseRepository.hent(behandling.getId());
        assertThat(uttaksperiodegrense.getErAktivt()).isTrue();
        assertThat(uttaksperiodegrense.getMottattDato()).isEqualTo(nyMottattDato);
        assertThat(Søknadsfrister.tidligsteDatoDagytelse(uttaksperiodegrense.getMottattDato())).isEqualTo(førsteLovligeUttaksdag);
    }

    @Test
    void lagrerMottattDatoFraSøknadVedEndringFraGyldigGrunnTilIkkeGyldigGrunn(EntityManager em) {
        var behandling = byggBehandlingMedYf(em);
        var mottattDatoSøknad = LocalDate.of(2019, 1, 1);
        var søknad = new SøknadEntitet.Builder()
                .medMottattDato(mottattDatoSøknad)
                .medSøknadsdato(mottattDatoSøknad)
                .build();
        søknadRepository.lagreOgFlush(behandling, søknad);

        var dto = new VurderSøknadsfristDto("bg", false);
        dto.setAnsesMottattDato(mottattDatoSøknad.plusYears(1));

        tjeneste.get().oppdater(dto, aksjonspunktParam(behandling, dto));

        var uttaksperiodegrense = uttaksperiodegrenseRepository.hent(behandling.getId());

        assertThat(uttaksperiodegrense.getMottattDato()).isEqualTo(mottattDatoSøknad);
    }

    private Behandling byggBehandlingMedYf(EntityManager em) {
        var mødrekvote = OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.TIDLIGERE_VEDTAK)
                .medMottattDato(LocalDate.of(2020, 1, 1))
                .medPeriodeType(UttakPeriodeType.MØDREKVOTE)
                .medPeriode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 2))
                .build();
        var fellesperiode = OppgittPeriodeBuilder.ny()
                .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
                .medMottattDato(LocalDate.of(2020, 1, 1))
                .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
                .medPeriode(LocalDate.of(2020, 2, 3), LocalDate.of(2020, 3, 3))
                .build();
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(mødrekvote.getFom()).build())
                .medJustertFordeling(new OppgittFordelingEntitet(List.of(mødrekvote, fellesperiode), true))
                .leggTilAksjonspunkt(AksjonspunktDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST, BehandlingStegType.SØKNADSFRIST_FORELDREPENGER)
                .medBehandlingsresultat(new Behandlingsresultat.Builder())
                .lagre(new BehandlingRepositoryProvider(em));
        var søknad = new SøknadEntitet.Builder()
                .medSøknadsdato(mødrekvote.getFom())
                .medMottattDato(mødrekvote.getFom())
                .build();
        søknadRepository.lagreOgFlush(behandling, søknad);
        var uttaksperiodegrense = new Uttaksperiodegrense(mødrekvote.getFom());
        uttaksperiodegrenseRepository.lagre(behandling.getId(), uttaksperiodegrense);
        return behandling;
    }

}
