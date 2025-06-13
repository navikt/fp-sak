package no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinje;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.UidentifisertBarnDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.fødsel.aksjonspunkt.OverstyringFaktaOmFødselDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.overstyring.FaktaOmFødselOverstyringshåndterer;

@CdiDbAwareTest
public class FaktaOmFødselOverstyringshåndtererTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private HistorikkinnslagRepository historikkinnslagRepository;
    @Inject
    private FaktaFødselTjeneste faktaFødselTjeneste;
    @Inject
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    private FaktaOmFødselOverstyringshåndterer oppdaterer;

    @BeforeEach
    public void setUp() {
        oppdaterer = new FaktaOmFødselOverstyringshåndterer(historikkinnslagRepository, faktaFødselTjeneste, familieHendelseTjeneste);
    }

    @Test
    void skal_oppdatere_termindato() {
        // Arrange
        var termindato = LocalDate.now();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse()
            .medAntallBarn(1)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(termindato.minusMonths(1)));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(LocalDate.now().minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());

        var behandling = scenario.lagre(repositoryProvider);

        var ref = BehandlingReferanse.fra(behandling);
        var termindatoFraDto = LocalDate.now().plusDays(1);
        var begrunnelse = "Endrer termindato på grunn av feil i søknad.";
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto(begrunnelse, termindatoFraDto, null);

        // Act
        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var historikkinnslag = historikkinnslagRepository.hent(ref.saksnummer()).getFirst();

        // Assert
        assertThat(familieHendelseEtterOverstyring.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTermindato).orElseThrow()).as(
            "Termindato skal være oppdatert").isEqualTo(termindatoFraDto);
        assertThat(historikkinnslag.getLinjer()).extracting(HistorikkinnslagLinje::getTekst)
            .containsAll(List.of("Overstyrt fakta om fødsel.",
                begrunnelse,
                "__Termindato__ er endret fra " + format(termindato) + " til __" + format(termindatoFraDto) + "__."));
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_barn_legges_til() {
        var termindato = LocalDate.now().minusDays(1);
        var fødselsdato = LocalDate.now();

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse()
            .medAntallBarn(1)
            .medFødselsDato(fødselsdato)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(termindato.minusMonths(1)));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(termindato.minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());

        scenario.medBekreftetHendelse().leggTilBarn(fødselsdato).medAntallBarn(1);
        scenario.medOverstyrtHendelse().medAntallBarn(1).leggTilBarn(fødselsdato);

        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        var barnDtoListe = List.of(new UidentifisertBarnDto(fødselsdato, null), new UidentifisertBarnDto(fødselsdato, null));
        var begrunnelse = "Legger til ekstra barn i overstyring.";
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto(begrunnelse, termindato, barnDtoListe);

        // Act
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var historikkinnslag = historikkinnslagRepository.hent(ref.saksnummer()).getFirst();


        // Assert
        assertThat(familieHendelseFørOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn før overstyring")
            .isEqualTo(1);
        assertThat(familieHendelseEtterOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn etter overstyring")
            .isEqualTo(2);

        assertThat(historikkinnslag.getLinjer()).extracting(HistorikkinnslagLinje::getTekst)
            .containsAll(List.of("Overstyrt fakta om fødsel.", begrunnelse,
                "__Antall barn__ er endret fra " + familieHendelseFørOverstyring.getGjeldendeAntallBarn() + " til __"
                    + familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0) + "__.",
                "Barn lagt til med fødselsdato: " + fødselsdato + "."));
    }

    @Test
    void skal_lage_historikk_nar_barn_fjernes() {
        var termindato = LocalDate.now().minusDays(1);
        var fødselsdato = LocalDate.now();

        // Arrange
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medSøknadHendelse()
            .medAntallBarn(1)
            .medFødselsDato(fødselsdato)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(termindato.minusMonths(1)));
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(termindato.minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());

        scenario.medBekreftetHendelse().leggTilBarn(fødselsdato).medAntallBarn(1);
        // Overstyrt versjon inneholder 1 barn fra bekreftet hendelse og det som skal fjernes. Dersom det kun inneholder 1 barn fra bekreftet hendelse, blir de andre barna fjernet fra overstyring.
        scenario.medOverstyrtHendelse().medAntallBarn(3).leggTilBarn(fødselsdato).leggTilBarn(fødselsdato).leggTilBarn(fødselsdato);

        var behandling = scenario.lagre(repositoryProvider);
        var ref = BehandlingReferanse.fra(behandling);
        // barnDtoListe inneholder gjeldende barn fra register og fra overstyrt. Denne vil overstyre fra overstyrt versjon som inneholder 3 barn (1 barn fra register og 2 barn fra overstyring).
        var barnDtoListe = List.of(new UidentifisertBarnDto(fødselsdato, null));
        var begrunnelse = "Fjernet 2 barn fra overstyring.";
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto(begrunnelse, termindato, barnDtoListe);

        // Act
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());
        var historikkinnslag = historikkinnslagRepository.hent(ref.saksnummer()).getFirst();

        // Assert
        assertThat(familieHendelseFørOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn før overstyring")
            .isEqualTo(3);
        assertThat(familieHendelseEtterOverstyring.getOverstyrtVersjon()).get()
            .extracting(v -> v.getBarna().size())
            .as("Antall barn etter overstyring")
            .isEqualTo(1);

        assertThat(historikkinnslag.getLinjer()).extracting(HistorikkinnslagLinje::getTekst)
            .containsAll(List.of("Overstyrt fakta om fødsel.", begrunnelse,
                "__Antall barn__ er endret fra " + familieHendelseFørOverstyring.getGjeldendeAntallBarn() + " til __"
                    + familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0) + "__.",
                "Barn fjernet med fødselsdato: " + fødselsdato + "."));
    }
}
