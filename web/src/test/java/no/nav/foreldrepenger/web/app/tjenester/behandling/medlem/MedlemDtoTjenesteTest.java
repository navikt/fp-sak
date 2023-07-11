package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.api.EndringsresultatPersonopplysningerForMedlemskap;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonopplysningDtoTjeneste;

class MedlemDtoTjenesteTest {

    @Test
    void skal_lage_medlem_dto() {
        var navn = "Lisa gikk til skolen";
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var stp = LocalDate.now();
        scenario.medSøknadHendelse().medFødselsDato(stp);
        var søkerAktørId = AktørId.dummy();
        scenario.medBruker(søkerAktørId);

        var søker = scenario.opprettBuilderForRegisteropplysninger()
                .leggTilPersonopplysninger(
                        Personopplysning.builder()
                                .aktørId(søkerAktørId)
                                .navn(navn))
                .build();

        scenario.medRegisterOpplysninger(søker);
        scenario.leggTilMedlemskapPeriode(new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.ENDELIG).build());

        scenario.medMedlemskap()
                .medErEosBorger(true)
                .medBosattVurdering(true)
                .medOppholdsrettVurdering(true)
                .medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.MEDLEM)
                .medLovligOppholdVurdering(true);

        var behandling = scenario.lagMocked();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

        var personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
        var medlemTjenesteMock = mock(MedlemTjeneste.class);
        var personDtoTjeneste = new PersonopplysningDtoTjeneste(personopplysningTjenesteMock, repositoryProvider);

        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any(), any()))
                .thenReturn(EndringsresultatPersonopplysningerForMedlemskap.builder().build());

        var dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, skjæringstidspunktTjeneste,
            medlemTjenesteMock, personopplysningTjenesteMock, personDtoTjeneste);

        var medlemDtoOpt = dtoTjeneste.lagMedlemV2Dto(behandling.getId());
        assertThat(medlemDtoOpt).hasValueSatisfying(medlemDto -> {
            assertThat(medlemDto.getFom()).isEqualTo(stp);
            assertThat(medlemDto.getMedlemskapPerioder()).hasSize(1);
        });
    }

    @Test
    void skal_sette_fom_til_endring_i_personopplysningers_gjeldende_fra() {
        var navn = "Lisa gikk til skolen";
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var søkerAktørId = AktørId.dummy();
        scenario.medBruker(søkerAktørId);

        var søker = scenario.opprettBuilderForRegisteropplysninger()
                .leggTilPersonopplysninger(
                        Personopplysning.builder()
                                .aktørId(søkerAktørId)
                                .navn(navn))
                .build();

        scenario.medRegisterOpplysninger(søker);
        scenario.leggTilMedlemskapPeriode(new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.ENDELIG).build());

        scenario.medMedlemskap()
                .medErEosBorger(true)
                .medBosattVurdering(true)
                .medOppholdsrettVurdering(true)
                .medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.MEDLEM)
                .medLovligOppholdVurdering(true);

        var behandling = scenario.lagMocked();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

        var personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
        var medlemTjenesteMock = mock(MedlemTjeneste.class);
        var personDtoTjeneste = new PersonopplysningDtoTjeneste(personopplysningTjenesteMock, repositoryProvider);
        var endringFraDato = LocalDate.now().minusDays(5);
        var endringsresultatPersonopplysningerForMedlemskap = EndringsresultatPersonopplysningerForMedlemskap.builder()
                .leggTilEndring(DatoIntervallEntitet.fraOgMed(endringFraDato), "", "2")
                .build();
        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any(), any())).thenReturn(endringsresultatPersonopplysningerForMedlemskap);

        var dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, skjæringstidspunktTjeneste,
            medlemTjenesteMock, personopplysningTjenesteMock, personDtoTjeneste);

        var medlemDtoOpt = dtoTjeneste.lagMedlemV2Dto(behandling.getId());
        assertThat(medlemDtoOpt.get().getFom()).isEqualTo(endringFraDato);
    }

    @Test
    void skal_lage_inntekt_for_ektefelle() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        var navn = "Lisa gikk til skolen";
        var annenPart = "Tripp, tripp, tripp, det sa";
        var aktørIdSøker = AktørId.dummy();
        var aktørIdAnnenPart = AktørId.dummy();
        scenario.medBruker(aktørIdSøker);

        scenario.leggTilMedlemskapPeriode(new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.ENDELIG).build());

        var personInformasjon = scenario.opprettBuilderForRegisteropplysninger()
                .leggTilPersonopplysninger(
                        Personopplysning.builder()
                                .aktørId(aktørIdSøker)
                                .navn(navn))
                .leggTilPersonopplysninger(
                        Personopplysning.builder()
                                .aktørId(aktørIdAnnenPart)
                                .navn(annenPart))
                .build();

        scenario.medRegisterOpplysninger(personInformasjon);

        scenario.medMedlemskap()
                .medErEosBorger(true)
                .medBosattVurdering(true)
                .medOppholdsrettVurdering(true)
                .medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.MEDLEM)
                .medLovligOppholdVurdering(true);

        var behandling = scenario.lagMocked();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

        var personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());

        var medlemTjenesteMock = mock(MedlemTjeneste.class);
        var personDtoTjeneste = new PersonopplysningDtoTjeneste(personopplysningTjenesteMock, repositoryProvider);

        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any(), any()))
                .thenReturn(EndringsresultatPersonopplysningerForMedlemskap.builder().build());
        var dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, skjæringstidspunktTjeneste,
            medlemTjenesteMock, personopplysningTjenesteMock, personDtoTjeneste);

        var medlemDtoOpt = dtoTjeneste.lagMedlemV2Dto(behandling.getId());
        assertThat(medlemDtoOpt).hasValueSatisfying(medlemDto -> assertThat(medlemDto.getMedlemskapPerioder()).hasSize(1));
    }

    @Test
    void dto_før_registerinnhenting() {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());

        var personInformasjon = scenario.opprettBuilderForRegisteropplysninger().build();

        scenario.medRegisterOpplysninger(personInformasjon);

        var behandling = scenario.lagMocked();
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

        var personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());

        var medlemTjenesteMock = mock(MedlemTjeneste.class);
        var personDtoTjeneste = new PersonopplysningDtoTjeneste(personopplysningTjenesteMock, repositoryProvider);
        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any(), any()))
            .thenReturn(EndringsresultatPersonopplysningerForMedlemskap.builder().build());
        var dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, skjæringstidspunktTjeneste,
            medlemTjenesteMock,
            personopplysningTjenesteMock, personDtoTjeneste);

        var medlemDtoOpt = dtoTjeneste.lagMedlemV2Dto(behandling.getId());
        assertThat(medlemDtoOpt).hasValueSatisfying(medlemDto -> assertThat(medlemDto.getMedlemskapPerioder()).isEmpty());
    }

}
