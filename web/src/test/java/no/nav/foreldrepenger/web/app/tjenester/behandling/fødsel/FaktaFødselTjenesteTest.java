package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.Kilde;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.UidentifisertBarnDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt.OverstyringFaktaOmFødselDto;

public class FaktaFødselTjenesteTest extends EntityManagerAwareTest {
    private static final LocalDate FØDSELSDATO = LocalDate.now();
    private static final LocalDate TERMINDATO = LocalDate.now();
    private BehandlingRepositoryProvider repositoryProvider;
    private FaktaFødselTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var fhTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        tjeneste = new FaktaFødselTjeneste(fhTjeneste);
    }

    @Test
    void skal_lagre_overstyrt_fødselsdato_og_termindato_når_register_har_en_annen_eksisterende_fødselsdato() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var barnDtoListe = List.of(new UidentifisertBarnDto(FØDSELSDATO, null));
        var behandling = byggBehandlingBekreftetFødsel(scenario, barnDtoListe);
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("begrunnelse", TERMINDATO, List.of(
            new UidentifisertBarnDto(FØDSELSDATO, null), new UidentifisertBarnDto(FØDSELSDATO.plusDays(1), null)
        ));

        // Act
        tjeneste.overstyrFaktaOmFødsel(behandling.getId(), overstyringFaktaOmFødselDto);

        // Assert
        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        var gjeldendeHendelse = fhFraRepo.getGjeldendeVersjon();
        assertThat(fhFraRepo.getGjeldendeAntallBarn()).isEqualTo(2);
        assertThat(gjeldendeHendelse.getTermindato()).hasValue(TERMINDATO);
        assertThat(gjeldendeHendelse.getBarna())
            .extracting(barn -> barn.getFødselsdato())
            .containsExactlyInAnyOrder(FØDSELSDATO, FØDSELSDATO.plusDays(1));
    }

    @Test
    void skal_lagre_termindato_i_gjeldende_versjon_når_det_ikke_finnes_noe_i_register() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = byggBehandlingTermin(scenario, TERMINDATO);
        var behandlingId = behandling.getId();
        var endretTermindato = TERMINDATO.minusWeeks(1);
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("begrunnelse", endretTermindato, Collections.EMPTY_LIST);

        // Act
        tjeneste.overstyrFaktaOmFødsel(behandlingId, overstyringFaktaOmFødselDto);

        // Assert
        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandlingId);
        var gjeldendeHendelse = fhFraRepo.getGjeldendeVersjon();
        assertThat(gjeldendeHendelse.getTermindato()).hasValue(endretTermindato);
    }

    @Test
    void skal_kunne_lagre_overstyrt_dødfødsel_hvis_ikke_finnes_i_register_fra_før() {
        // Arrange
        var dødsdato = FØDSELSDATO.plusDays(1);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var barnDtoListe = List.of(new UidentifisertBarnDto(FØDSELSDATO, null));
        var behandling = byggBehandlingBekreftetFødsel(scenario, barnDtoListe);
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("begrunnelse", null, List.of(
            new UidentifisertBarnDto(FØDSELSDATO, dødsdato)
        ));

        // Act
        tjeneste.overstyrFaktaOmFødsel(behandling.getId(), overstyringFaktaOmFødselDto);

        // Assert
        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(fhFraRepo.getOverstyrtVersjon()).hasValue(fhFraRepo.getGjeldendeVersjon());
        var gjeldendeVersjon = fhFraRepo.getGjeldendeVersjon();
        assertThat(gjeldendeVersjon.getBarna()).hasSize(1);
        assertThat(gjeldendeVersjon.getBarna().getFirst().getDødsdato()).hasValue(dødsdato);
    }

    @Test
    void skal_kunne_lagre_overstyrt_dødfødsel_hvis_to_barn_fødselsdatoer_finnes_i_register_fra_før() {
        // Arrange
        var dødsdato = FØDSELSDATO.plusDays(1);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var barnDtoListe = List.of(new UidentifisertBarnDto(FØDSELSDATO, null), new UidentifisertBarnDto(FØDSELSDATO, null));
        var behandling = byggBehandlingBekreftetFødsel(scenario, barnDtoListe);
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("begrunnelse", null, List.of(
            new UidentifisertBarnDto(FØDSELSDATO, dødsdato)
        ));

        // Act
        tjeneste.overstyrFaktaOmFødsel(behandling.getId(), overstyringFaktaOmFødselDto);

        // Assert
        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(fhFraRepo.getOverstyrtVersjon()).hasValue(fhFraRepo.getGjeldendeVersjon());
        var gjeldendeVersjon = fhFraRepo.getGjeldendeVersjon();
        assertThat(gjeldendeVersjon.getBarna()).hasSize(1);
        assertThat(gjeldendeVersjon.getBarna().getFirst().getDødsdato()).hasValue(dødsdato);
    }

    @Test
    void skal_kunne_fjerne_et_av_to_barn_fra_gjeldende_ved_feiltakelse_lagt_inn_opprinnelig() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        var søknadHendelse = scenario.medSøknadHendelse();
        søknadHendelse.medAntallBarn(1)
            .medFødselsDato(FØDSELSDATO,1)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(TERMINDATO)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(TERMINDATO.minusMonths(1)));

        // Tvillinger i overstyrt hendelse
        var overstyrtHendelse = scenario.medOverstyrtHendelse();
        overstyrtHendelse.medAntallBarn(2).leggTilBarn(FØDSELSDATO).leggTilBarn(FØDSELSDATO).build();

        var behandling = scenario.lagre(repositoryProvider);

        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("begrunnelse", null, List.of(
            new UidentifisertBarnDto(FØDSELSDATO, null)));

        // Act
        var fhFraRepoFoer = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(fhFraRepoFoer.getGjeldendeVersjon().getBarna()).hasSize(2);

        tjeneste.overstyrFaktaOmFødsel(behandling.getId(), overstyringFaktaOmFødselDto);

        // Assert
        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        var gjeldende = fhFraRepo.getGjeldendeVersjon();
        assertThat(gjeldende.getBarna()).hasSize(1);
    }

    @Test
    void skal_hente_fakta_om_fødsel_med_register_barn_og_overstyrbar_termindato() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        var søknadHendelse = scenario.medSøknadHendelse();
        søknadHendelse.medAntallBarn(1)
            .medFødselsDato(FØDSELSDATO)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
            .getTerminbekreftelseBuilder()
            .medTermindato(TERMINDATO)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(TERMINDATO.minusMonths(1)));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());

        var bekreftetHendelse = scenario.medBekreftetHendelse();
        bekreftetHendelse.medFødselsDato(FØDSELSDATO).medAntallBarn(1);

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());
        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.termindato().termindato()).isEqualTo(TERMINDATO); // TODO Thao: Her er termindato null. Hør med Ragnhild
        assertThat(gjeldende.termindato().kanOverstyres()).isTrue();
        assertThat(gjeldende.barn()).hasSize(1);
        assertThat(gjeldende.barn().getFirst().barn().getFodselsdato()).isEqualTo(FØDSELSDATO);
        assertThat(gjeldende.barn().getFirst().kilde()).isEqualTo(Kilde.FOLKEREGISTER);
        assertThat(gjeldende.barn().getFirst().kanOverstyres()).isFalse();
    }

    private Behandling byggBehandlingBekreftetFødsel(AbstractTestScenario<?> scenario, List<UidentifisertBarnDto> barnListe) {
        var hendelse = scenario.medBekreftetHendelse();
        hendelse.medAntallBarn(barnListe.size());
        barnListe.forEach(barn -> hendelse.leggTilBarn(barn.getFodselsdato(), barn.getDodsdato().orElse(null)));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());
        return scenario.lagre(repositoryProvider);
    }

    private Behandling byggBehandlingTermin(AbstractTestScenario<?> scenario, LocalDate termindato) {
        scenario.medSøknadHendelse()
            .medAntallBarn(1)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(termindato.minusMonths(1)));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());

        return scenario.lagre(repositoryProvider);
    }
}
