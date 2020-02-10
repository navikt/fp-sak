package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SVANGERSKAPSPENGERVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.StartpunktRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapKildeType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.util.FPDateUtil;

@RunWith(CdiRunner.class)
public class KontrollerFaktaStegImplTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private Behandling behandling;
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final SvangerskapspengerRepository svangerskapspengerRepository = repositoryProvider.getSvangerskapspengerRepository();

    @Inject
    @BehandlingTypeRef
    @FagsakYtelseTypeRef("SVP")
    @StartpunktRef
    private KontrollerFaktaStegImpl steg;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    private AktørId aktørId = AktørId.dummy();
    private LocalDate jordmorsdato = LocalDate.now().minusDays(30);


    @Before
    public void oppsett() {
        ScenarioMorSøkerSvangerskapspenger scenario = byggBehandlingMedMorSøkerSVP();
        scenario.medBruker(aktørId, NavBrukerKjønn.KVINNE);
        MedlemskapPerioderBuilder builder = new MedlemskapPerioderBuilder();
        builder.medPeriode(LocalDate.now().minusMonths(2), LocalDate.now().plusDays(2))
            .medDekningType(MedlemskapDekningType.UNNTATT)
            .medKildeType(MedlemskapKildeType.TPS);

        scenario.leggTilMedlemskapPeriode(builder.build());
        behandling =lagre(scenario);
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), OppgittOpptjeningBuilder.ny());
        lagreSvp(behandling, jordmorsdato);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_utlede_inngangsvilkår_for_svp() {
        Fagsak fagsak = behandling.getFagsak();
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling.getId());
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(), lås);

        // Act
        steg.utførSteg(kontekst);
        behandlingRepository.lagre(behandling, lås);

        // Assert
        List<VilkårType> resulat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()).getVilkårResultat().getVilkårene()
            .stream()
            .map(Vilkår::getVilkårType)
            .collect(Collectors.toList());

        assertThat(resulat)
            .containsExactlyInAnyOrder(MEDLEMSKAPSVILKÅRET, SØKERSOPPLYSNINGSPLIKT, OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET,
                BEREGNINGSGRUNNLAGVILKÅR, SVANGERSKAPSPENGERVILKÅR);
    }

    private ScenarioMorSøkerSvangerskapspenger byggBehandlingMedMorSøkerSVP() {
        ScenarioMorSøkerSvangerskapspenger scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.medBruker(aktørId, NavBrukerKjønn.KVINNE);
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario);
        return scenario;
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario) {
        PersonInformasjon.Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, NavBrukerKjønn.KVINNE, Region.UDEFINERT)
            .build();
        scenario.medRegisterOpplysninger(søker);
    }

    private void lagreSvp(Behandling behandling, LocalDate jordmorsdato) {
        SvpTilretteleggingEntitet tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(jordmorsdato)
            .medIngenTilrettelegging(jordmorsdato)
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
            .medMottattTidspunkt(FPDateUtil.nå())
            .medKopiertFraTidligereBehandling(false)
            .build();
        SvpGrunnlagEntitet svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }
}
