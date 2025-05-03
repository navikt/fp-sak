package no.nav.foreldrepenger.behandling.steg.avklarfakta.svp;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSPERIODEVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OPPTJENINGSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SVANGERSKAPSPENGERVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKERSOPPLYSNINGSPLIKT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapKildeType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;

@CdiDbAwareTest
class KontrollerFaktaStegImplTest {

    private Behandling behandling;
    private final BehandlingRepositoryProvider repositoryProvider;
    private final BehandlingRepository behandlingRepository;
    private final SvangerskapspengerRepository svangerskapspengerRepository;

    @Inject
    @BehandlingTypeRef
    @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
    private KontrollerFaktaStegImpl steg;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    private AktørId aktørId = AktørId.dummy();
    private LocalDate jordmorsdato = LocalDate.now().minusDays(30);

    public KontrollerFaktaStegImplTest(EntityManager em) {
        repositoryProvider = new BehandlingRepositoryProvider(em);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        svangerskapspengerRepository = new SvangerskapspengerRepository(em);

    }

    @BeforeEach
    public void oppsett() {
        var scenario = byggBehandlingMedMorSøkerSVP();
        scenario.medBruker(aktørId, NavBrukerKjønn.KVINNE);
        var builder = new MedlemskapPerioderBuilder();
        builder.medPeriode(LocalDate.now().minusMonths(2), LocalDate.now().plusDays(2))
                .medDekningType(MedlemskapDekningType.UNNTATT)
                .medKildeType(MedlemskapKildeType.TPS);

        scenario.leggTilMedlemskapPeriode(builder.build());
        behandling = lagre(scenario);
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), OppgittOpptjeningBuilder.ny());
        lagreSvp(behandling, jordmorsdato);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    void skal_utlede_inngangsvilkår_for_svp() {
        var lås = behandlingRepository.taSkriveLås(behandling.getId());
        var kontekst = new BehandlingskontrollKontekst(behandling, lås);

        // Act
        steg.utførSteg(kontekst);
        behandlingRepository.lagre(behandling, lås);

        // Assert
        var resulat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId()).getVilkårResultat().getVilkårene()
                .stream()
                .map(Vilkår::getVilkårType)
                .toList();

        assertThat(resulat)
                .containsExactlyInAnyOrder(MEDLEMSKAPSVILKÅRET, SØKERSOPPLYSNINGSPLIKT, OPPTJENINGSPERIODEVILKÅR, OPPTJENINGSVILKÅRET,
                        BEREGNINGSGRUNNLAGVILKÅR, SVANGERSKAPSPENGERVILKÅR);
    }

    private ScenarioMorSøkerSvangerskapspenger byggBehandlingMedMorSøkerSVP() {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.medBruker(aktørId, NavBrukerKjønn.KVINNE);
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario);
        return scenario;
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario) {
        var builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        var søkerAktørId = scenario.getDefaultBrukerAktørId();
        var søker = builderForRegisteropplysninger
                .medPersonas()
                .voksenPerson(søkerAktørId, SivilstandType.UOPPGITT, NavBrukerKjønn.KVINNE)
                .build();
        scenario.medRegisterOpplysninger(søker);
    }

    private void lagreSvp(Behandling behandling, LocalDate jordmorsdato) {
        var tilrettelegging = new SvpTilretteleggingEntitet.Builder()
                .medBehovForTilretteleggingFom(jordmorsdato)
                .medIngenTilrettelegging(jordmorsdato, jordmorsdato, SvpTilretteleggingFomKilde.SØKNAD)
                .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
                .medArbeidsgiver(Arbeidsgiver.person(AktørId.dummy()))
                .medMottattTidspunkt(LocalDateTime.now())
                .medKopiertFraTidligereBehandling(false)
                .build();
        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
                .medBehandlingId(behandling.getId())
                .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
                .build();
        svangerskapspengerRepository.lagreOgFlush(svpGrunnlag);
    }
}
