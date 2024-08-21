package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonAdresse;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.Tid;

@CdiDbAwareTest
class MedlemskapsvilkårRegelGrunnlagByggerTest {

    @Inject
    private MedlemskapsvilkårRegelGrunnlagBygger regelGrunnlagBygger;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    @Inject
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    @Test
    void bygger_grunnlag() {
        var fødselsdato = LocalDate.of(2024, 10, 15);
        var stp = fødselsdato.minusWeeks(3);
        var registerMedlemskapsperiode = new MedlemskapPerioderBuilder().medPeriode(fødselsdato.minusYears(2), null).build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFødselAdopsjonsdato(fødselsdato)
            .medDefaultFordeling(stp)
            .leggTilMedlemskapPeriode(registerMedlemskapsperiode);
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
        var oppgittUtenlandsopphold = new MedlemskapOppgittLandOppholdEntitet.Builder().medPeriode(fødselsdato.minusYears(1), fødselsdato.minusMonths(2))
            .medLand(Landkoder.DEU)
            .build();
        var behandling = scenario.medOppgittTilknytning(new MedlemskapOppgittTilknytningEntitet.Builder().medOppholdNå(true)
                .medOpphold(List.of(oppgittUtenlandsopphold)).medOppgittDato(fødselsdato.minusMonths(1)))
            .medRegisterOpplysninger(personInformasjonBuilder.medPersonas()
                .voksenPerson(scenario.getDefaultBrukerAktørId(), SivilstandType.GIFT, NavBrukerKjønn.KVINNE)
                .bostedsadresse(adresse)
                .statsborgerskap(Landkoder.USA, statsborgerFom, statsborgerTom)
                .statsborgerskap(Landkoder.SWE, statsborgerFom, statsborgerTom)
                .personstatus(PersonstatusType.BOSA, personstatusFom, personstatusTom)
                .opphold(OppholdstillatelseType.MIDLERTIDIG, oppholdstillatelseFom, oppholdstillatelseTom)
                .build())
            .lagre(repositoryProvider);
        var resultat = regelGrunnlagBygger.lagRegelGrunnlagInngangsvilkår(BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId())));

        assertThat(resultat.personopplysninger().adresser()).hasSize(1);
        var adresse1 = resultat.personopplysninger().adresser().stream().findFirst().orElseThrow();
        assertThat(adresse1.periode().getFomDato()).isEqualTo(adresse.getPeriode().getFomDato());
        assertThat(adresse1.periode().getTomDato()).isEqualTo(adresse.getPeriode().getTomDato());
        assertThat(adresse1.erUtenlandsk()).isFalse();
        assertThat(adresse1.type()).isEqualTo(MedlemskapsvilkårRegelGrunnlag.Adresse.Type.BOSTEDSADRESSE);

        assertThat(resultat.personopplysninger().personstatus()).hasSize(1);
        var personstatus1 = resultat.personopplysninger().personstatus().stream().findFirst().orElseThrow();
        assertThat(personstatus1.interval().getFomDato()).isEqualTo(personstatusFom);
        assertThat(personstatus1.interval().getTomDato()).isEqualTo(personstatusTom);
        assertThat(personstatus1.type()).isEqualTo(MedlemskapsvilkårRegelGrunnlag.Personopplysninger.PersonstatusPeriode.Type.BOSATT_ETTER_FOLKEREGISTERLOVEN);


        assertThat(resultat.personopplysninger().regioner()).hasSize(1);
        var regionPeriode1 = resultat.personopplysninger().regioner().stream().findFirst().orElseThrow();
        assertThat(regionPeriode1.region()).isEqualTo(MedlemskapsvilkårRegelGrunnlag.Personopplysninger.Region.NORDEN); //Norden prioriteres
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

        assertThat(resultat.registrertMedlemskapBeslutning()).hasSize(1);
        var medl2Periode1 = resultat.registrertMedlemskapBeslutning().stream().findFirst().orElseThrow();
        assertThat(medl2Periode1.interval().getFomDato()).isEqualTo(registerMedlemskapsperiode.getFom());
        assertThat(medl2Periode1.interval().getTomDato()).isEqualTo(registerMedlemskapsperiode.getTom());

    }

}
