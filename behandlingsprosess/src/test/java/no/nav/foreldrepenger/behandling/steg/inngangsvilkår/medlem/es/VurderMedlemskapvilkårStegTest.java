package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlem.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlemskap.es.Medlemsvilkårutleder;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.medlemskap.es.VurderMedlemskapvilkårStegImpl;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Gyldighetsperiode;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemskapVurderingPeriodeTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.RegelOrkestrerer;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.InngangsvilkårMedlemskap;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.InngangsvilkårMedlemskapForutgående;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.AvklarMedlemskapUtleder;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemRegelGrunnlagBygger;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.BotidCore2024;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@CdiDbAwareTest
class VurderMedlemskapvilkårStegTest {
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

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

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void forutgående_medlem_oppfylt() {

        // Arrange
        var scenario = lagTestScenarioMedlem(LocalDate.now().plus(Period.parse("P18W3D")), false);

        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, VilkårUtfallType.IKKE_VURDERT);

        var behandling = lagre(scenario);
        var kontekst = new BehandlingskontrollKontekst(behandling,
            repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        // Act - vurder vilkåret
        var vilkårutleder = new Medlemsvilkårutleder(repositoryProvider,
            new BotidCore2024(null, null));

        var inngangsvilkårFellesTjeneste = forutgåendeTjeneste();

        new VurderMedlemskapvilkårStegImpl(repositoryProvider, inngangsvilkårFellesTjeneste, vilkårutleder).utførSteg(kontekst);

        var vilkårResultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()).getVilkårResultat();
        assertThat(vilkårResultat.getVilkårTyper()).contains(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE);
        assertThat(vilkårResultat.getVilkårTyper()).doesNotContain(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårResultat.hentAlleGjeldendeVilkårsutfall()).containsOnlyOnce(VilkårUtfallType.OPPFYLT);

    }

    @Test
    void forutgående_medlem_ikke_oppfylt() {

        // Arrange
        var scenario = lagTestScenarioMedlem(LocalDate.now().plus(Period.parse("P18W3D")), true);

        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, VilkårUtfallType.IKKE_VURDERT);

        var behandling = lagre(scenario);
        var kontekst = new BehandlingskontrollKontekst(behandling,
            repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        // Act - vurder vilkåret
        var vilkårutleder = new Medlemsvilkårutleder(repositoryProvider,
            new BotidCore2024(null, null));

        var inngangsvilkårFellesTjeneste = forutgåendeTjeneste();

        new VurderMedlemskapvilkårStegImpl(repositoryProvider, inngangsvilkårFellesTjeneste, vilkårutleder).utførSteg(kontekst);

        var vilkårResultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()).getVilkårResultat();
        assertThat(vilkårResultat.getVilkårTyper()).contains(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE);
        assertThat(vilkårResultat.getVilkårTyper()).doesNotContain(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårResultat.hentAlleGjeldendeVilkårsutfall()).containsOnlyOnce(VilkårUtfallType.IKKE_OPPFYLT);

    }

    @Test
    void skal_endre_til_forutgående_medlem_oppfylt() {

        // Arrange
        var scenario = lagTestScenarioMedlem(LocalDate.now().plus(Period.parse("P18W3D")), false);

        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_VURDERT);

        var behandling = lagre(scenario);
        var kontekst = new BehandlingskontrollKontekst(behandling,
                repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        // Act - vurder vilkåret
        var vilkårutleder = new Medlemsvilkårutleder(repositoryProvider,
            new BotidCore2024(null, null));

        var inngangsvilkårFellesTjeneste = forutgåendeTjeneste();

        new VurderMedlemskapvilkårStegImpl(repositoryProvider, inngangsvilkårFellesTjeneste, vilkårutleder).utførSteg(kontekst);

        var vilkårResultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()).getVilkårResultat();
        assertThat(vilkårResultat.getVilkårTyper()).contains(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE);
        assertThat(vilkårResultat.getVilkårTyper()).doesNotContain(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårResultat.hentAlleGjeldendeVilkårsutfall()).containsOnlyOnce(VilkårUtfallType.OPPFYLT);

    }

    private InngangsvilkårFellesTjeneste forutgåendeTjeneste() {
        var medlemRegelGrunnlagBygger = new MedlemRegelGrunnlagBygger(medlemTjeneste, personopplysningTjeneste, medlemskapVurderingPeriodeTjeneste,
            inntektArbeidYtelseTjeneste, satsRepo, stpTjeneste, personinfoAdapter);
        when(personinfoAdapter.hentFnr(any())).thenReturn(Optional.of(new PersonIdent(new FiktiveFnr().nesteKvinneFnr())));
        var inngangsvilkårMedlemskap = new InngangsvilkårMedlemskapForutgående(new AvklarMedlemskapUtleder(medlemRegelGrunnlagBygger));
        var inngangsvilkårFellesTjeneste = new InngangsvilkårFellesTjeneste(new RegelOrkestrerer(new InngangsvilkårTjeneste(
            new UnitTestLookupInstanceImpl<>(inngangsvilkårMedlemskap), repositoryProvider)));
        return inngangsvilkårFellesTjeneste;
    }

    @Test
    void klassisk_medlem_oppfylt() {

        // Arrange
        var ikraftredelse = LocalDate.of(2024, 9, 30);
        var scenario = lagTestScenarioMedlem(ikraftredelse, false);

        scenario.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_VURDERT);

        var behandling = lagre(scenario);
        var kontekst = new BehandlingskontrollKontekst(behandling,
            repositoryProvider.getBehandlingLåsRepository().taLås(behandling.getId()));

        // Act - vurder vilkåret
        var vilkårutleder = new Medlemsvilkårutleder(repositoryProvider,
            new BotidCore2024(null, null));

        var medlemRegelGrunnlagBygger = new MedlemRegelGrunnlagBygger(medlemTjeneste, personopplysningTjeneste, medlemskapVurderingPeriodeTjeneste,
            inntektArbeidYtelseTjeneste, satsRepo, stpTjeneste, personinfoAdapter);
        when(personinfoAdapter.hentFnr(any())).thenReturn(Optional.of(new PersonIdent(new FiktiveFnr().nesteKvinneFnr())));
        var inngangsvilkårMedlemskap = new InngangsvilkårMedlemskap(new AvklarMedlemskapUtleder(medlemRegelGrunnlagBygger));
        var inngangsvilkårFellesTjeneste = new InngangsvilkårFellesTjeneste(new RegelOrkestrerer(new InngangsvilkårTjeneste(
            new UnitTestLookupInstanceImpl<>(inngangsvilkårMedlemskap), repositoryProvider)));

        new VurderMedlemskapvilkårStegImpl(repositoryProvider, inngangsvilkårFellesTjeneste, vilkårutleder).utførSteg(kontekst);

        var vilkårResultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()).getVilkårResultat();
        assertThat(vilkårResultat.getVilkårTyper()).contains(VilkårType.MEDLEMSKAPSVILKÅRET);
        assertThat(vilkårResultat.getVilkårTyper()).doesNotContain(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE);
        assertThat(vilkårResultat.hentAlleGjeldendeVilkårsutfall()).containsOnlyOnce(VilkårUtfallType.OPPFYLT);

    }

    private ScenarioMorSøkerEngangsstønad lagTestScenarioMedlem(LocalDate termindato, boolean medlperiode) {
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse()
            .medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato.plusDays(40))
                .medNavnPå("navn navnesen")
                .medUtstedtDato(termindato.minusWeeks(2)));
        scenario.medOverstyrtHendelse()
            .medTerminbekreftelse(scenario.medOverstyrtHendelse().getTerminbekreftelseBuilder()
                .medTermindato(termindato.plusDays(40))
                .medNavnPå("navn navnesen")
                .medUtstedtDato(termindato.minusWeeks(2)));
        if (medlperiode) {
            scenario.leggTilMedlemskapPeriode(new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FTL_2_7_A)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(termindato.minusMonths(1), termindato.plusMonths(1))
                .build());
        }

        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var søker = builderForRegisteropplysninger.medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT)
            .personstatus(PersonstatusType.BOSA)
            .statsborgerskap(Landkoder.NOR)
            .adresse(AdresseType.BOSTEDSADRESSE, new AdressePeriode(Gyldighetsperiode.innenfor(termindato.minusYears(2), termindato.plusYears(2)),
                Adresseinfo.builder(AdresseType.BOSTEDSADRESSE).medLand(Landkoder.NOR).build()))
            .build();
        scenario.medRegisterOpplysninger(søker);
        return scenario;
    }

}
