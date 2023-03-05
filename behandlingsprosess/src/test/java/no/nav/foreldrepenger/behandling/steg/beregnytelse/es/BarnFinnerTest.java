package no.nav.foreldrepenger.behandling.steg.beregnytelse.es;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.vedtak.exception.FunksjonellException;

class BarnFinnerTest {

    private final int maksStønadsalder = 15;

    @Test
    void skal_finne_antall_barn_basert_på_fødsel() {
        // Arrange
        var antallBarnPåFødsel = 2;
        var scenario = byggBehandlingsgrunnlagForFødsel(antallBarnPåFødsel);
        var behandling = scenario.lagMocked();

        // Act
        var barnFinner = new BarnFinner(scenario.mockBehandlingRepositoryProvider().getFamilieHendelseRepository());
        var funnetAntallBarn = barnFinner.finnAntallBarn(behandling.getId(), maksStønadsalder);

        // Assert
        assertThat(antallBarnPåFødsel).isEqualTo(funnetAntallBarn);
    }

    @Test
    void skal_finne_antall_barn_basert_på_termin() {
        // Arrange
        var antallBarnPåTerminBekreftelse = 2;
        var scenario = byggBehandlingsgrunnlagForTermin(antallBarnPåTerminBekreftelse);
        var behandling = scenario.lagMocked();

        // Act
        var barnFinner = new BarnFinner(scenario.mockBehandlingRepositoryProvider().getFamilieHendelseRepository());
        var funnetAntallBarn = barnFinner.finnAntallBarn(behandling.getId(), maksStønadsalder);

        // Assert
        assertThat(antallBarnPåTerminBekreftelse).isEqualTo(funnetAntallBarn);
    }

    @Test
    void skal_finne_antall_barn_basert_på_adopsjon() {
        // Arrange
        var overtakelseDato = LocalDate.now();
        // Skal kun gis stønad for barn < 15 år, dvs 2 barn her
        var fødselsdato14År363Dager = overtakelseDato.minusYears(15).plusDays(2);
        var fødselsdato14År364Dager = overtakelseDato.minusYears(15).plusDays(1);
        var fødselsdato15År = overtakelseDato.minusYears(15);

        var scenario = byggBehandlingsgrunnlagForAdopsjon(
                asList(fødselsdato14År363Dager, fødselsdato14År364Dager, fødselsdato15År), overtakelseDato);
        var behandling = scenario.lagMocked();

        // Act
        var barnFinner = new BarnFinner(scenario.mockBehandlingRepositoryProvider().getFamilieHendelseRepository());
        var funnetAntallBarn = barnFinner.finnAntallBarn(behandling.getId(), maksStønadsalder);

        // Assert
        assertThat(funnetAntallBarn).isEqualTo(2);
    }

    @Test
    void skal_finne_antall_barn_basert_på_omsorgsovertakelse() {
        // Arrange
        var scenario = byggBehandlingsgrunnlagForOmsorgsovertakelse(2);
        var behandling = scenario.lagMocked();

        // Act
        var barnFinner = new BarnFinner(scenario.mockBehandlingRepositoryProvider().getFamilieHendelseRepository());
        var funnetAntallBarn = barnFinner.finnAntallBarn(behandling.getId(), maksStønadsalder);

        // Assert
        assertThat(2).isEqualTo(funnetAntallBarn);
    }

    @Test
    void skal_kaste_feil_dersom_antall_barn_ikke_kan_finnes_i_grunnlag() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = scenario.lagMocked();

        // Act
        var barnFinner = new BarnFinner(scenario.mockBehandlingRepositoryProvider().getFamilieHendelseRepository());
        assertThrows(FunksjonellException.class, () -> barnFinner.finnAntallBarn(behandling.getId(), maksStønadsalder));
    }

    private ScenarioMorSøkerEngangsstønad byggBehandlingsgrunnlagForFødsel(int antallBarn) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final var hendelseBuilder = scenario.medBekreftetHendelse().medAntallBarn(antallBarn);
        IntStream.range(0, antallBarn).forEach(it -> hendelseBuilder.leggTilBarn(LocalDate.now()));
        scenario.medBekreftetHendelse(hendelseBuilder);
        return scenario;
    }

    private ScenarioMorSøkerEngangsstønad byggBehandlingsgrunnlagForTermin(int antallBarn) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        final var familieHendelseBuilder = scenario.medBekreftetHendelse()
                .medTerminbekreftelse(scenario.medBekreftetHendelse().getTerminbekreftelseBuilder()
                        .medTermindato(LocalDate.now())
                        .medNavnPå("LEGEN MIN")
                        .medUtstedtDato(LocalDate.now()))
                .medAntallBarn(antallBarn);
        scenario.medBekreftetHendelse(familieHendelseBuilder);

        return scenario;
    }

    private ScenarioFarSøkerEngangsstønad byggBehandlingsgrunnlagForOmsorgsovertakelse(int antallBarn) {
        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();

        for (var nr = 1; nr <= antallBarn; nr++) {
            scenario.medSøknadHendelse().leggTilBarn(LocalDate.now());
        }

        var søker = scenario.opprettBuilderForRegisteropplysninger()
                .medPersonas()
                .kvinne(scenario.getDefaultBrukerAktørId(), SivilstandType.GIFT)
                .statsborgerskap(Landkoder.NOR)
                .build();
        scenario.medRegisterOpplysninger(søker);

        scenario.medSøknad().medFarSøkerType(FarSøkerType.OVERTATT_OMSORG);
        final var adopsjonBuilder = scenario.medSøknadHendelse().getAdopsjonBuilder();
        scenario.medSøknadHendelse().erOmsorgovertagelse().medAdopsjon(adopsjonBuilder.medOmsorgsovertakelseDato(LocalDate.now()));
        return scenario;
    }

    private ScenarioMorSøkerEngangsstønad byggBehandlingsgrunnlagForAdopsjon(List<LocalDate> adopsjonsdatoer, LocalDate overtakelseDato) {
        var scenario = ScenarioMorSøkerEngangsstønad.forAdopsjon();
        final var hendelseBuilder = scenario.medBekreftetHendelse().medAntallBarn(adopsjonsdatoer.size());
        hendelseBuilder.medAdopsjon(scenario.medBekreftetHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(overtakelseDato));

        for (var nr = 1; nr <= adopsjonsdatoer.size(); nr++) {
            hendelseBuilder.leggTilBarn(adopsjonsdatoer.get(nr - 1));
        }
        scenario.medBekreftetHendelse(hendelseBuilder);
        return scenario;
    }
}
