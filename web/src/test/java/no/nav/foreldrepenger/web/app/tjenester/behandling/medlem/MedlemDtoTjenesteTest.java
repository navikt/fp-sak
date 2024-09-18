package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonAdresse;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.api.EndringsresultatPersonopplysningerForMedlemskap;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAvvik;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonopplysningDtoTjeneste;
import no.nav.vedtak.konfig.Tid;

@CdiDbAwareTest
class MedlemDtoTjenesteTest {

    @Inject
    private MedlemDtoTjeneste medlemDtoTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void skal_lage_medlem_dto() {
        var navn = "Lisa gikk til skolen";
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        var stp = LocalDate.now();
        scenario.medSøknadHendelse().medFødselsDato(stp);
        var søkerAktørId = AktørId.dummy();
        scenario.medBruker(søkerAktørId);

        var søker = scenario.opprettBuilderForRegisteropplysninger()
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(søkerAktørId).navn(navn))
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

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider);

        var personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
        var medlemTjenesteMock = mock(MedlemTjeneste.class);
        var personDtoTjeneste = new PersonopplysningDtoTjeneste(personopplysningTjenesteMock, repositoryProvider);

        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any(), any())).thenReturn(
            EndringsresultatPersonopplysningerForMedlemskap.builder().build());

        var dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, skjæringstidspunktTjeneste, medlemTjenesteMock, personopplysningTjenesteMock,
            personDtoTjeneste, null, null, null);

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
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(søkerAktørId).navn(navn))
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

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider);

        var personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
        var medlemTjenesteMock = mock(MedlemTjeneste.class);
        var personDtoTjeneste = new PersonopplysningDtoTjeneste(personopplysningTjenesteMock, repositoryProvider);
        var endringFraDato = LocalDate.now().minusDays(5);
        var endringsresultatPersonopplysningerForMedlemskap = EndringsresultatPersonopplysningerForMedlemskap.builder()
            .leggTilEndring(DatoIntervallEntitet.fraOgMed(endringFraDato), "", "2")
            .build();
        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any(), any())).thenReturn(endringsresultatPersonopplysningerForMedlemskap);

        var dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, skjæringstidspunktTjeneste, medlemTjenesteMock, personopplysningTjenesteMock,
            personDtoTjeneste, null, null, null);

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
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(aktørIdSøker).navn(navn))
            .leggTilPersonopplysninger(Personopplysning.builder().aktørId(aktørIdAnnenPart).navn(annenPart))
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

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider);

        var personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());

        var medlemTjenesteMock = mock(MedlemTjeneste.class);
        var personDtoTjeneste = new PersonopplysningDtoTjeneste(personopplysningTjenesteMock, repositoryProvider);

        when(medlemTjenesteMock.søkerHarEndringerIPersonopplysninger(any(), any())).thenReturn(
            EndringsresultatPersonopplysningerForMedlemskap.builder().build());
        var dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, skjæringstidspunktTjeneste, medlemTjenesteMock, personopplysningTjenesteMock,
            personDtoTjeneste, null, null, null);

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

        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider);

        var personopplysningTjenesteMock = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());

        var medlemTjenesteMock = mock(MedlemTjeneste.class);
        var personDtoTjeneste = new PersonopplysningDtoTjeneste(personopplysningTjenesteMock, repositoryProvider);
        var dtoTjeneste = new MedlemDtoTjeneste(repositoryProvider, skjæringstidspunktTjeneste, medlemTjenesteMock, personopplysningTjenesteMock,
            personDtoTjeneste, null, null, null);

        var medlemDtoOpt = dtoTjeneste.lagMedlemV2Dto(behandling.getId());
        assertThat(medlemDtoOpt).hasValueSatisfying(medlemDto -> assertThat(medlemDto.getMedlemskapPerioder()).isEmpty());
    }

    @Test
    void skal_lage_medlemskap_v3() {
        var fødselsdato = LocalDate.of(2024, 10, 15);
        var stp = fødselsdato.minusWeeks(3);
        var aktørIdSøker = AktørId.dummy();
        var aktørIdAnnenPart = AktørId.dummy();
        var registerMedlemskapsperiode = new MedlemskapPerioderBuilder().medPeriode(fødselsdato.minusYears(2), null).build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato)
            .medDefaultFordeling(stp)
            .leggTilMedlemskapPeriode(registerMedlemskapsperiode);

        scenario.medSøknadAnnenPart().medAktørId(aktørIdAnnenPart).medNavn("Ola Dunk").build();

        var personInformasjonBuilder = scenario.opprettBuilderForRegisteropplysninger();
        var adresse = PersonAdresse.builder()
            .adresseType(AdresseType.BOSTEDSADRESSE)
            .periode(fødselsdato.minusYears(5), Tid.TIDENES_ENDE)
            .land(Landkoder.NOR);
        var personstatusFom = fødselsdato.minusYears(10);
        var personstatusTom = Tid.TIDENES_ENDE;
        var statsborgerFom = fødselsdato.minusYears(10);
        var statsborgerTom = fødselsdato.minusMonths(2);
        var oppholdstillatelseFom = fødselsdato.minusYears(2);
        var oppholdstillatelseTom = fødselsdato.minusWeeks(2);
        var oppgittUtenlandsopphold = new MedlemskapOppgittLandOppholdEntitet.Builder().medPeriode(fødselsdato.minusYears(1),
            fødselsdato.minusMonths(2)).medLand(Landkoder.DEU).build();

       var søker = personInformasjonBuilder.medPersonas()
           .voksenPerson(scenario.getDefaultBrukerAktørId(), SivilstandType.GIFT, NavBrukerKjønn.KVINNE)
           .bostedsadresse(adresse)
           .statsborgerskap(Landkoder.USA, statsborgerFom, statsborgerTom)
           .statsborgerskap(Landkoder.SWE, statsborgerFom, statsborgerTom)
           .personstatus(PersonstatusType.BOSA, personstatusFom, personstatusTom)
           .opphold(OppholdstillatelseType.MIDLERTIDIG, oppholdstillatelseFom, oppholdstillatelseTom)
           .build();


        var annenpart = personInformasjonBuilder.medPersonas()
            .voksenPerson(aktørIdAnnenPart, SivilstandType.GIFT, NavBrukerKjønn.MANN)
            .bostedsadresse(adresse)
            .statsborgerskap(Landkoder.GEO, statsborgerFom, statsborgerTom)
            .statsborgerskap(Landkoder.CAN, statsborgerFom, statsborgerTom)
            .personstatus(PersonstatusType.BOSA, personstatusFom, personstatusTom)
            .build();


        var behandling = scenario.medOppgittTilknytning(new MedlemskapOppgittTilknytningEntitet.Builder().medOppholdNå(true)
                .medOpphold(List.of(oppgittUtenlandsopphold))
                .medOppgittDato(fødselsdato.minusMonths(1)))
            .medRegisterOpplysninger(annenpart)
            .medRegisterOpplysninger(søker)
            .leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR)
            .lagre(repositoryProvider);

        var dto = medlemDtoTjeneste.lagMedlemskap(behandling.getUuid());

        assertThat(dto.avvik()).containsExactlyInAnyOrder(MedlemskapAvvik.MEDL_PERIODER,
            MedlemskapAvvik.BOSATT_UTENLANDSOPPHOLD);

        assertThat(dto.personstatuser()).hasSize(1);
        var personstatus1 = dto.personstatuser().stream().findFirst().orElseThrow();
        assertThat(personstatus1.fom()).isEqualTo(personstatusFom);
        assertThat(personstatus1.tom()).isEqualTo(personstatusTom);
        assertThat(personstatus1.type()).isEqualTo(PersonstatusType.BOSA);

        assertThat(dto.adresser()).hasSize(1);
        var adresse1 = dto.adresser().stream().findFirst().orElseThrow();
        assertThat(adresse1.fom()).isEqualTo(adresse.getPeriode().getFomDato());
        assertThat(adresse1.tom()).isEqualTo(adresse.getPeriode().getTomDato());
        assertThat(adresse1.adresse().getAdresseType()).isEqualTo(AdresseType.BOSTEDSADRESSE);

        assertThat(dto.regioner()).hasSize(1);
        var regionPeriode1 = dto.regioner().stream().findFirst().orElseThrow();
        assertThat(regionPeriode1.type()).isEqualTo(Region.NORDEN); //Norden prioriteres
        assertThat(regionPeriode1.fom()).isEqualTo(statsborgerFom);
        assertThat(regionPeriode1.tom()).isEqualTo(statsborgerTom);

        assertThat(dto.oppholdstillatelser()).hasSize(1);
        var oppholdstillatelse1 = dto.oppholdstillatelser().stream().findFirst().orElseThrow();
        assertThat(oppholdstillatelse1.fom()).isEqualTo(oppholdstillatelseFom);
        assertThat(oppholdstillatelse1.tom()).isEqualTo(oppholdstillatelseTom);
        assertThat(oppholdstillatelse1.type()).isEqualTo(OppholdstillatelseType.MIDLERTIDIG);

        assertThat(dto.utenlandsopphold()).hasSize(1);
        var utenlandsopphold1 = dto.utenlandsopphold().stream().findFirst().orElseThrow();
        assertThat(utenlandsopphold1.fom()).isEqualTo(oppgittUtenlandsopphold.getPeriodeFom());
        assertThat(utenlandsopphold1.tom()).isEqualTo(oppgittUtenlandsopphold.getPeriodeTom());
        assertThat(utenlandsopphold1.landkode()).isEqualTo(Landkoder.DEU);

        assertThat(dto.medlemskapsperioder()).hasSize(1);
        var medl2Periode1 = dto.medlemskapsperioder().stream().findFirst().orElseThrow();
        assertThat(medl2Periode1.fom()).isEqualTo(registerMedlemskapsperiode.getFom());
        assertThat(medl2Periode1.tom()).isEqualTo(registerMedlemskapsperiode.getTom());
        assertThat(medl2Periode1.dekningType()).isEqualTo(registerMedlemskapsperiode.getDekningType());
        assertThat(medl2Periode1.beslutningsdato()).isEqualTo(registerMedlemskapsperiode.getBeslutningsdato());

        assertThat(dto.annenpart().adresser()).hasSize(1);
        var adresseAP1 = dto.adresser().stream().findFirst().orElseThrow();
        assertThat(adresseAP1.fom()).isEqualTo(adresse.getPeriode().getFomDato());
        assertThat(adresseAP1.tom()).isEqualTo(adresse.getPeriode().getTomDato());
        assertThat(adresseAP1.adresse().getAdresseType()).isEqualTo(AdresseType.BOSTEDSADRESSE);

        assertThat(dto.annenpart().regioner()).hasSize(1);
        var regionPeriodeAP1 = dto.annenpart().regioner().stream().findFirst().orElseThrow();
        assertThat(regionPeriodeAP1.type()).isEqualTo(Region.TREDJELANDS_BORGER);
        assertThat(regionPeriodeAP1.fom()).isEqualTo(statsborgerFom);
        assertThat(regionPeriodeAP1.tom()).isEqualTo(statsborgerTom);
        assertThat(dto.annenpart().personstatuser()).hasSize(1);

        assertThat(dto.annenpart().personstatuser()).hasSize(1);
        var personstatusAP1 = dto.annenpart().personstatuser().stream().findFirst().orElseThrow();
        assertThat(personstatusAP1.fom()).isEqualTo(personstatusFom);
        assertThat(personstatusAP1.tom()).isEqualTo(personstatusTom);
        assertThat(personstatusAP1.type()).isEqualTo(PersonstatusType.BOSA);
    }
}
