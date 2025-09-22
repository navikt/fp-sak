package no.nav.foreldrepenger.familiehendelse.overstyring;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinje;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.FaktaFødselTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.DokumentertBarnDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OverstyringFaktaOmFødselDto;

@CdiDbAwareTest
class FaktaOmFødselOverstyringshåndtererTest {

    private final LocalDate termindato = LocalDate.now().minusDays(1);
    private final LocalDate fødselsdato = termindato.plusDays(1);
    private final LocalDate fødselsdato2 = termindato.plusDays(2);

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private HistorikkinnslagRepository historikkinnslagRepository;
    @Inject
    private FaktaFødselTjeneste faktaFødselTjeneste;
    @Inject
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private FaktaOmFødselOverstyringshåndterer oppdaterer;
    private AbstractTestScenario<?> scenario;

    @BeforeEach
    void setUp() {
        oppdaterer = new FaktaOmFødselOverstyringshåndterer(faktaFødselTjeneste, familieHendelseTjeneste);
        scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
    }

    @Test
    void skal_oppdatere_termindato() {
        var ref = byggBehandlingReferanse(termindato, null, List.of());
        var termindatoFraDto = termindato.plusDays(1);
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("Endrer termindato på grunn av feil i søknad.", termindatoFraDto, null);

        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        assertHistorikkinnslag(ref, List.of("__Overstyrt fakta om fødsel__.",
            "__Termindato__ er endret fra " + format(termindato) + " til __" + format(termindatoFraDto) + "__.", "__Er barnet født?__ Nei.",
            overstyringFaktaOmFødselDto.getBegrunnelse()));
        assertThat(familieHendelseEtterOverstyring.getOverstyrtVersjon().flatMap(FamilieHendelseEntitet::getTermindato).orElseThrow()).as(
            "Termindato skal være oppdatert").isEqualTo(termindatoFraDto);
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_ett_barn_legges_til() {
        var ref = byggBehandlingReferanse(termindato, fødselsdato, List.of(fødselsdato));
        // barnDtoListe inneholder gjeldende barn fra register i tillegg til det som skal legges til. Dette vil overstyre fra overstyrt versjon som inneholder 1 barn fra bekreftet hendelse.
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato, null));
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("Legger til ekstra barn i overstyring.", termindato, barnDtoListe);
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        assertAntallBarn(familieHendelseFørOverstyring, 1);
        assertAntallBarn(familieHendelseEtterOverstyring, 2);
        assertHistorikkinnslag(ref, List.of("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.",
            "__Antall barn__ er endret fra " + familieHendelseFørOverstyring.getGjeldendeAntallBarn() + " til __"
                + familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0) + "__.",
            "__Barn 2__ er satt til __f. " + format(fødselsdato) + "__.", overstyringFaktaOmFødselDto.getBegrunnelse()));
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_tre_barn_legges_til_med_to_forskjellige_fødselsdatoer() {
        var ref = byggBehandlingReferanse(termindato, fødselsdato, List.of(fødselsdato));
        // barnDtoListe inneholder gjeldende barn fra register i tillegg til det som skal legges til. Det vil si 1 barn fra bekreftet hendelse og 3 barn som skal legges til.
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato, null),
            new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato2, null));
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("Legger til ekstra barn i overstyring.", termindato, barnDtoListe);
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        assertAntallBarn(familieHendelseFørOverstyring, 1);
        assertAntallBarn(familieHendelseEtterOverstyring, 4);
        assertHistorikkinnslag(ref, List.of("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.",
            "__Antall barn__ er endret fra " + familieHendelseFørOverstyring.getGjeldendeAntallBarn() + " til __"
                + familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0) + "__.",
            "__Barn 2__ er satt til __f. " + format(fødselsdato) + "__.", "__Barn 3__ er satt til __f. " + format(fødselsdato) + "__.",
            "__Barn 4__ er satt til __f. " + format(fødselsdato2) + "__.", overstyringFaktaOmFødselDto.getBegrunnelse()));
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_to_barn_legges_til_hvor_to_er_i_overstyrt_fra_før() {
        var ref = byggBehandlingReferanse(termindato, fødselsdato, List.of(fødselsdato, fødselsdato));
        // barnDtoListe inneholder gjeldende barn fra register i tillegg til det som skal legges til. Det vil si 1 barn fra bekreftet hendelse og 3 barn som skal legges til.
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato, null),
            new DokumentertBarnDto(fødselsdato, fødselsdato), new DokumentertBarnDto(fødselsdato2, null));
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("Legger til ekstra barn i overstyring.", termindato, barnDtoListe);
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        assertAntallBarn(familieHendelseFørOverstyring, 2);
        assertAntallBarn(familieHendelseEtterOverstyring, 4);
        assertHistorikkinnslag(ref, List.of("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.",
            "__Antall barn__ er endret fra " + familieHendelseFørOverstyring.getGjeldendeAntallBarn() + " til __"
                + familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0) + "__.",
            "__Barn 3__ er satt til __f. " + format(fødselsdato) + " - d. " + format(fødselsdato) + "__.",
            "__Barn 4__ er satt til __f. " + format(fødselsdato2) + "__.", overstyringFaktaOmFødselDto.getBegrunnelse()));
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_barn_fjernes() {
        var ref = byggBehandlingReferanse(termindato, fødselsdato, List.of(fødselsdato, fødselsdato, fødselsdato));
        // barnDtoListe inneholder gjeldende barn fra register og fra overstyrt. Denne vil overstyre fra overstyrt versjon som inneholder 3 barn (1 barn fra register og 2 barn fra overstyring).
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null));
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("Fjernet 2 barn fra overstyring.", termindato, barnDtoListe);
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        assertAntallBarn(familieHendelseFørOverstyring, 3);
        assertAntallBarn(familieHendelseEtterOverstyring, 1);
        assertHistorikkinnslag(ref, List.of("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.",
            "__Antall barn__ er endret fra " + familieHendelseFørOverstyring.getGjeldendeAntallBarn() + " til __"
                + familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0) + "__.",
            "__Barn 2__ __f. " + format(fødselsdato) + "__ er fjernet.", "__Barn 3__ __f. " + format(fødselsdato) + "__ er fjernet.",
            overstyringFaktaOmFødselDto.getBegrunnelse()));
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_tre_barn_fjernes_med_to_forskjellige_fødselsdatoer() {
        var ref = byggBehandlingReferanse(termindato, fødselsdato, List.of(fødselsdato, fødselsdato, fødselsdato2, fødselsdato2));
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null));
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("Fjernet 3 barn fra overstyring.", termindato, barnDtoListe);
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        assertAntallBarn(familieHendelseFørOverstyring, 4);
        assertAntallBarn(familieHendelseEtterOverstyring, 1);
        assertHistorikkinnslag(ref, List.of("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.",
            "__Antall barn__ er endret fra " + familieHendelseFørOverstyring.getGjeldendeAntallBarn() + " til __"
                + familieHendelseEtterOverstyring.getOverstyrtVersjon().map(FamilieHendelseEntitet::getAntallBarn).orElse(0) + "__.",
            "__Barn 2__ __f. " + format(fødselsdato) + "__ er fjernet.", "__Barn 3__ __f. " + format(fødselsdato2) + "__ er fjernet.",
            "__Barn 4__ __f. " + format(fødselsdato2) + "__ er fjernet.", overstyringFaktaOmFødselDto.getBegrunnelse()));
    }

    @Test
    void skal_oppdatere_og_lage_historikk_ved_endret_dødsdato() {
        var dødsdato = fødselsdato.plusDays(1);
        var ref = byggBehandlingReferanse(termindato, fødselsdato, List.of(fødselsdato, fødselsdato));
        // barnDtoListe inneholder gjeldende barn. Et barn fra bekreftet og et barn som skal overstyres.
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato, dødsdato));
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("Endret på dødsdato.", termindato, barnDtoListe);

        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        assertBarn(familieHendelseEtterOverstyring, List.of(fødselsdato, fødselsdato), Arrays.asList(null, dødsdato));
        assertHistorikkinnslag(ref,
            List.of("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.", "__Antall barn:__ " + barnDtoListe.size() + ".",
                "__Barn 2__ er endret fra f. " + format(fødselsdato) + " til __f. " + format(fødselsdato) + " - d. " + format(dødsdato) + "__.",
                overstyringFaktaOmFødselDto.getBegrunnelse()));
    }

    @Test
    void skal_oppdatere_og_lage_historikk_når_barn_fjernes_og_legges_til_et_nytt_barn_samtidig() {
        var ref = byggBehandlingReferanse(termindato, fødselsdato, List.of(fødselsdato, fødselsdato));
        // barnDtoListe inneholder gjeldende barn fra register. Barn med fødselsdato er fjernet og barn med fødselsdato2 er lagt til.
        var barnDtoListe = List.of(new DokumentertBarnDto(fødselsdato, null), new DokumentertBarnDto(fødselsdato2, fødselsdato2));
        var overstyringFaktaOmFødselDto = new OverstyringFaktaOmFødselDto("Fjernet 1 barn med fødselsdato og lagt til 1 barn med fødselsdato2.",
            termindato, barnDtoListe);
        var familieHendelseFørOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        oppdaterer.håndterOverstyring(overstyringFaktaOmFødselDto, ref);
        var familieHendelseEtterOverstyring = familieHendelseTjeneste.hentAggregat(ref.behandlingId());

        assertBarn(familieHendelseFørOverstyring, List.of(fødselsdato, fødselsdato), Arrays.asList(null, null));
        assertBarn(familieHendelseEtterOverstyring, List.of(fødselsdato, fødselsdato2), Arrays.asList(null, fødselsdato2));
        assertHistorikkinnslag(ref,
            List.of("__Overstyrt fakta om fødsel__.", "__Er barnet født?__ Ja.", "__Antall barn:__ " + barnDtoListe.size() + ".",
                "__Barn 2__ er endret fra f. " + format(fødselsdato) + " til __f. " + format(fødselsdato2) + " - d. " + format(fødselsdato2) + "__.",
                overstyringFaktaOmFødselDto.getBegrunnelse()));
    }

    private BehandlingReferanse byggBehandlingReferanse(LocalDate termindato, LocalDate fødselsdato, List<LocalDate> overstyrteBarn) {
        byggSøknadhendelse(termindato, fødselsdato, overstyrteBarn);
        var behandling = scenario.lagre(repositoryProvider);
        return BehandlingReferanse.fra(behandling);
    }

    private void byggSøknadhendelse(LocalDate termindato, LocalDate fødselsdato, List<LocalDate> overstyrteBarn) {
        var søknadHendelse = scenario.medSøknadHendelse();
        søknadHendelse.medAntallBarn(1)
            .medTerminbekreftelse(scenario.medSøknadHendelse()
                .getTerminbekreftelseBuilder()
                .medTermindato(termindato)
                .medNavnPå("LEGEN LEGESEN")
                .medUtstedtDato(termindato.minusMonths(1)));
        if (fødselsdato != null) {
            søknadHendelse.medFødselsDato(fødselsdato);
        }
        scenario.medBruker(AktørId.dummy()).medSøknad().medMottattDato(termindato.minusWeeks(2));
        scenario.medSøknadAnnenPart().medAktørId(AktørId.dummy());
        if (!overstyrteBarn.isEmpty()) {
            scenario.medBekreftetHendelse().leggTilBarn(fødselsdato).medAntallBarn(1);
            var overstyrtHendelse = scenario.medOverstyrtHendelse().medAntallBarn(overstyrteBarn.size());
            for (var barn : overstyrteBarn) {
                overstyrtHendelse.leggTilBarn(barn);
            }
        }
    }

    private static void assertAntallBarn(FamilieHendelseGrunnlagEntitet familieHendelse, int forventetAntall) {
        assertThat(familieHendelse.getOverstyrtVersjon()).get().extracting(v -> v.getBarna().size()).isEqualTo(forventetAntall);
    }

    private static void assertBarn(FamilieHendelseGrunnlagEntitet familieHendelse,
                            List<LocalDate> forventedeFødselsdatoer,
                            List<LocalDate> forventedeDødsdatoer) {
        var barna = familieHendelse.getOverstyrtVersjon().map(FamilieHendelseEntitet::getBarna).orElseThrow();
        assertThat(barna).hasSize(forventedeFødselsdatoer.size());

        for (int i = 0; i < forventedeFødselsdatoer.size(); i++) {
            var barn = barna.get(i);
            assertThat(barn.getFødselsdato()).isEqualTo(forventedeFødselsdatoer.get(i));
            if (forventedeDødsdatoer.get(i) != null) {
                assertThat(barn.getDødsdato()).contains(forventedeDødsdatoer.get(i));
            } else {
                assertThat(barn.getDødsdato()).isEmpty();
            }
        }
    }

    private void assertHistorikkinnslag(BehandlingReferanse ref, List<String> forventedeTekster) {
        var historikkinnslag = historikkinnslagRepository.hent(ref.saksnummer()).getFirst();
        assertThat(historikkinnslag.getLinjer()).extracting(HistorikkinnslagLinje::getTekst).containsExactlyElementsOf(forventedeTekster);
    }
}
