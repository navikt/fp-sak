package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.dto.DokumentertBarnDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.fødsel.dto.OverstyringFaktaOmFødselDto;
import no.nav.foreldrepenger.familiehendelse.event.FamiliehendelseEventPubliserer;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

class FaktaFødselTjenesteTest extends EntityManagerAwareTest {
    private static final LocalDate FØDSELSDATO = LocalDate.now();
    private static final LocalDate TERMINDATO = LocalDate.now();

    private BehandlingRepositoryProvider repositoryProvider;
    private FaktaFødselTjeneste tjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        familieHendelseTjeneste = new FamilieHendelseTjeneste(mock(FamiliehendelseEventPubliserer.class),
            repositoryProvider.getFamilieHendelseRepository());
        tjeneste = new FaktaFødselTjeneste(familieHendelseTjeneste, mock(OpplysningsPeriodeTjeneste.class), mock(HistorikkinnslagRepository.class));
    }

    @Test
    void skal_kaste_exception_når_fødselsdato_ikke_er_innenfor_gyldig_intervall_for_termindato() {
        var barnDtoListe = List.of(new DokumentertBarnDto(TERMINDATO.plusWeeks(8), null));
        var dto = new OverstyringFaktaOmFødselDto("Legger til fødselsdato 8 uker etter termindato.", TERMINDATO, barnDtoListe);
        var ref = BehandlingReferanse.fra(byggBehandlingBekreftetFødsel(ScenarioMorSøkerForeldrepenger.forFødsel(), barnDtoListe));
        var fh = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        var exception = assertThrows(FunksjonellException.class,
            () -> tjeneste.overstyrFaktaOmFødsel(ref, fh, Optional.of(dto.getTermindato()), dto.getBarn(), dto.getBegrunnelse(), false));
        assertThat(exception).extracting("kode", "msg", "løsningsforslag")
            .containsExactly("FP-076346", "For stort avvik termin/fødsel", "Sjekk datoer eller meld sak i Porten");
    }

    @Test
    void skal_kaste_exception_når_dødsdato_er_før_fødselsdato() {
        var fødselsdato = TERMINDATO.plusDays(1);
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato, fødselsdato.minusDays(1)));
        var behandling = byggBehandlingBekreftetFødsel(ScenarioMorSøkerForeldrepenger.forFødsel(), barnDtoListe);
        var dto = new OverstyringFaktaOmFødselDto("Legger til dødsdato før fødselsdato", TERMINDATO, barnDtoListe);
        var fh = familieHendelseTjeneste.hentAggregat(behandling.getId());

        var ref = BehandlingReferanse.fra(behandling);
        var exception = assertThrows(FunksjonellException.class,
            () -> tjeneste.overstyrFaktaOmFødsel(ref, fh, Optional.of(dto.getTermindato()), dto.getBarn(),
                dto.getBegrunnelse(), false));
        assertThat(exception).extracting("kode", "msg", "løsningsforslag")
            .containsExactly("FP-076345", "Dødsdato før fødselsdato", "Se over fødsels- og dødsdato");
    }

    @Test
    void skal_lagre_overstyrt_fødselsdato_og_termindato_når_register_har_en_annen_eksisterende_fødselsdato() {
        var behandling = byggBehandlingBekreftetFødsel(ScenarioMorSøkerForeldrepenger.forFødsel(),
            List.of(new DokumentertBarnDto(FØDSELSDATO, null)));
        var dto = new OverstyringFaktaOmFødselDto("begrunnelse", TERMINDATO,
            List.of(new DokumentertBarnDto(FØDSELSDATO, null), new DokumentertBarnDto(FØDSELSDATO.plusDays(1), null)));
        var fh = familieHendelseTjeneste.hentAggregat(behandling.getId());

        tjeneste.overstyrFaktaOmFødsel(BehandlingReferanse.fra(behandling), fh, Optional.of(dto.getTermindato()), dto.getBarn(), dto.getBegrunnelse(),
            false);

        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        var gjeldendeHendelse = fhFraRepo.getGjeldendeVersjon();
        assertThat(fhFraRepo.getGjeldendeAntallBarn()).isEqualTo(2);
        assertThat(gjeldendeHendelse.getTermindato()).hasValue(TERMINDATO);
        assertThat(gjeldendeHendelse.getBarna()).extracting(UidentifisertBarn::getFødselsdato)
            .containsExactlyInAnyOrder(FØDSELSDATO, FØDSELSDATO.plusDays(1));
    }

    @Test
    void skal_lagre_termindato_i_gjeldende_versjon_når_det_ikke_finnes_noe_i_register() {
        var behandling = byggBehandlingTermin(ScenarioMorSøkerEngangsstønad.forFødsel());
        var endretTermindato = TERMINDATO.minusWeeks(1);
        var dto = new OverstyringFaktaOmFødselDto("begrunnelse", endretTermindato, List.of());
        var fh = familieHendelseTjeneste.hentAggregat(behandling.getId());

        tjeneste.overstyrFaktaOmFødsel(BehandlingReferanse.fra(behandling), fh, Optional.of(dto.getTermindato()), dto.getBarn(), dto.getBegrunnelse(),
            false);

        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        var gjeldendeHendelse = fhFraRepo.getGjeldendeVersjon();
        assertThat(gjeldendeHendelse.getTermindato()).hasValue(endretTermindato);
    }

    @Test
    void skal_kunne_lagre_overstyrt_dødfødsel_hvis_ikke_finnes_i_register_fra_før() {
        var dødsdato = FØDSELSDATO.plusDays(1);
        var behandling = byggBehandlingBekreftetFødsel(ScenarioMorSøkerForeldrepenger.forFødsel(),
            List.of(new DokumentertBarnDto(FØDSELSDATO, null)));
        var dto = new OverstyringFaktaOmFødselDto("begrunnelse", TERMINDATO, List.of(new DokumentertBarnDto(FØDSELSDATO, dødsdato)));
        var fh = familieHendelseTjeneste.hentAggregat(behandling.getId());

        tjeneste.overstyrFaktaOmFødsel(BehandlingReferanse.fra(behandling), fh, Optional.of(dto.getTermindato()), dto.getBarn(), dto.getBegrunnelse(),
            false);

        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(fhFraRepo.getOverstyrtVersjon()).hasValue(fhFraRepo.getGjeldendeVersjon());
        var gjeldendeVersjon = fhFraRepo.getGjeldendeVersjon();
        assertThat(gjeldendeVersjon.getBarna()).hasSize(1);
        assertThat(gjeldendeVersjon.getBarna().getFirst().getDødsdato()).hasValue(dødsdato);
    }

    @Test
    void skal_kunne_lagre_overstyrt_dødfødsel_hvis_to_barn_fødselsdatoer_finnes_i_register_fra_før() {
        var dødsdato = FØDSELSDATO.plusDays(1);
        var behandling = byggBehandlingBekreftetFødsel(ScenarioMorSøkerForeldrepenger.forFødsel(),
            List.of(new DokumentertBarnDto(FØDSELSDATO, null), new DokumentertBarnDto(FØDSELSDATO, null)));
        var dto = new OverstyringFaktaOmFødselDto("begrunnelse", TERMINDATO, List.of(new DokumentertBarnDto(FØDSELSDATO, dødsdato)));
        var fh = familieHendelseTjeneste.hentAggregat(behandling.getId());

        tjeneste.overstyrFaktaOmFødsel(BehandlingReferanse.fra(behandling), fh, Optional.of(dto.getTermindato()), dto.getBarn(), dto.getBegrunnelse(),
            false);

        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(fhFraRepo.getOverstyrtVersjon()).hasValue(fhFraRepo.getGjeldendeVersjon());
        var gjeldendeVersjon = fhFraRepo.getGjeldendeVersjon();
        assertThat(gjeldendeVersjon.getBarna()).hasSize(1);
        assertThat(gjeldendeVersjon.getBarna().getFirst().getDødsdato()).hasValue(dødsdato);
    }

    @Test
    void skal_kunne_fjerne_et_av_to_barn_fra_gjeldende_ved_feiltakelse_lagt_inn_opprinnelig() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse()
            .medAntallBarn(1)
            .medFødselsDato(FØDSELSDATO, 1)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(TERMINDATO)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(TERMINDATO.minusMonths(1)));

        // Tvillinger i overstyrt hendelse
        scenario.medOverstyrtHendelse().medAntallBarn(2).leggTilBarn(FØDSELSDATO).leggTilBarn(FØDSELSDATO).build();

        var behandling = scenario.lagre(repositoryProvider);
        var fh = familieHendelseTjeneste.hentAggregat(behandling.getId());
        var dto = new OverstyringFaktaOmFødselDto("begrunnelse", TERMINDATO, List.of(new DokumentertBarnDto(FØDSELSDATO, null)));

        var fhFraRepoFoer = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());

        tjeneste.overstyrFaktaOmFødsel(BehandlingReferanse.fra(behandling), fh, Optional.of(dto.getTermindato()), dto.getBarn(), dto.getBegrunnelse(),
            false);

        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        var gjeldende = fhFraRepo.getGjeldendeVersjon();
        assertThat(fhFraRepoFoer.getGjeldendeVersjon().getBarna()).hasSize(2);
        assertThat(gjeldende.getBarna()).hasSize(1);
    }

    private Behandling byggBehandlingBekreftetFødsel(AbstractTestScenario<?> scenario, List<DokumentertBarnDto> barnListe) {
        var hendelse = scenario.medBekreftetHendelse().medAntallBarn(barnListe.size());
        barnListe.forEach(barn -> hendelse.leggTilBarn(barn.fødselsdato(), barn.dødsdato()));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());
        return scenario.lagre(repositoryProvider);
    }

    private Behandling byggBehandlingTermin(AbstractTestScenario<?> scenario) {
        scenario.medSøknadHendelse()
            .medAntallBarn(1)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(FaktaFødselTjenesteTest.TERMINDATO)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(FaktaFødselTjenesteTest.TERMINDATO.minusMonths(1)));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());
        return scenario.lagre(repositoryProvider);
    }
}
