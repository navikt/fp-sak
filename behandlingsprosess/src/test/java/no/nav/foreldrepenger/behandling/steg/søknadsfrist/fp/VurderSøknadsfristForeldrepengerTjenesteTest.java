package no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class VurderSøknadsfristForeldrepengerTjenesteTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private SøknadsfristTjeneste tjeneste;

    @Inject
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    @Inject
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;


    @Test
    public void skal_oppdatere_behandlingsresultet_med_uttaksperiodegrense() {
        // Arrange
        LocalDate nyMottattDato = LocalDate.of(2018,1,15);
        LocalDate førsteLovligeUttaksdag = LocalDate.of(2017,10,1);
        VurderSøknadsfristAksjonspunktDto adapter = new VurderSøknadsfristAksjonspunktDto(nyMottattDato, "Begrunnelse");
        var behandling = byggBehandlingMedYf();

        // Act
        tjeneste.lagreVurderSøknadsfristResultat(behandling, adapter);

        // Assert
        var uttaksperiodegrense =  uttaksperiodegrenseRepository.hent(behandling.getId());
        assertThat(uttaksperiodegrense.getErAktivt()).isTrue();
        assertThat(uttaksperiodegrense.getMottattDato()).isEqualTo(nyMottattDato);
        assertThat(uttaksperiodegrense.getFørsteLovligeUttaksdag()).isEqualTo(førsteLovligeUttaksdag);
    }

    @Test
    public void skal_oppdatere_behandlingsresultat_med_eksisterende_uttaksperiodegrense() {
        // Arrange
        LocalDate gammelMottatDato = LocalDate.of(2018,3,15);
        String begrunnelse = "Begrunnelse";
        VurderSøknadsfristAksjonspunktDto gammelSøknadsfristGrense = new VurderSøknadsfristAksjonspunktDto(gammelMottatDato, begrunnelse);
        var behandling = byggBehandlingMedYf();
        tjeneste.lagreVurderSøknadsfristResultat(behandling, gammelSøknadsfristGrense);

        LocalDate nyMottattDato = LocalDate.of(2018,2,28);
        LocalDate førsteLovligeUttaksdag = LocalDate.of(2017,11,1);
        VurderSøknadsfristAksjonspunktDto nySøknadsfristGrense = new VurderSøknadsfristAksjonspunktDto(nyMottattDato, begrunnelse);

        // Act
        tjeneste.lagreVurderSøknadsfristResultat(behandling, nySøknadsfristGrense);

        // Assert
        Uttaksperiodegrense uttaksperiodegrense = uttaksperiodegrenseRepository.hent(behandling.getId());
        assertThat(uttaksperiodegrense.getErAktivt()).isTrue();
        assertThat(uttaksperiodegrense.getMottattDato()).isEqualTo(nyMottattDato);
        assertThat(uttaksperiodegrense.getFørsteLovligeUttaksdag()).isEqualTo(førsteLovligeUttaksdag);
    }

    @Test
    public void finnerSøknadsfristForPeriodeStartDato() {
        LocalDate periodeStart = LocalDate.of(2018, 1, 31);
        LocalDate forventetSøknadsfrist = LocalDate.of(2018, 04, 30);

        LocalDate søknadsfrist = tjeneste.finnSøknadsfristForPeriodeMedStart(periodeStart);
        assertThat(søknadsfrist).isEqualTo(forventetSøknadsfrist);

        periodeStart = LocalDate.of(2018, 1, 31);
        søknadsfrist = tjeneste.finnSøknadsfristForPeriodeMedStart(periodeStart);
        assertThat(søknadsfrist).isEqualTo(forventetSøknadsfrist);
    }

    @Test
    public void skal_oppdatere_mottatt_dato_i_oppgitte_perioder() {
        LocalDate nyMottattDato = LocalDate.of(2018,1,15);
        VurderSøknadsfristAksjonspunktDto adapter = new VurderSøknadsfristAksjonspunktDto(nyMottattDato, "Begrunnelse");

        var behandling = byggBehandlingMedYf();
        tjeneste.lagreVurderSøknadsfristResultat(behandling, adapter);

        var ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var justertFordelingSortert = ytelseFordelingAggregat.getJustertFordeling().orElseThrow().getOppgittePerioder().stream()
            .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
            .collect(Collectors.toList());
        //Skal ikke oppdatere vedtaksperioder
        assertThat(justertFordelingSortert.get(0).getMottattDato()).isNotEqualTo(nyMottattDato);
        assertThat(justertFordelingSortert.get(1).getMottattDato()).isEqualTo(nyMottattDato);
    }

    private Behandling byggBehandlingMedYf() {
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
        return ScenarioMorSøkerForeldrepenger.forFødsel()
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(mødrekvote.getFom()).build())
            .medJustertFordeling(new OppgittFordelingEntitet(List.of(mødrekvote, fellesperiode), true))
            .lagre(new BehandlingRepositoryProvider(repoRule.getEntityManager()));
    }
}
