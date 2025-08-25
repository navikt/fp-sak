package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

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
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.DokumentertBarnDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt.OverstyringFaktaOmFødselDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.FødselDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.dto.Kilde;
import no.nav.vedtak.exception.FunksjonellException;

public class FaktaFødselTjenesteTest extends EntityManagerAwareTest {
    private static final LocalDate FØDSELSDATO = LocalDate.now();
    private static final LocalDate TERMINDATO = LocalDate.now();
    private static final LocalDate UTSTEDTDATO = LocalDate.now().minusMonths(1);
    private BehandlingRepositoryProvider repositoryProvider;
    private FaktaFødselTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        var fhTjeneste = new FamilieHendelseTjeneste(null, repositoryProvider.getFamilieHendelseRepository());
        tjeneste = new FaktaFødselTjeneste(fhTjeneste, repositoryProvider.getBehandlingRepository());
    }

    // HentFaktaOmFødsel

    @Test
    void skal_kunne_hente_fakta_om_fødsel_med_både_overstyrt_og_bekreftet_barn() {
        // Hvis det finnes ett barn i overstyrt og et annet barn i bekreftet, skal gjeldende liste inneholde begge barnene:
        // Ett barn fra overstyrt (kanOverstyres = true) og ett barn fra FREG (kanOverstyres = false).

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelseTermin(scenario, TERMINDATO, 1);

        scenario.medBekreftetHendelse().medAntallBarn(1).leggTilBarn(FØDSELSDATO);

        scenario.medOverstyrtHendelse()
            .medAntallBarn(2)
            .leggTilBarn(FØDSELSDATO)
            .leggTilBarn(FØDSELSDATO); // Overstyrt barn vil inneholder alle barn fra bekreftet og overstyrt.

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.antallBarn()).extracting(FødselDto.Gjeldende.AntallBarn::antall, FødselDto.Gjeldende.AntallBarn::kilde)
            .containsExactly(2, Kilde.SAKSBEHANDLER);
        assertThat(gjeldende.termin()).extracting(FødselDto.Gjeldende.Termin::termindato, FødselDto.Gjeldende.Termin::kilde)
            .containsExactly(TERMINDATO, Kilde.SØKNAD);

        assertThat(gjeldende.barn()).hasSize(2)
            .extracting(b -> b.barn().fødselsdato(), FødselDto.Gjeldende.GjeldendeBarn::kilde, FødselDto.Gjeldende.GjeldendeBarn::kanOverstyres)
            .containsExactlyInAnyOrder(tuple(FØDSELSDATO, Kilde.SAKSBEHANDLER, true), tuple(FØDSELSDATO, Kilde.FOLKEREGISTER, false));
    }

    @Test
    void skal_hente_fakta_om_fødsel_med_bekreftet_barn_og_overstyrt_dødfødt_barn() {
        // Hvis det finnes et dødfødt barn i overstyring og et annet levende barn i bekreftet, skal gjeldende liste inneholde begge barnene:
        // Ett barn fra overstyring (kanOverstyres = true) og ett barn fra FREG (kanOverstyres = false).
        // Her er det f.eks tvillinger som er født i utlandet, hvor én overlever og det andre barnet er dødfødt, og mor har dødsattest for det dødfødte barnet.

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelseTermin(scenario, TERMINDATO, 1);

        scenario.medBekreftetHendelse().medAntallBarn(1).leggTilBarn(FØDSELSDATO);

        var dødsdato = LocalDate.now().plusDays(2);
        scenario.medOverstyrtHendelse().medAntallBarn(2).leggTilBarn(FØDSELSDATO).leggTilBarn(FØDSELSDATO, dødsdato); // Dødfødt barn

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        // Sjekk at termindato er korrekt og kan overstyres fra søknad
        assertThat(gjeldende.termin()).extracting(FødselDto.Gjeldende.Termin::termindato, FødselDto.Gjeldende.Termin::kilde)
            .containsExactly(TERMINDATO, Kilde.SØKNAD);

        // Sjekk at utstedtdato er korrekt
        assertThat(gjeldende.utstedtdato()).extracting(FødselDto.Gjeldende.Utstedtdato::utstedtdato, FødselDto.Gjeldende.Utstedtdato::kilde)
            .containsExactly(UTSTEDTDATO, Kilde.SØKNAD);

        // Sjekk at begge barn (overstyrt og bekreftet) er med
        assertThat(gjeldende.barn()).hasSize(2);
        assertThat(gjeldende.antallBarn()).extracting(FødselDto.Gjeldende.AntallBarn::antall, FødselDto.Gjeldende.AntallBarn::kilde)
            .containsExactly(2, Kilde.SAKSBEHANDLER);


        // Sjekk at barn har riktige verdier for fødselsdato, dødsdato, kilde og kanOverstyres
        assertThat(gjeldende.barn()).extracting(b -> b.barn().fødselsdato(), b -> b.barn().dødsdato(), FødselDto.Gjeldende.GjeldendeBarn::kilde,
                FødselDto.Gjeldende.GjeldendeBarn::kanOverstyres)
            .containsExactlyInAnyOrder(tuple(FØDSELSDATO, dødsdato, Kilde.SAKSBEHANDLER, true), tuple(FØDSELSDATO, null, Kilde.FOLKEREGISTER, false));
    }

    @Test
    void skal_kunne_hente_fakta_om_fødsel_med_overstyrt_barn_termindato_og_ikke_fra_søknad() {
        // Hvis det finnes barn både i overstyrt og søknad, skal gjeldende liste kun inneholde overstyrte barn med kanOverstyres = true.
        // Dette er f.eks. tilfelle når mor søker feil antall barn og termindato. Da har saksbehandler mulighet til å overstyre til riktig antall barn og termindato.

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelseTermin(scenario, TERMINDATO.minusWeeks(1), 2);

        var overstyrthendelse = scenario.medOverstyrtHendelse();
        overstyrthendelse.medAntallBarn(1)
            .medTerminbekreftelse(
                overstyrthendelse.getTerminbekreftelseBuilder().medTermindato(TERMINDATO).medUtstedtDato(UTSTEDTDATO).medNavnPå("LEGEN LEGESEN"))
            .leggTilBarn(FØDSELSDATO);

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.termin()).extracting(FødselDto.Gjeldende.Termin::termindato, FødselDto.Gjeldende.Termin::kilde)
            .containsExactly(TERMINDATO, Kilde.SAKSBEHANDLER);
        assertThat(gjeldende.barn()).hasSize(1);
        assertThat(gjeldende.antallBarn()).extracting(FødselDto.Gjeldende.AntallBarn::antall, FødselDto.Gjeldende.AntallBarn::kilde)
            .containsExactly(1, Kilde.SAKSBEHANDLER);
        assertThat(gjeldende.barn()).extracting(b -> b.barn().fødselsdato(), b -> b.barn().dødsdato(), FødselDto.Gjeldende.GjeldendeBarn::kilde,
            FødselDto.Gjeldende.GjeldendeBarn::kanOverstyres).containsExactlyInAnyOrder(tuple(FØDSELSDATO, null, Kilde.SAKSBEHANDLER, true));
    }

    @Test
    void skal_kunne_hente_fakta_om_fødsel_med_kun_søknadsbarn_når_kun_disse_finnes() {
        // Dersom det kun finnes barn fra søknad, skal gjeldende liste inneholde disse barna med kanOverstyres = true.

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();

        // Søker om foreldrepenger for 2 barn med termindato, hvor ingen av barna er født ennå.
        var søknadHendelse = scenario.medSøknadHendelse();
        søknadHendelse.medAntallBarn(2)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(TERMINDATO)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(UTSTEDTDATO));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.barn()).isEmpty(); // Ingen barn er født ennå, så listen skal være tom

        // Mor har søkt om foreldrepenger for 2 barn, men ingen av barna er født ennå
        assertThat(gjeldende.antallBarn()).extracting(FødselDto.Gjeldende.AntallBarn::antall, FødselDto.Gjeldende.AntallBarn::kilde)
            .containsExactly(2, Kilde.SØKNAD);
        assertThat(gjeldende.termin()).extracting(FødselDto.Gjeldende.Termin::termindato, FødselDto.Gjeldende.Termin::kilde)
            .containsExactly(TERMINDATO, Kilde.SØKNAD);

        assertThat(gjeldende.utstedtdato()).extracting(FødselDto.Gjeldende.Utstedtdato::utstedtdato, FødselDto.Gjeldende.Utstedtdato::kilde)
            .containsExactly(UTSTEDTDATO, Kilde.SØKNAD);
    }

    @Test
    void skal_hente_fakta_om_fødsel_med_kun_bekreftet_barn_når_kun_disse_finnes() {
        // Når det finnes både bekreftede og søknadsbarn, skal gjeldende liste kun inneholde bekreftede barn med kanOverstyres = false.
        // Det skal fortsatt være mulig å legge til eller fjerne barn via overstyring.

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        byggSøknadhendelseTermin(scenario, TERMINDATO, 1);

        scenario.medBekreftetHendelse().medAntallBarn(2).leggTilBarn(FØDSELSDATO).leggTilBarn(FØDSELSDATO.plusDays(1));

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.barn()).hasSize(2);

        assertThat(gjeldende.antallBarn()).extracting(FødselDto.Gjeldende.AntallBarn::antall, FødselDto.Gjeldende.AntallBarn::kilde)
            .containsExactly(2, Kilde.FOLKEREGISTER);
        assertThat(gjeldende.termin()).extracting(FødselDto.Gjeldende.Termin::termindato, FødselDto.Gjeldende.Termin::kilde)
            .containsExactly(TERMINDATO, Kilde.SØKNAD);

        assertThat(gjeldende.barn()).extracting(b -> b.barn().fødselsdato(), b -> b.barn().dødsdato(), FødselDto.Gjeldende.GjeldendeBarn::kilde,
                FødselDto.Gjeldende.GjeldendeBarn::kanOverstyres)
            .containsExactlyInAnyOrder(tuple(FØDSELSDATO, null, Kilde.FOLKEREGISTER, false),
                tuple(FØDSELSDATO.plusDays(1), null, Kilde.FOLKEREGISTER, false));
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
                .medUtstedtDato(UTSTEDTDATO));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());

        var bekreftetHendelse = scenario.medBekreftetHendelse();
        bekreftetHendelse.medFødselsDato(FØDSELSDATO).medAntallBarn(1);

        var behandling = scenario.lagre(repositoryProvider);

        // Act
        var fødselDto = tjeneste.hentFaktaOmFødsel(behandling.getId());

        // Assert
        var gjeldende = fødselDto.gjeldende();
        assertThat(gjeldende).isNotNull();
        assertThat(gjeldende.termin().termindato()).isEqualTo(TERMINDATO);
        assertThat(gjeldende.barn()).hasSize(1)
            .extracting(b -> b.barn().fødselsdato(), FødselDto.Gjeldende.GjeldendeBarn::kilde, FødselDto.Gjeldende.GjeldendeBarn::kanOverstyres)
            .containsExactly(tuple(FØDSELSDATO, Kilde.FOLKEREGISTER, false));
    }

    // Overstyring

    @Test
    void skal_kaste_exception_når_fødselsdato_ikke_er_innenfor_gyldig_intervall_for_termindato() {
        // Arrange
        var fødselsdato = TERMINDATO.plusWeeks(8);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null));
        var behandling = byggBehandlingBekreftetFødsel(scenario, barnDtoListe);
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("Legger til fødselsdato 8 uker etter termindato.", TERMINDATO, barnDtoListe);

        // Act & Assert
        var behandlingId = behandling.getId();
        var exception = assertThrows(FunksjonellException.class, () -> tjeneste.overstyrFaktaOmFødsel(behandlingId, overstyringFaktaOmFødselDto));
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
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("Legger til dødsdato før fødselsdato", TERMINDATO, barnDtoListe);

        // Act og Assert
        var behandlingId = behandling.getId();
        var exception = assertThrows(FunksjonellException.class, () -> tjeneste.overstyrFaktaOmFødsel(behandlingId, overstyringFaktaOmFødselDto));
        assertThat(exception).extracting("kode", "msg", "løsningsforslag")
            .containsExactly("FP-076345", "Dødsdato før fødselsdato", "Se over fødsels- og dødsdato");
    }

    @Test
    void skal_lagre_overstyrt_fødselsdato_og_termindato_når_register_har_en_annen_eksisterende_fødselsdato() {
        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var barnDtoListe = List.of(new DokumentertBarnDto(FØDSELSDATO, null));
        var behandling = byggBehandlingBekreftetFødsel(scenario, barnDtoListe);
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("begrunnelse", TERMINDATO,
            List.of(new DokumentertBarnDto(FØDSELSDATO, null), new DokumentertBarnDto(FØDSELSDATO.plusDays(1), null)));

        // Act
        tjeneste.overstyrFaktaOmFødsel(behandling.getId(), overstyringFaktaOmFødselDto);

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
        var barnDtoListe = List.of(new DokumentertBarnDto(FØDSELSDATO, null));
        var behandling = byggBehandlingBekreftetFødsel(scenario, barnDtoListe);
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("begrunnelse", null, List.of(new DokumentertBarnDto(FØDSELSDATO, dødsdato)));

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
        var barnDtoListe = List.of(new DokumentertBarnDto(FØDSELSDATO, null), new DokumentertBarnDto(FØDSELSDATO, null));
        var behandling = byggBehandlingBekreftetFødsel(scenario, barnDtoListe);
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("begrunnelse", null, List.of(new DokumentertBarnDto(FØDSELSDATO, dødsdato)));

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

        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("begrunnelse", null, List.of(new DokumentertBarnDto(FØDSELSDATO, null)));

        // Act
        var fhFraRepoFoer = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        assertThat(fhFraRepoFoer.getGjeldendeVersjon().getBarna()).hasSize(2);

        tjeneste.overstyrFaktaOmFødsel(behandling.getId(), overstyringFaktaOmFødselDto);

        // Assert
        var fhFraRepo = repositoryProvider.getFamilieHendelseRepository().hentAggregat(behandling.getId());
        var gjeldende = fhFraRepo.getGjeldendeVersjon();
        assertThat(gjeldende.getBarna()).hasSize(1);
    }

    private void byggSøknadhendelseTermin(AbstractTestScenario<?> scenario, LocalDate termindato, int antallBarn) {
        var søknadshendelse = scenario.medSøknadHendelse();
        søknadshendelse.medTerminbekreftelse(scenario.medSøknadHendelse()
            .getTerminbekreftelseBuilder()
            .medTermindato(termindato)
            .medNavnPå("LEGEN LEGESEN")
            .medUtstedtDato(UTSTEDTDATO)).medFødselsDato(FØDSELSDATO).medAntallBarn(antallBarn);
        if (antallBarn == 2) {
            søknadshendelse.leggTilBarn(FØDSELSDATO);
        }
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());
    }

    private Behandling byggBehandlingBekreftetFødsel(AbstractTestScenario<?> scenario, List<DokumentertBarnDto> barnListe) {
        var hendelse = scenario.medBekreftetHendelse();
        hendelse.medAntallBarn(barnListe.size());
        barnListe.forEach(barn -> hendelse.leggTilBarn(barn.getFødselsdato(), barn.getDødsdato().orElse(null)));
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
