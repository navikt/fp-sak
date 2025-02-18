package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import static no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder;
import static no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.v2.Personopplysninger.Adresse;
import static no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.v2.Personopplysninger.PersonstatusPeriode;
import static no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.v2.Personopplysninger.Region;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemskapVurderingPeriodeTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.v2.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.v2.Personopplysninger;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;

@CdiDbAwareTest
class MedlemRegelGrunnlagByggerTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Inject
    private MedlemTjeneste medlemTjeneste;
    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;
    @Inject
    private MedlemskapVurderingPeriodeTjeneste medlemskapVurderingPeriodeTjeneste;
    @Inject
    private SatsRepository satsRepo;
    @Inject
    private SkjæringstidspunktTjeneste stpTjeneste;

    @Test
    void bygger_inngangsvilkår_grunnlag() {
        var fødselsdato = LocalDate.of(2024, 10, 15);
        var stp = fødselsdato.minusWeeks(3);
        var registerMedlemskapsperiode = new MedlemskapPerioderBuilder().medPeriode(fødselsdato.minusYears(2), null).build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato)
            .medDefaultFordeling(stp)
            .leggTilMedlemskapPeriode(registerMedlemskapsperiode);
        var personInformasjonBuilder = scenario.opprettBuilderForRegisteropplysninger();
        var adressinfo = Adresseinfo.builder(AdresseType.BOSTEDSADRESSE).medLand(Landkoder.NOR);
        var adresse = new AdressePeriode(Gyldighetsperiode.innenfor(fødselsdato.minusYears(5), Tid.TIDENES_ENDE), adressinfo.build());
        var personstatusFom = fødselsdato.minusYears(10);
        var personstatusTom = Tid.TIDENES_ENDE;
        var statsborgerFom = fødselsdato.minusYears(10);
        var statsborgerTom = fødselsdato;
        var oppholdstillatelseFom = fødselsdato.minusYears(2);
        var oppholdstillatelseTom = fødselsdato.minusWeeks(2);
        var oppgittUtenlandsopphold = new MedlemskapOppgittLandOppholdEntitet.Builder().medPeriode(fødselsdato.minusYears(1),
            fødselsdato.minusMonths(2)).medLand(Landkoder.DEU).build();
        var behandling = scenario.medOppgittTilknytning(new MedlemskapOppgittTilknytningEntitet.Builder().medOppholdNå(true)
                .medOpphold(List.of(oppgittUtenlandsopphold))
                .medOppgittDato(fødselsdato.minusMonths(1)))
            .medRegisterOpplysninger(personInformasjonBuilder.medPersonas()
                .voksenPerson(scenario.getDefaultBrukerAktørId(), SivilstandType.GIFT, NavBrukerKjønn.KVINNE)
                .bostedsadresse(adresse)
                .statsborgerskap(Landkoder.USA, statsborgerFom, statsborgerTom)
                .statsborgerskap(Landkoder.SWE, statsborgerFom, statsborgerTom)
                .personstatus(PersonstatusType.BOSA, personstatusFom, personstatusTom)
                .opphold(OppholdstillatelseType.MIDLERTIDIG, oppholdstillatelseFom, oppholdstillatelseTom)
                .build())
            .lagre(repositoryProvider);

        var yrkesAktivitetPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(fødselsdato.minusYears(1), fødselsdato.minusWeeks(3));
        var inntekt = BigDecimal.valueOf(20);
        var inntektFom = yrkesAktivitetPeriode.getFomDato();
        var inntektTom = yrkesAktivitetPeriode.getTomDato();
        var iayBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var inntektBuilder = InntektBuilder.oppdatere(Optional.empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING)
            .leggTilInntektspost(InntektspostBuilder.ny().medPeriode(inntektFom, inntektTom).medBeløp(inntekt));
        var aktørInntektBuilder = iayBuilder.getAktørInntektBuilder(behandling.getAktørId())
            .leggTilInntekt(inntektBuilder);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(),
            iayBuilder
                .leggTilAktørArbeid(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty())
                    .medAktørId(behandling.getAktørId())
                    .leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                        .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                        .leggTilAktivitetsAvtale(nyAktivitetsAvtaleBuilder().medPeriode(yrkesAktivitetPeriode))))
                .leggTilAktørInntekt(aktørInntektBuilder));

        var regelGrunnlagBygger = new MedlemRegelGrunnlagBygger(medlemTjeneste, personopplysningTjeneste, medlemskapVurderingPeriodeTjeneste,
            inntektArbeidYtelseTjeneste, satsRepo, stpTjeneste, personinfoAdapter);

        var resultat = regelGrunnlagBygger.lagRegelGrunnlag(BehandlingReferanse.fra(behandling));

        assertThat(resultat.personopplysninger().adresser()).hasSize(1);
        var adresse1 = resultat.personopplysninger().adresser().stream().findFirst().orElseThrow();
        assertThat(adresse1.periode().getFomDato()).isEqualTo(adresse.gyldighetsperiode().fom());
        assertThat(adresse1.periode().getTomDato()).isEqualTo(adresse.gyldighetsperiode().tom());
        assertThat(adresse1.erUtenlandsk()).isFalse();
        assertThat(adresse1.type()).isEqualTo(Adresse.Type.BOSTEDSADRESSE);

        assertThat(resultat.personopplysninger().personstatus()).hasSize(1);
        var personstatus1 = resultat.personopplysninger().personstatus().stream().findFirst().orElseThrow();
        assertThat(personstatus1.interval().getFomDato()).isEqualTo(personstatusFom);
        assertThat(personstatus1.interval().getTomDato()).isEqualTo(personstatusTom);
        assertThat(personstatus1.type()).isEqualTo(PersonstatusPeriode.Type.BOSATT_ETTER_FOLKEREGISTERLOVEN);
        assertThat(resultat.personopplysninger().personIdentType()).isEqualTo(Personopplysninger.PersonIdentType.DNR);

        assertThat(resultat.personopplysninger().regioner()).hasSize(1);
        var regionPeriode1 = resultat.personopplysninger().regioner().stream().findFirst().orElseThrow();
        assertThat(regionPeriode1.region()).isEqualTo(Region.NORDEN); //Norden prioriteres
        assertThat(regionPeriode1.periode().getFomDato()).isEqualTo(statsborgerFom);
        assertThat(regionPeriode1.periode().getTomDato()).isEqualTo(statsborgerTom);

        assertThat(resultat.personopplysninger().oppholdstillatelser()).hasSize(1);
        var oppholdstillatelse1 = resultat.personopplysninger().oppholdstillatelser().stream().findFirst().orElseThrow();
        assertThat(oppholdstillatelse1.getFomDato()).isEqualTo(oppholdstillatelseFom);
        assertThat(oppholdstillatelse1.getTomDato()).isEqualTo(oppholdstillatelseTom);

        assertThat(resultat.vurderingsperiodeBosatt()).isNotNull();
        assertThat(resultat.vurderingsperiodeLovligOpphold()).isNotNull();

        assertThat(resultat.søknad().utenlandsopphold()).hasSize(1);
        var utenlandsopphold1 = resultat.søknad().utenlandsopphold().stream().findFirst().orElseThrow();
        assertThat(utenlandsopphold1.getFomDato()).isEqualTo(oppgittUtenlandsopphold.getPeriodeFom());
        assertThat(utenlandsopphold1.getTomDato()).isEqualTo(oppgittUtenlandsopphold.getPeriodeTom());

        assertThat(resultat.registrertMedlemskapPerioder()).hasSize(1);
        var medl2Periode1 = resultat.registrertMedlemskapPerioder().stream().findFirst().orElseThrow();
        assertThat(medl2Periode1.getFomDato()).isEqualTo(registerMedlemskapsperiode.getFom());
        assertThat(medl2Periode1.getTomDato()).isEqualTo(registerMedlemskapsperiode.getTom());

        assertThat(resultat.arbeid().ansettelsePerioder()).hasSize(1);
        var ansettelsePeriode1 = resultat.arbeid().ansettelsePerioder().stream().findFirst().orElseThrow();
        assertThat(ansettelsePeriode1.getFomDato()).isEqualTo(yrkesAktivitetPeriode.getFomDato());
        assertThat(ansettelsePeriode1.getTomDato()).isEqualTo(yrkesAktivitetPeriode.getTomDato());

        assertThat(resultat.arbeid().inntekter()).hasSize(1);
        var inntekt1 = resultat.arbeid().inntekter().stream().findFirst().orElseThrow();
        assertThat(inntekt1.interval().getFomDato()).isEqualTo(inntektFom);
        assertThat(inntekt1.interval().getTomDato()).isEqualTo(inntektTom);
        assertThat(inntekt1.beløp()).isEqualTo(new MedlemskapsvilkårGrunnlag.Beløp(inntekt));

    }

}
