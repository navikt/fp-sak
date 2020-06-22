package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagAndelTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagBeregningsresultatTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagEnAndelTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagToAndelerMotsattRekkefølgeTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagToAndelerTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagUttakResultatPlanTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.RevurderingTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.impl.BehandlingskontrollTjenesteImpl;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.fastsettuttaksgrunnlag.svp.EndringsdatoRevurderingUtlederImpl;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class RevurderingBehandlingsresultatutlederTest {

    public static final String ORGNR = KUNSTIG_ORG;
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.now();

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;
    @Inject
    @FagsakYtelseTypeRef("SVP")
    private RevurderingEndring revurderingEndring;
    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Inject
    private MedlemTjeneste medlemTjeneste;
    @Inject
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    @Inject
    private VergeRepository vergeRepository;

    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    private final YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private RevurderingTjeneste revurderingTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    private SvangerskapspengerUttakResultatRepository uttakRepository = repositoryProvider.getSvangerskapspengerUttakResultatRepository();
    private HentOgLagreBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste = new HentOgLagreBeregningsgrunnlagTjeneste(repoRule.getEntityManager());
    private RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder;
    private boolean erVarselOmRevurderingSendt = true;

    private Behandling behandlingSomSkalRevurderes;
    private Behandling revurdering;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;
    private LocalDate endringsdato = LocalDate.now().minusMonths(3);
    private EndringsdatoRevurderingUtlederImpl endringsdatoRevurderingUtlederImpl = mock(EndringsdatoRevurderingUtlederImpl.class);
    private OpphørUttakTjeneste opphørUttakTjeneste = mock(OpphørUttakTjeneste.class);
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);

    @Before
    public void setUp() {
        ScenarioMorSøkerSvangerskapspenger scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.KONTROLLER_FAKTA);
        behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandlingSomSkalRevurderes, LocalDate.now().minusYears(1), LocalDate.now(), false);
        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);

        revurderingBehandlingsresultatutleder = new RevurderingBehandlingsresultatutleder(repositoryProvider,
            hentBeregningsgrunnlagTjeneste,
            endringsdatoRevurderingUtlederImpl,
            opphørUttakTjeneste,
            skjæringstidspunktTjeneste,
            medlemTjeneste);

        BehandlingskontrollTjenesteImpl behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider
        );
        RevurderingTjenesteFelles revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider);
        revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider, behandlingskontrollTjeneste,
            iayTjeneste, revurderingEndring, revurderingTjenesteFelles, vergeRepository);
        revurdering = revurderingTjeneste
            .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(), BehandlingÅrsakType.RE_HENDELSE_FØDSEL, new OrganisasjonsEnhet("1234", "Test"));
    }

    // Case 1
    // Løpende vedtak: Ja
    // Ikke oppfylt inngangsvilkår på skjæringstidspunktet
    // Ikke oppfylt inngangsvilkår i perioden
    // Endring i uttaksperiode: Ja
    @Test
    public void tilfelle_1_behandlingsresultat_lik_opphør_rettentil_lik_nei_foreldrepenger_opphører() {
        // Arrange
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Endring i uttakperiode (ulik lengde)
        List<LocalDateInterval> opprinneligePerioder = List.of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));
        List<LocalDateInterval> revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            opprinneligePerioder, List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeIkkeOppfyltÅrsak.INGEN)
        );
        lagUttakResultatPlanForBehandling(revurdering,
            revurderingPerioder);

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 2
    // Løpende vedtak: Ja
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Ikkje oppfylt inngangsvilkår i perioden
    // Endring i uttaksperiode: Ja
    @Test
    public void tilfelle_2_behandlingsresultat_lik_opphør_rettentil_lik_nei_foreldrepenger_opphører() {
        // Arrange
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Endring i uttakperiode (ulik lengde)
        List<LocalDateInterval> opprinneligePerioder = List.of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));
        List<LocalDateInterval> revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            opprinneligePerioder
        );
        lagUttakResultatPlanForBehandling(revurdering,
            revurderingPerioder);

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 3
    // Løpende vedtak: Ja
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Oppfylt inngangsvilkår i perioden
    // Siste uttaksperiode avslått med opphørsårsak
    // Endring i uttaksperiode: Ja
    @Test
    public void tilfelle_3_behandlingsresultat_lik_opphør_rettentil_lik_nei_foreldrepenger_opphører() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Endring i uttakperiode (ulik lengde)
        List<LocalDateInterval> opprinneligePerioder = List.of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));
        List<LocalDateInterval> revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak og endring i uttak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes, opprinneligePerioder);

        // Siste periode avslått med opphørsårsak og endring
        lagUttakResultatPlanForBehandling(revurdering,
            revurderingPerioder, List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305)
        );
        lagreEndringsdato(endringsdato);

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 4
    // Løpende vedtak: Nei
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Oppfylt inngangsvilkår i perioden
    // Siste uttaksperiode IKKJE avslått med opphørsårsak
    // Endring i uttaksperiode: Ja
    @Test
    public void tilfelle_4_med_endring_i_uttak_behandlingsresultat_lik_innvilget_rettentil_lik_ja_konsekvens_endring_i_uttak() {
        // Arrange
        LocalDate endringsdato = LocalDate.of(2018, 1, 1);
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);
        lagreEndringsdato(endringsdato);

        // Endring i uttakperiode (ulik lengde)
        List<LocalDateInterval> opprinneligePerioder = List.of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.minusDays(5)));
        List<LocalDateInterval> revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak og endring i uttak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            opprinneligePerioder
        );

        // Siste periode ikkje avslått med opphørsårsak
        lagUttakResultatPlanForBehandling(revurdering,
            revurderingPerioder
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INNVILGET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_UTTAK);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 4
    // Løpende vedtak: Nei
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Oppfylt inngangsvilkår i perioden
    // Siste uttaksperiode IKKJE avslått med opphørsårsak
    // Endring i uttaksperiode: Ja
    // Endring i beregning: Ja
    @Test
    public void tilfelle_4_med_endring_i_uttak_og_beregning_behandlingsresultat_lik_innvilget_rettentil_lik_ja_konsekvens_endring_i_uttak_og_endring_i_beregning() {
        // Arrange
        LocalDate endringsdato = LocalDate.of(2018, 1, 1);
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Endring i uttakperiode (ulik lengde)
        List<LocalDateInterval> opprinneligePerioder = List.of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.minusDays(5)));
        List<LocalDateInterval> revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak og endring i uttak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            opprinneligePerioder
        );

        // Endring i beregning
        List<ÅpenDatoIntervallEntitet> bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, true, false, bgPeriode);

        // Siste periode ikkje avslått med opphørsårsak
        lagUttakResultatPlanForBehandling(revurdering,
            revurderingPerioder
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INNVILGET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_BEREGNING, KonsekvensForYtelsen.ENDRING_I_UTTAK);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 5
    // Løpende vedtak: Ja
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Oppfylt inngangsvilkår i perioden
    // Siste uttaksperiode IKKJE avslått med opphørsårsak
    // Endring i beregning
    // Endring i uttaksperiode: Nei
    @Test
    public void tilfelle_5_behandlingsresultat_lik_FPEndret_rettentil_lik_ja_foreldrepenger_konsekvens_Endring_i_beregning() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        List<LocalDateInterval> opprinneligePerioder = List.of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            opprinneligePerioder
        );

        // Like perioder, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
            opprinneligePerioder
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning
        List<ÅpenDatoIntervallEntitet> bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, true, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_BEREGNING);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 6
    // Løpende vedtak: Ja
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Oppfylt inngangsvilkår i perioden
    // Siste uttaksperiode IKKJE avslått med opphørsårsak
    // Endring i beregning
    // Endring i uttaksperiode: Ja
    @Test
    public void tilfelle_6_behandlingsresultat_lik_FPEndret_rettentil_lik_ja_foreldrepenger_konsekvens_endring_i_beregning_og_uttak() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        List<LocalDateInterval> opprinneligePerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            opprinneligePerioder
        );

        // Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
            opprinneligePerioder, List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeIkkeOppfyltÅrsak.INGEN), List.of(50)
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning
        List<ÅpenDatoIntervallEntitet> bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, true, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_BEREGNING, KonsekvensForYtelsen.ENDRING_I_UTTAK);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 7
    // Løpende vedtak: Ja
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Oppfylt inngangsvilkår i perioden
    // Siste uttaksperiode IKKJE avslått med opphørsårsak
    // Endring i beregning: Nei
    // Endring i uttaksperiode: Ja
    @Test
    public void tilfelle_7_behandlingsresultat_lik_FPEndret_rettentil_lik_ja_foreldrepenger_konsekven_endring_i_uttak() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        List<LocalDateInterval> opprinneligePerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            opprinneligePerioder
        );

        // Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
            opprinneligePerioder, List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeIkkeOppfyltÅrsak.INGEN), List.of(50)
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning
        List<ÅpenDatoIntervallEntitet> bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_UTTAK);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 8
    // Løpende vedtak: Ja
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Oppfylt inngangsvilkår i perioden
    // Siste uttaksperiode IKKJE avslått med opphørsårsak
    // Endring i beregning: kun endring i fordeling av ytelsen
    // Endring i uttaksperiode: Nei
    @Test
    public void tilfelle_8_behandlingsresultat_lik_FPEndret_rettentil_lik_ja_foreldrepenger_konsekven_endring_i_fordeling_av_ytelsen() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        List<LocalDateInterval> opprinneligePerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            opprinneligePerioder
        );

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
            opprinneligePerioder
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning: kun endring i fordeling av ytelsen
        List<ÅpenDatoIntervallEntitet> bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, true, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 9
    // Løpende vedtak: Ja
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Oppfylt inngangsvilkår i perioden
    // Siste uttaksperiode IKKJE avslått med opphørsårsak
    // Endring i beregning: Nei
    // Endring i uttaksperiode: Nei
    @Test
    public void tilfelle_9_behandlingsresultat_lik_ingenEndring_rettentil_lik_ja_foreldrepenger_konsekvens_ingenEndring() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        List<LocalDateInterval> opprinneligePerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            opprinneligePerioder
        );

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
            opprinneligePerioder
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);

        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning: Ingen endring
        List<ÅpenDatoIntervallEntitet> bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(uendretUtfall).isTrue();
    }

    // Case 9
    // Løpende vedtak: Ja
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Oppfylt inngangsvilkår i perioden
    // Siste uttaksperiode IKKJE avslått med opphørsårsak
    // Endring i beregning: Nei (endring i rekkefølge av andeler, men ikkje endring i fordeling)
    // Endring i uttaksperiode: Nei
    @Test
    public void tilfelle_9_ulik_rekkefølge_av_andeler_behandlingsresultat_lik_ingenEndring_rettentil_lik_ja_foreldrepenger_konsekvens_ingenEndring() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        List<LocalDateInterval> opprinneligePerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            opprinneligePerioder
        );

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
            opprinneligePerioder
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning: Ingen endring, kun endring i rekkefølge av andeler
        List<ÅpenDatoIntervallEntitet> bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, true, bgPeriode, new LagToAndelerTjeneste());
        byggBeregningsgrunnlagForBehandling(revurdering, false, true, bgPeriode, new LagToAndelerMotsattRekkefølgeTjeneste());

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(uendretUtfall).isTrue();
    }

    @Test
    public void skal_gi_ingen_vedtaksbrev_når_ingen_endring_og_varsel_om_revurdering_ikke_er_sendt() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        List<LocalDateInterval> opprinneligePerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            opprinneligePerioder, List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeIkkeOppfyltÅrsak.INGEN)
        );

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
            opprinneligePerioder, List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeIkkeOppfyltÅrsak.INGEN)
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning: kun endring i fordeling av ytelsen
        List<ÅpenDatoIntervallEntitet> bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, false);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(bhResultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.INGEN);
        assertThat(uendretUtfall).isTrue();
    }

    @Test
    public void skal_gi_ingen_endring_når_original_revurdering_også_hadde_avslått_siste_uttaksperiode() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Uttaksperiode som brukes for begge behandlinger
        List<LocalDateInterval> revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            revurderingPerioder,
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305)
        );

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering,
            revurderingPerioder,
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305)
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(uendretUtfall).isTrue();
    }

    @Test
    public void skal_gi_opphør_når_det_er_flere_perioder_som_avslås() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Uttaksperiode som brukes for begge behandlinger
        List<LocalDateInterval> revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            revurderingPerioder,
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8304)
        );

        // Alle perioder avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering,
            revurderingPerioder,
            List.of(PeriodeResultatType.AVSLÅTT, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeIkkeOppfyltÅrsak._8305, PeriodeIkkeOppfyltÅrsak._8306)
        );
        lagreEndringsdato(endringsdato);

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
        assertThat(uendretUtfall).isFalse();
    }

    @Test
    public void skal_gi_endring_når_original_revurdering_ikke_har_samme_skalHindreTilbakketrekk() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Uttaksperiode som brukes for begge behandlinger
        List<LocalDateInterval> revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            revurderingPerioder,
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305)
        );

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering,
            revurderingPerioder,
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305)
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        leggTilTilbaketrekk(true, false);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(uendretUtfall).isFalse();
    }

    @Test
    public void skal_gi_endring_når_original_revurdering_mangler_skalHindreTilbakketrekk() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Uttaksperiode som brukes for begge behandlinger
        List<LocalDateInterval> revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            revurderingPerioder,
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305)
        );

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering,
            revurderingPerioder,
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305)
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        leggTilTilbaketrekk(null, false);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(uendretUtfall).isFalse();
    }

    @Test
    public void skal_gi_ingen_endring_når_original_revurdering_har_samme_skalHindreTilbakketrekk() {
        // Arrange
        LocalDate endringsdato = LocalDate.now();
        when(endringsdatoRevurderingUtlederImpl.utledEndringsdato(any())).thenReturn(endringsdato);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Uttaksperiode som brukes for begge behandlinger
        List<LocalDateInterval> revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
            revurderingPerioder,
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305)
        );

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering,
            revurderingPerioder,
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305)
        );

        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        leggTilTilbaketrekk(false, false);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(uendretUtfall).isTrue();
    }

    private Behandlingsresultat bestemBehandlingsresultatForRevurdering(Behandling revurdering, boolean erVarselOmRevurderingSendt) {
        var ref = BehandlingReferanse.fra(revurdering, SKJÆRINGSTIDSPUNKT_BEREGNING);
        return revurderingBehandlingsresultatutleder.bestemBehandlingsresultatForRevurdering(ref, erVarselOmRevurderingSendt);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    private SvangerskapspengerUttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
                                                                                     List<LocalDateInterval> perioder) {
        SvangerskapspengerUttakResultatEntitet uttakresultat = LagUttakResultatPlanTjeneste.lagUttakResultatPlanSVPTjeneste(behandling,
            perioder, Collections.nCopies(perioder.size(), PeriodeResultatType.INNVILGET), Collections.nCopies(perioder.size(), PeriodeIkkeOppfyltÅrsak.INGEN),
            Collections.nCopies(perioder.size(), 100));
        uttakRepository.lagre(behandling.getId(), uttakresultat);
        return uttakresultat;
    }

    private SvangerskapspengerUttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
                                                                                     List<LocalDateInterval> perioder,
                                                                                     List<PeriodeResultatType> periodeResultatTyper,
                                                                                     List<PeriodeIkkeOppfyltÅrsak> periodeResultatÅrsak) {
        SvangerskapspengerUttakResultatEntitet uttakresultat = LagUttakResultatPlanTjeneste.lagUttakResultatPlanSVPTjeneste(behandling,
            perioder, periodeResultatTyper, periodeResultatÅrsak, Collections.nCopies(perioder.size(), 100));
        uttakRepository.lagre(behandling.getId(), uttakresultat);
        return uttakresultat;
    }

    private SvangerskapspengerUttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
                                                                                     List<LocalDateInterval> perioder,
                                                                                     List<PeriodeResultatType> periodeResultatTyper,
                                                                                     List<PeriodeIkkeOppfyltÅrsak> periodeResultatÅrsak,
                                                                                     List<Integer> utbetalingsgrad) {
        SvangerskapspengerUttakResultatEntitet uttakresultat = LagUttakResultatPlanTjeneste.lagUttakResultatPlanSVPTjeneste(behandling,
            perioder, periodeResultatTyper, periodeResultatÅrsak, utbetalingsgrad);
        uttakRepository.lagre(behandling.getId(), uttakresultat);
        return uttakresultat;
    }

    private void lagBeregningsresultatperiodeMedEndringstidspunkt(LocalDate endringsdato) {
        BeregningsresultatEntitet originaltresultat = LagBeregningsresultatTjeneste.lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, true, ORGNR);
        BeregningsresultatEntitet revurderingsresultat = LagBeregningsresultatTjeneste.lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, false, ORGNR);
        beregningsresultatRepository.lagre(revurdering, revurderingsresultat);
        beregningsresultatRepository.lagre(behandlingSomSkalRevurderes, originaltresultat);
    }

    private BeregningsgrunnlagEntitet byggBeregningsgrunnlagForBehandling(Behandling behandling, boolean medOppjustertDagsat, boolean skalDeleAndelMellomArbeidsgiverOgBruker, List<ÅpenDatoIntervallEntitet> perioder) {
        return byggBeregningsgrunnlagForBehandling(behandling, medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker, perioder, new LagEnAndelTjeneste());
    }

    private BeregningsgrunnlagEntitet byggBeregningsgrunnlagForBehandling(Behandling behandling, boolean medOppjustertDagsat, boolean skalDeleAndelMellomArbeidsgiverOgBruker, List<ÅpenDatoIntervallEntitet> perioder, LagAndelTjeneste lagAndelTjeneste) {
        beregningsgrunnlag = LagBeregningsgrunnlagTjeneste.lagBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker, perioder, lagAndelTjeneste);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);
        return beregningsgrunnlag;
    }

    private void lagreEndringsdato(LocalDate endringsdato) {
        AvklarteUttakDatoerEntitet avklarteDatoer = new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(endringsdato).build();
        ytelsesFordelingRepository.lagre(revurdering.getId(), avklarteDatoer);
    }

    private void leggTilTilbaketrekk(Boolean behandlingMedTilbaketrekk, Boolean originalBehandlingMedTilbaketrekk) {
        if (behandlingMedTilbaketrekk != null) {
            beregningsresultatRepository.lagreMedTilbaketrekk(revurdering, behandlingMedTilbaketrekk);
        }
        if (originalBehandlingMedTilbaketrekk != null) {
            beregningsresultatRepository.lagreMedTilbaketrekk(behandlingSomSkalRevurderes, originalBehandlingMedTilbaketrekk);
        }
    }
}
