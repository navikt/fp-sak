package no.nav.foreldrepenger.inngangsvilkaar.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.YtelseMaksdatoTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioFarSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.Kjoenn;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.SoekerRolle;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.AdopsjonsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel.FødselsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.RegisterInnhentingIntervall;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class InngangsvilkårOversetterTest {
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private InngangsvilkårOversetter oversetter;

    @Inject
    private BasisPersonopplysningTjeneste personopplysningTjeneste;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();

    private YrkesaktivitetBuilder yrkesaktivitetBuilder;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider,
        new RegisterInnhentingIntervall(Period.of(1, 0, 0), Period.of(0, 6, 0)));

    @Before
    public void oppsett() {
        oversetter = new InngangsvilkårOversetter(repositoryProvider, personopplysningTjeneste,
            new YtelseMaksdatoTjeneste(repositoryProvider, new RelatertBehandlingTjeneste(repositoryProvider)),
            iayTjeneste,
            null);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_mappe_fra_domenefødsel_til_regelfødsel() {
        LocalDate now = LocalDate.now();
        LocalDate søknadsdato = now;
        LocalDate fødselFødselsdato = now.plusDays(7);
        Behandling behandling = opprettBehandlingForFødsel(now, søknadsdato, fødselFødselsdato, RelasjonsRolleType.MORA);

        FødselsvilkårGrunnlag grunnlag = oversetter.oversettTilRegelModellFødsel(lagRef(behandling));

        // Assert
        assertThat(grunnlag.getSoekersKjonn()).isEqualTo(Kjoenn.KVINNE);
        assertThat(grunnlag.getBekreftetFoedselsdato()).isEqualTo(fødselFødselsdato);
        assertThat(grunnlag.getAntallBarn()).isEqualTo(1);
        assertThat(grunnlag.getBekreftetTermindato()).isNull();
        assertThat(grunnlag.getSoekerRolle()).isEqualTo(SoekerRolle.MORA);
        assertThat(grunnlag.getDagensdato()).isEqualTo(søknadsdato);
        assertThat(grunnlag.isErSøktOmTermin()).isFalse();
    }

    @Test
    public void skal_mappe_fra_domenefødsel_til_regelfødsel_dersom_søker_er_medmor() {
        LocalDate now = LocalDate.now();
        LocalDate søknadsdato = now;
        LocalDate fødselFødselsdato = now.plusDays(7);
        Behandling behandling = opprettBehandlingForFødsel(now, søknadsdato, fødselFødselsdato, RelasjonsRolleType.FARA);

        FødselsvilkårGrunnlag grunnlag = oversetter.oversettTilRegelModellFødsel(lagRef(behandling));

        // Assert
        assertThat(grunnlag.getSoekersKjonn()).isEqualTo(Kjoenn.KVINNE); // snodig, men søker er kvinne her med rolle FARA
        assertThat(grunnlag.getBekreftetFoedselsdato()).isEqualTo(fødselFødselsdato);
        assertThat(grunnlag.getBekreftetTermindato()).isNull();
        assertThat(grunnlag.getSoekerRolle()).isEqualTo(SoekerRolle.FARA);
        assertThat(grunnlag.getDagensdato()).isEqualTo(søknadsdato);
        assertThat(grunnlag.isErSøktOmTermin()).isFalse();
    }

    private Behandling opprettBehandlingForFødsel(LocalDate now, LocalDate søknadsdato, LocalDate fødselFødselsdato,
                                                  RelasjonsRolleType rolle) {
        // Arrange
        LocalDate søknadFødselsdato = now.plusDays(2);

        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();

        scenario.medSøknad()
            .medSøknadsdato(søknadsdato);

        scenario.medSøknadHendelse().medFødselsDato(søknadFødselsdato);

        scenario.medBekreftetHendelse()
            // Fødsel
            .leggTilBarn(fødselFødselsdato)
            .medAntallBarn(1);

        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId barnAktørId = AktørId.dummy();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        PersonInformasjon fødtBarn = builderForRegisteropplysninger
            .medPersonas()
            .fødtBarn(barnAktørId, LocalDate.now().plusDays(7))
            .relasjonTil(søkerAktørId, rolle, null)
            .build();

        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT, Region.NORDEN)
            .statsborgerskap(Landkoder.NOR)
            .relasjonTil(barnAktørId, RelasjonsRolleType.BARN, null)
            .build();
        scenario.medRegisterOpplysninger(søker);
        scenario.medRegisterOpplysninger(fødtBarn);

        return lagre(scenario);
    }

    @Test
    public void skal_mappe_fra_domeneadoosjon_til_regeladopsjon() {
        // Arrange
        LocalDate søknadsdato = LocalDate.now().plusDays(1);
        LocalDate søknadFødselsdato = LocalDate.now().plusDays(2);
        LocalDate fødselAdopsjonsdatoFraSøknad = LocalDate.now().plusDays(8);
        Map<Integer, LocalDate> map = new HashMap<>();
        map.put(1, fødselAdopsjonsdatoFraSøknad);

        var scenario = ScenarioFarSøkerEngangsstønad.forAdopsjon();
        scenario.medSøknad()
            .medSøknadsdato(søknadsdato)
            .build();
        scenario.medSøknadHendelse().medFødselsDato(søknadFødselsdato);

        scenario.medBekreftetHendelse().medAdopsjon(
            scenario.medBekreftetHendelse().getAdopsjonBuilder()
                .medErEktefellesBarn(true)
                .medAdoptererAlene(true)
                .medOmsorgsovertakelseDato(fødselAdopsjonsdatoFraSøknad))
            .leggTilBarn(fødselAdopsjonsdatoFraSøknad)
            // Adosjon
            .build();

        PersonInformasjon søker = scenario.opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .mann(scenario.getDefaultBrukerAktørId(), SivilstandType.UOPPGITT, Region.NORDEN)
            .statsborgerskap(Landkoder.NOR)
            .build();
        scenario.medRegisterOpplysninger(søker);

        Behandling behandling = lagre(scenario);

        AdopsjonsvilkårGrunnlag grunnlag = oversetter.oversettTilRegelModellAdopsjon(lagRef(behandling));

        // Assert
        assertThat(grunnlag.getSoekersKjonn()).isEqualTo(Kjoenn.MANN);
        assertThat(grunnlag.getBekreftetAdopsjonBarn().get(0).getFoedselsdato()).isEqualTo(map.get(1));
        assertThat(grunnlag.isEktefellesBarn()).isTrue();
        assertThat(grunnlag.isMannAdoptererAlene()).isTrue();
        assertThat(grunnlag.getOmsorgsovertakelsesdato()).isEqualTo(fødselAdopsjonsdatoFraSøknad);
    }

    @Test
    public void skal_mappe_fra_domenemedlemskap_til_regelmedlemskap() {
        // Arrange

        LocalDate skjæringstidspunkt = LocalDate.now();

        var scenario = oppsett(skjæringstidspunkt);
        Behandling behandling = lagre(scenario);

        opprettArbeidOgInntektForBehandling(behandling, skjæringstidspunkt.minusMonths(5), skjæringstidspunkt.plusMonths(4), true);

        VurdertMedlemskap vurdertMedlemskap = new VurdertMedlemskapBuilder()
            .medMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType.MEDLEM)
            .medBosattVurdering(true)
            .medLovligOppholdVurdering(true)
            .medOppholdsrettVurdering(true)
            .build();
        MedlemskapRepository medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        medlemskapRepository.lagreMedlemskapVurdering(behandling.getId(), vurdertMedlemskap);

        // Act
        MedlemskapsvilkårGrunnlag grunnlag = oversetter.oversettTilRegelModellMedlemskap(lagRef(behandling));

        // Assert
        assertTrue(grunnlag.isBrukerAvklartBosatt());
        assertTrue(grunnlag.isBrukerAvklartLovligOppholdINorge());
        assertTrue(grunnlag.isBrukerAvklartOppholdsrett());
        assertTrue(grunnlag.isBrukerAvklartPliktigEllerFrivillig());
        assertTrue(grunnlag.isBrukerNorskNordisk());
        assertFalse(grunnlag.isBrukerBorgerAvEUEOS());
        assertTrue(grunnlag.harSøkerArbeidsforholdOgInntekt());
    }

    @Test
    public void skal_mappe_fra_domenemedlemskap_til_regelmedlemskap_med_ingen_relevant_arbeid_og_inntekt() {

        // Arrange
        LocalDate skjæringstidspunkt = LocalDate.now();
        var scenario = oppsett(skjæringstidspunkt);
        Behandling behandling = lagre(scenario);
        opprettArbeidOgInntektForBehandling(behandling, skjæringstidspunkt.minusMonths(5), skjæringstidspunkt.minusDays(1), true);

        // Act
        MedlemskapsvilkårGrunnlag grunnlag = oversetter.oversettTilRegelModellMedlemskap(lagRef(behandling));

        // Assert
        assertFalse(grunnlag.harSøkerArbeidsforholdOgInntekt());
    }

    @Test
    public void skal_mappe_fra_domenemedlemskap_til_regelmedlemskap_med_relevant_arbeid_og_ingen_pensjonsgivende_inntekt() {

        // Arrange
        LocalDate skjæringstidspunkt = LocalDate.now();
        var scenario = oppsett(skjæringstidspunkt);
        Behandling behandling = lagre(scenario);
        opprettArbeidOgInntektForBehandling(behandling, skjæringstidspunkt.minusMonths(5), skjæringstidspunkt.plusDays(10), false);

        // Act
        MedlemskapsvilkårGrunnlag grunnlag = oversetter.oversettTilRegelModellMedlemskap(lagRef(behandling));

        // Assert
        assertFalse(grunnlag.harSøkerArbeidsforholdOgInntekt());
    }

    private AbstractTestScenario<?> oppsett(LocalDate skjæringstidspunkt) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medDefaultOppgittTilknytning();
        scenario.medSøknadHendelse().medFødselsDato(skjæringstidspunkt);
        scenario.medSøknad()
            .medMottattDato(LocalDate.of(2017, 3, 15));

        PersonInformasjon søker = scenario.opprettBuilderForRegisteropplysninger()
            .medPersonas()
            .kvinne(scenario.getDefaultBrukerAktørId(), SivilstandType.GIFT, Region.NORDEN)
            .personstatus(PersonstatusType.BOSA)
            .statsborgerskap(Landkoder.NOR)
            .build();
        scenario.medRegisterOpplysninger(søker);
        return scenario;
    }

    private void opprettArbeidOgInntektForBehandling(Behandling behandling, LocalDate fom, LocalDate tom,
                                                                                   boolean harPensjonsgivendeInntekt) {

        String orgnr = "42";

        var aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        AktørId aktørId = behandling.getAktørId();
        lagAktørArbeid(aggregatBuilder, aktørId, orgnr, fom, tom, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, Optional.empty());
        for (LocalDate dt = fom; dt.isBefore(tom); dt = dt.plusMonths(1)) {
            lagInntekt(aggregatBuilder, aktørId, orgnr, dt, dt.plusMonths(1), harPensjonsgivendeInntekt);
        }

        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);
    }

    private AktørArbeid lagAktørArbeid(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
                                       LocalDate fom, LocalDate tom, ArbeidType arbeidType, Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
            .getAktørArbeidBuilder(aktørId);

        Opptjeningsnøkkel opptjeningsnøkkel;
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(virksomhetOrgnr);
        if (arbeidsforholdRef.isPresent()) {
            opptjeningsnøkkel = new Opptjeningsnøkkel(arbeidsforholdRef.get(), arbeidsgiver.getIdentifikator(), null);
        } else {
            opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);
        }

        yrkesaktivitetBuilder = aktørArbeidBuilder
            .getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel, arbeidType);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));

        yrkesaktivitetBuilder.leggTilAktivitetsAvtale(aktivitetsAvtale)
            .medArbeidType(arbeidType)
            .medArbeidsgiver(arbeidsgiver);

        yrkesaktivitetBuilder.medArbeidsforholdId(arbeidsforholdRef.orElse(null));

        aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeidBuilder);
        return aktørArbeidBuilder.build();
    }

    private void lagInntekt(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder, AktørId aktørId, String virksomhetOrgnr,
                            LocalDate fom, LocalDate tom, boolean harPensjonsgivendeInntekt) {
        var opptjeningsnøkkel = Opptjeningsnøkkel.forOrgnummer(virksomhetOrgnr);

        var aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);

        Stream<InntektsKilde> inntektsKildeStream;
        if (harPensjonsgivendeInntekt) {
            inntektsKildeStream = Stream.of(InntektsKilde.INNTEKT_BEREGNING, InntektsKilde.INNTEKT_SAMMENLIGNING, InntektsKilde.INNTEKT_OPPTJENING);
        } else {
            inntektsKildeStream = Stream.of(InntektsKilde.INNTEKT_BEREGNING, InntektsKilde.INNTEKT_SAMMENLIGNING);
        }

        inntektsKildeStream.forEach(kilde -> {
            InntektBuilder inntektBuilder = aktørInntektBuilder.getInntektBuilder(kilde, opptjeningsnøkkel);
            InntektspostBuilder inntektspost = InntektspostBuilder.ny()
                .medBeløp(BigDecimal.valueOf(35000))
                .medPeriode(fom, tom)
                .medInntektspostType(InntektspostType.LØNN);
            inntektBuilder.leggTilInntektspost(inntektspost).medArbeidsgiver(yrkesaktivitetBuilder.build().getArbeidsgiver());
            aktørInntektBuilder.leggTilInntekt(inntektBuilder);
            inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntektBuilder);
        });
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
    }

}
