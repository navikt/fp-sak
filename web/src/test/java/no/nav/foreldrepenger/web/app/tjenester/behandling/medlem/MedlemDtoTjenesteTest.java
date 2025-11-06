package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemskapVurderingPeriodeTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.AvklarMedlemskapUtleder;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemRegelGrunnlagBygger;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;

@CdiDbAwareTest
class MedlemDtoTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private SkjæringstidspunktTjeneste stpTjeneste;
    @Inject
    private MedlemTjeneste medlemTjeneste;
    @Inject
    private PersonopplysningTjeneste poTjeneste;
    @Inject
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    @Inject
    private MedlemskapVurderingPeriodeTjeneste medlemVurderingPeriodeTjeneste;
    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Inject
    private SatsRepository satsRepository;

    @Test
    void skal_lage_medlemskap_dto() {
        var fødselsdato = LocalDate.of(2024, 10, 15);
        var stp = fødselsdato.minusWeeks(3);
        var aktørIdAnnenPart = AktørId.dummy();
        var registerMedlemskapsperiode = new MedlemskapPerioderBuilder().medPeriode(fødselsdato.minusYears(2), null).build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato)
            .medDefaultFordeling(stp)
            .leggTilMedlemskapPeriode(registerMedlemskapsperiode);

        scenario.medSøknadAnnenPart().medAktørId(aktørIdAnnenPart).medNavn("Ola Dunk").build();

        var personInformasjonBuilder = scenario.opprettBuilderForRegisteropplysninger();
        var adresse = new AdressePeriode(Gyldighetsperiode.innenfor(fødselsdato.minusYears(5), null),
            Adresseinfo.builder(AdresseType.BOSTEDSADRESSE).medLand(Landkoder.NOR).build());
        var personstatusFom = fødselsdato.minusYears(10);
        var personstatusTom = Tid.TIDENES_ENDE;
        var statsborgerFom = fødselsdato.minusYears(10);
        var statsborgerTom = fødselsdato.minusMonths(2);
        var oppholdstillatelseFom = fødselsdato.minusYears(2);
        var oppholdstillatelseTom = fødselsdato.minusWeeks(2);
        var oppgittUtenlandsopphold = new MedlemskapOppgittLandOppholdEntitet.Builder().medPeriode(fødselsdato.minusYears(1),
            fødselsdato.minusMonths(2)).erTidligereOpphold(true).medLand(Landkoder.DEU).build();

        var søker = personInformasjonBuilder.medPersonas()
            .voksenPerson(scenario.getDefaultBrukerAktørId(), SivilstandType.GIFT, NavBrukerKjønn.KVINNE)
            .bostedsadresse(adresse)
            .statsborgerskap(Landkoder.USA, statsborgerFom, statsborgerTom)
            .statsborgerskap(Landkoder.SWE, statsborgerFom, statsborgerTom)
            .personstatus(PersonstatusType.BOSA, personstatusFom, personstatusTom)
            .opphold(OppholdstillatelseType.MIDLERTIDIG, oppholdstillatelseFom, oppholdstillatelseTom)
            .build();


        var annenpart = personInformasjonBuilder.medPersonas().voksenPerson(aktørIdAnnenPart, SivilstandType.GIFT, NavBrukerKjønn.MANN)
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

        var medlemDtoTjeneste = dtoTjeneste(behandling.getAktørId());
        var dto = medlemDtoTjeneste.lagMedlemskap(behandling.getUuid()).orElseThrow();

        assertThat(dto.avvik()).containsExactlyInAnyOrder(MedlemskapAvvik.MEDL_PERIODER,
            MedlemskapAvvik.BOSATT_UTENLANDSOPPHOLD);

        assertThat(dto.personstatuser()).hasSize(1);
        var personstatus1 = dto.personstatuser().stream().findFirst().orElseThrow();
        assertThat(personstatus1.fom()).isEqualTo(personstatusFom);
        assertThat(personstatus1.tom()).isEqualTo(personstatusTom);
        assertThat(personstatus1.type()).isEqualTo(PersonstatusType.BOSA);

        assertThat(dto.adresser()).hasSize(1);
        var adresse1 = dto.adresser().stream().findFirst().orElseThrow();
        assertThat(adresse1.fom()).isEqualTo(adresse.gyldighetsperiode().fom());
        assertThat(adresse1.tom()).isEqualTo(adresse.gyldighetsperiode().tom());
        assertThat(adresse1.adresseType()).isEqualTo(AdresseType.BOSTEDSADRESSE);

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

        var utlandsoppholdFør = dto.oppgittUtlandsopphold().utlandsoppholdFør();
        assertThat(utlandsoppholdFør).hasSize(1);
        var utenlandsopphold1 = utlandsoppholdFør.stream().findFirst().orElseThrow();
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
        assertThat(adresseAP1.fom()).isEqualTo(adresse.gyldighetsperiode().fom());
        assertThat(adresseAP1.tom()).isEqualTo(adresse.gyldighetsperiode().tom());
        assertThat(adresseAP1.adresseType()).isEqualTo(AdresseType.BOSTEDSADRESSE);

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

    private MedlemDtoTjeneste dtoTjeneste(AktørId aktørId) {
        var personinfoAdapter = mock(PersoninfoAdapter.class);
        when(personinfoAdapter.hentFnr(aktørId)).thenReturn(Optional.of(new PersonIdent(new FiktiveFnr().nesteKvinneFnr())));
        var utleder = new AvklarMedlemskapUtleder(new MedlemRegelGrunnlagBygger(medlemTjeneste, poTjeneste, medlemVurderingPeriodeTjeneste,
            iayTjeneste, satsRepository, stpTjeneste, personinfoAdapter));
        return new MedlemDtoTjeneste(repositoryProvider, stpTjeneste, medlemTjeneste, poTjeneste, utleder,
            new VilkårResultatRepository(repositoryProvider.getEntityManager()), behandlingProsesseringTjeneste);
    }
}
