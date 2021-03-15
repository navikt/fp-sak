package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
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

public class MedlemDtoTjenesteTest {

    @Test
    public void skal_lage_medlem_dto() {
        String navn = "Lisa gikk til skolen";
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var stp = LocalDate.now();
        scenario.medSøknadHendelse().medFødselsDato(stp);
        AktørId søkerAktørId = AktørId.dummy();
        scenario.medBruker(søkerAktørId);

        PersonInformasjon søker = scenario.opprettBuilderForRegisteropplysninger()
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

        Behandling behandling = scenario.lagMocked();
        final BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

        PersonopplysningTjeneste personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
        MedlemTjeneste medlemTjenesteMock = mock(MedlemTjeneste.class);
        var personDtoTjeneste = new PersonopplysningDtoTjeneste(personopplysningTjenesteMock, repositoryProvider);

        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any()))
                .thenReturn(EndringsresultatPersonopplysningerForMedlemskap.builder().build());

        MedlemDtoTjeneste dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, skjæringstidspunktTjeneste,
            medlemTjenesteMock, personopplysningTjenesteMock, personDtoTjeneste);

        var medlemDtoOpt = dtoTjeneste.lagMedlemV2Dto(behandling.getId());
        assertThat(medlemDtoOpt).hasValueSatisfying(medlemDto -> {
            assertThat(medlemDto.getFom()).isEqualTo(stp);
            assertThat(medlemDto.getMedlemskapPerioder()).hasSize(1);
        });
    }

    @Test
    public void skal_sette_fom_til_endring_i_personopplysningers_gjeldende_fra() {
        String navn = "Lisa gikk til skolen";
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        AktørId søkerAktørId = AktørId.dummy();
        scenario.medBruker(søkerAktørId);

        PersonInformasjon søker = scenario.opprettBuilderForRegisteropplysninger()
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

        Behandling behandling = scenario.lagMocked();
        final BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

        PersonopplysningTjeneste personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
        MedlemTjeneste medlemTjenesteMock = mock(MedlemTjeneste.class);
        var personDtoTjeneste = new PersonopplysningDtoTjeneste(personopplysningTjenesteMock, repositoryProvider);
        LocalDate endringFraDato = LocalDate.now().minusDays(5);
        var endringsresultatPersonopplysningerForMedlemskap = EndringsresultatPersonopplysningerForMedlemskap.builder()
                .leggTilEndring(EndringsresultatPersonopplysningerForMedlemskap.EndretAttributt.Adresse,
                        DatoIntervallEntitet.fraOgMed(endringFraDato), "", "2")
                .build();
        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any())).thenReturn(endringsresultatPersonopplysningerForMedlemskap);

        MedlemDtoTjeneste dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, skjæringstidspunktTjeneste,
            medlemTjenesteMock, personopplysningTjenesteMock, personDtoTjeneste);

        var medlemDtoOpt = dtoTjeneste.lagMedlemV2Dto(behandling.getId());
        assertThat(medlemDtoOpt.get().getFom()).isEqualTo(endringFraDato);
    }

    @Test
    public void skal_lage_inntekt_for_ektefelle() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());
        String navn = "Lisa gikk til skolen";
        String annenPart = "Tripp, tripp, tripp, det sa";
        AktørId aktørIdSøker = AktørId.dummy();
        AktørId aktørIdAnnenPart = AktørId.dummy();
        scenario.medBruker(aktørIdSøker);

        scenario.leggTilMedlemskapPeriode(new MedlemskapPerioderBuilder().medMedlemskapType(MedlemskapType.ENDELIG).build());

        PersonInformasjon personInformasjon = scenario.opprettBuilderForRegisteropplysninger()
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

        Behandling behandling = scenario.lagMocked();
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
                new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

        PersonopplysningTjeneste personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());

        MedlemTjeneste medlemTjenesteMock = mock(MedlemTjeneste.class);
        var personDtoTjeneste = new PersonopplysningDtoTjeneste(personopplysningTjenesteMock, repositoryProvider);

        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any()))
                .thenReturn(EndringsresultatPersonopplysningerForMedlemskap.builder().build());
        MedlemDtoTjeneste dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, skjæringstidspunktTjeneste,
            medlemTjenesteMock, personopplysningTjenesteMock, personDtoTjeneste);

        var medlemDtoOpt = dtoTjeneste.lagMedlemV2Dto(behandling.getId());
        assertThat(medlemDtoOpt).hasValueSatisfying(medlemDto -> {
            assertThat(medlemDto.getMedlemskapPerioder()).hasSize(1);
        });
    }

    @Test
    public void dto_før_registerinnhenting() {
        ScenarioMorSøkerEngangsstønad scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now());

        PersonInformasjon personInformasjon = scenario.opprettBuilderForRegisteropplysninger().build();

        scenario.medRegisterOpplysninger(personInformasjon);

        Behandling behandling = scenario.lagMocked();
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
            new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

        PersonopplysningTjeneste personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());

        MedlemTjeneste medlemTjenesteMock = mock(MedlemTjeneste.class);
        var personDtoTjeneste = new PersonopplysningDtoTjeneste(personopplysningTjenesteMock, repositoryProvider);
        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any()))
            .thenReturn(EndringsresultatPersonopplysningerForMedlemskap.builder().build());
        MedlemDtoTjeneste dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, skjæringstidspunktTjeneste,
            medlemTjenesteMock,
            personopplysningTjenesteMock, personDtoTjeneste);

        var medlemDtoOpt = dtoTjeneste.lagMedlemV2Dto(behandling.getId());
        assertThat(medlemDtoOpt).hasValueSatisfying(medlemDto -> {
            assertThat(medlemDto.getMedlemskapPerioder()).isEmpty();
        });
    }

}
