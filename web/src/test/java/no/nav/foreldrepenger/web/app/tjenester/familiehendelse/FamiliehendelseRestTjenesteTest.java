package no.nav.foreldrepenger.web.app.tjenester.familiehendelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.OmsorgsovertakelseVilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;

@CdiDbAwareTest
class FamiliehendelseRestTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private FamilieHendelseRepository familieHendelseRepository;

    @Test
    void skal_returnere_familiehendelse_for_fødsel_og_termin() {
        var termindato = LocalDate.of(2023, 6, 15);
        var fødselsdato = LocalDate.of(2023, 6, 10);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato);
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder().medTermindato(termindato));

        var behandling = scenario.lagre(repositoryProvider);
        var grunnlag = familieHendelseRepository.hentAggregat(behandling.getId());

        var result = FamiliehendelseRestTjeneste.mapTilFamilieHendelseDto(grunnlag);

        assertThat(result).isNotNull();
        assertThat(result.fødselTermin()).isNotNull();
        assertThat(result.adopsjon()).isNull();

        var fødselTermin = result.fødselTermin();
        assertThat(fødselTermin.termindato()).isEqualTo(termindato);
        assertThat(fødselTermin.fødselsdato()).isEqualTo(fødselsdato);
    }

    @Test
    void skal_returnere_familiehendelse_for_fødsel_uten_termin() {
        var fødselsdato = LocalDate.of(2023, 6, 10);

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato);

        var behandling = scenario.lagre(repositoryProvider);
        var grunnlag = familieHendelseRepository.hentAggregat(behandling.getId());

        var result = FamiliehendelseRestTjeneste.mapTilFamilieHendelseDto(grunnlag);

        assertThat(result).isNotNull();
        assertThat(result.fødselTermin()).isNotNull();
        assertThat(result.adopsjon()).isNull();

        var fødselTermin = result.fødselTermin();
        assertThat(fødselTermin.termindato()).isNull();
        assertThat(fødselTermin.fødselsdato()).isEqualTo(fødselsdato);
    }

    @Test
    void skal_returnere_familiehendelse_for_adopsjon() {
        var omsorgsovertakelseDato = LocalDate.of(2023, 5, 1);
        var foreldreansvarDato = LocalDate.of(2023, 5, 15);
        var ankomstNorgeDato = LocalDate.of(2023, 4, 20);
        var fødselsdatoBarn1 = LocalDate.of(2021, 3, 10);
        var fødselsdatoBarn2 = LocalDate.of(2022, 8, 5);

        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse().medAdopsjon(
            scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(omsorgsovertakelseDato)
                .medForeldreansvarDato(foreldreansvarDato)
                .medAnkomstDato(ankomstNorgeDato)
                .medOmsorgovertalseVilkårType(OmsorgsovertakelseVilkårType.ES_OMSORGSVILKÅRET)
                .medErEktefellesBarn(true)
                .medAdoptererAlene(false)
        );
        scenario.medSøknadHendelse().leggTilBarn(fødselsdatoBarn1);
        scenario.medSøknadHendelse().leggTilBarn(fødselsdatoBarn2);

        var behandling = scenario.lagre(repositoryProvider);
        var grunnlag = familieHendelseRepository.hentAggregat(behandling.getId());

        var result = FamiliehendelseRestTjeneste.mapTilFamilieHendelseDto(grunnlag);

        assertThat(result).isNotNull();
        assertThat(result.fødselTermin()).isNull();
        assertThat(result.adopsjon()).isNotNull();

        var adopsjonDto = result.adopsjon();
        assertThat(adopsjonDto.antallBarn()).isEqualTo(2);
        assertThat(adopsjonDto.omsorgsovertakelseDato()).isEqualTo(omsorgsovertakelseDato);
        assertThat(adopsjonDto.omsorgsovertakelseVilkårType()).isEqualTo(OmsorgsovertakelseVilkårType.ES_OMSORGSVILKÅRET);
        assertThat(adopsjonDto.ektefellesBarn()).isTrue();
        assertThat(adopsjonDto.mannAdoptererAlene()).isFalse();

        assertThat(adopsjonDto.fødselsdatoer()).hasSize(2);
        assertThat(adopsjonDto.fødselsdatoer()).containsEntry(0, fødselsdatoBarn1);
        assertThat(adopsjonDto.fødselsdatoer()).containsEntry(1, fødselsdatoBarn2);
    }

    @Test
    void skal_returnere_familiehendelse_for_adopsjon_med_minimale_data() {
        var omsorgsovertakelseDato = LocalDate.of(2023, 5, 1);
        var fødselsdatoBarn = omsorgsovertakelseDato.plusWeeks(2);

        var scenario = ScenarioMorSøkerForeldrepenger.forAdopsjon();
        scenario.medSøknadHendelse().medAdopsjon(
            scenario.medSøknadHendelse().getAdopsjonBuilder()
                .medOmsorgsovertakelseDato(omsorgsovertakelseDato)
        );
        scenario.medSøknadHendelse().leggTilBarn(fødselsdatoBarn);

        var behandling = scenario.lagre(repositoryProvider);
        var grunnlag = familieHendelseRepository.hentAggregat(behandling.getId());

        var result = FamiliehendelseRestTjeneste.mapTilFamilieHendelseDto(grunnlag);

        assertThat(result).isNotNull();
        assertThat(result.fødselTermin()).isNull();
        assertThat(result.adopsjon()).isNotNull();

        var adopsjonDto = result.adopsjon();
        assertThat(adopsjonDto.antallBarn()).isEqualTo(1);
        assertThat(adopsjonDto.omsorgsovertakelseDato()).isEqualTo(omsorgsovertakelseDato);
        assertThat(adopsjonDto.omsorgsovertakelseVilkårType()).isNotNull();
        assertThat(adopsjonDto.ektefellesBarn()).isFalse();
        assertThat(adopsjonDto.mannAdoptererAlene()).isFalse();
    }
}
