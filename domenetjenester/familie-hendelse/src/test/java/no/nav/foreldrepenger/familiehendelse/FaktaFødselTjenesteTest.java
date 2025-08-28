package no.nav.foreldrepenger.familiehendelse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.DokumentertBarnDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OverstyringFaktaOmFødselDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

public class FaktaFødselTjenesteTest extends EntityManagerAwareTest {
    private static final LocalDate FØDSELSDATO = LocalDate.now();
    private static final LocalDate TERMINDATO = LocalDate.now();

    private BehandlingRepositoryProvider repositoryProvider;
    private FaktaFødselTjeneste tjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        tjeneste = new FaktaFødselTjeneste(familieHendelseTjeneste, mock(OpplysningsPeriodeTjeneste.class));
    }

    @Test
    void skal_kaste_exception_når_fødselsdato_ikke_er_innenfor_gyldig_intervall_for_termindato() {
        // Arrange
        var fødselsdato = TERMINDATO.plusWeeks(8);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null));
        var behandling = byggBehandlingBekreftetFødsel(scenario, barnDtoListe);
        var dto = new OverstyringFaktaOmFødselDto("Legger til fødselsdato 8 uker etter termindato.", TERMINDATO,
            barnDtoListe);
        var ref = BehandlingReferanse.fra(behandling);
        var fh = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        // Act & Assert
        var exception = assertThrows(FunksjonellException.class,
            () -> tjeneste.overstyrFaktaOmFødsel(ref, fh, Optional.of(dto.getTermindato()), dto.getBarn()));
        assertThat(exception).extracting("kode", "msg", "løsningsforslag")
            .containsExactly("FP-076346", "For stort avvik termin/fødsel", "Sjekk datoer eller meld sak i Porten");
    }

    @Test
    void skal_kaste_exception_når_dødsdato_er_før_fødselsdato() {
        // Arrange
        var fødselsdato = TERMINDATO.plusDays(1);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato, fødselsdato.minusDays(1)));
        var behandling = byggBehandlingBekreftetFødsel(scenario, barnDtoListe);
        var dto = new OverstyringFaktaOmFødselDto("Legger til dødsdato før fødselsdato", TERMINDATO, barnDtoListe);
        var ref = BehandlingReferanse.fra(behandling);
        var fh = familieHendelseTjeneste.hentAggregat(behandling.getId());

        // Act og Assert
        var exception = assertThrows(FunksjonellException.class,
            () -> tjeneste.overstyrFaktaOmFødsel(ref, fh, Optional.of(dto.getTermindato()), dto.getBarn()));
        assertThat(exception).extracting("kode", "msg", "løsningsforslag")
            .containsExactly("FP-076345", "Dødsdato før fødselsdato", "Se over fødsels- og dødsdato");
    }

    @Test
    void skal_lagre_overstyrt_fødselsdato_og_termindato_når_register_har_en_annen_eksisterende_fødselsdato() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var barnDtoListe = List.of(new DokumentertBarnDto(FØDSELSDATO, null));
        var behandling = byggBehandlingBekreftetFødsel(scenario, barnDtoListe);
        var dto = new OverstyringFaktaOmFødselDto("begrunnelse", TERMINDATO,
            List.of(new DokumentertBarnDto(FØDSELSDATO, null), new DokumentertBarnDto(FØDSELSDATO.plusDays(1), null)));
        var ref = BehandlingReferanse.fra(behandling);
        var fh = familieHendelseTjeneste.hentAggregat(behandling.getId());
        // Act
        tjeneste.overstyrFaktaOmFødsel(ref, fh, Optional.of(dto.getTermindato()), dto.getBarn());

        // Assert
        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        var gjeldendeHendelse = fhFraRepo.getGjeldendeVersjon();
        assertThat(fhFraRepo.getGjeldendeAntallBarn()).isEqualTo(2);
        assertThat(gjeldendeHendelse.getTermindato()).hasValue(TERMINDATO);
        assertThat(gjeldendeHendelse.getBarna()).extracting(barn -> barn.getFødselsdato())
            .containsExactlyInAnyOrder(FØDSELSDATO, FØDSELSDATO.plusDays(1));
    }

    @Test
    void skal_lagre_termindato_i_gjeldende_versjon_når_det_ikke_finnes_noe_i_register() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var behandling = byggBehandlingTermin(scenario, TERMINDATO);
        var behandlingId = behandling.getId();
        var endretTermindato = TERMINDATO.minusWeeks(1);
        var dto = new OverstyringFaktaOmFødselDto("begrunnelse", endretTermindato, Collections.EMPTY_LIST);
        var ref = BehandlingReferanse.fra(behandling);
        var fh = familieHendelseTjeneste.hentAggregat(behandling.getId());
        // Act
        tjeneste.overstyrFaktaOmFødsel(ref,fh, Optional.of(dto.getTermindato()), dto.getBarn());

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
        var barnDtoListe = List.of(new DokumentertBarnDto(FØDSELSDATO, null));
        var behandling = byggBehandlingBekreftetFødsel(scenario, barnDtoListe);
        var dto = new OverstyringFaktaOmFødselDto("begrunnelse", TERMINDATO,
            List.of(new DokumentertBarnDto(FØDSELSDATO, dødsdato)));
        var ref = BehandlingReferanse.fra(behandling);
        var fh = familieHendelseTjeneste.hentAggregat(behandling.getId());
        // Act
        tjeneste.overstyrFaktaOmFødsel(ref, fh, Optional.of(dto.getTermindato()), dto.getBarn());

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
        var barnDtoListe = List.of(new DokumentertBarnDto(FØDSELSDATO, null), new DokumentertBarnDto(FØDSELSDATO, null));
        var behandling = byggBehandlingBekreftetFødsel(scenario, barnDtoListe);
        var dto = new OverstyringFaktaOmFødselDto("begrunnelse", TERMINDATO,
            List.of(new DokumentertBarnDto(FØDSELSDATO, dødsdato)));
        var ref = BehandlingReferanse.fra(behandling);
        var fh = familieHendelseTjeneste.hentAggregat(behandling.getId());
        // Act
        tjeneste.overstyrFaktaOmFødsel(ref, fh, Optional.of(dto.getTermindato()), dto.getBarn());

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
            .medFødselsDato(FØDSELSDATO, 1)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(TERMINDATO)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(TERMINDATO.minusMonths(1)));

        // Tvillinger i overstyrt hendelse
        var overstyrtHendelse = scenario.medOverstyrtHendelse();
        overstyrtHendelse.medAntallBarn(2).leggTilBarn(FØDSELSDATO).leggTilBarn(FØDSELSDATO).build();

        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        var fh = familieHendelseTjeneste.hentAggregat(behandling.getId());
        var dto = new OverstyringFaktaOmFødselDto("begrunnelse", TERMINDATO, List.of(new DokumentertBarnDto(FØDSELSDATO, null)));

        // Act
        var fhFraRepoFoer = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(fhFraRepoFoer.getGjeldendeVersjon().getBarna()).hasSize(2);

        tjeneste.overstyrFaktaOmFødsel(ref, fh, Optional.of(dto.getTermindato()), dto.getBarn());

        // Assert
        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        var gjeldende = fhFraRepo.getGjeldendeVersjon();
        assertThat(gjeldende.getBarna()).hasSize(1);
    }

    private Behandling byggBehandlingBekreftetFødsel(AbstractTestScenario<?> scenario, List<DokumentertBarnDto> barnListe) {
        var hendelse = scenario.medBekreftetHendelse();
        hendelse.medAntallBarn(barnListe.size());
        barnListe.forEach(barn -> hendelse.leggTilBarn(barn.fødselsdato(), barn.dødsdato()));
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

