package no.nav.foreldrepenger.behandling.revurdering.ytelse.svp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
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
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
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
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatRepository;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@CdiDbAwareTest
class RevurderingBehandlingsresultatutlederTest {

    public static final String ORGNR = KUNSTIG_ORG;
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.now();

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;
    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
    private RevurderingEndring revurderingEndring;
    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Inject
    private MedlemTjeneste medlemTjeneste;
    @Inject
    private VergeRepository vergeRepository;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider;
    @Inject
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    @Inject
    private UttakTjeneste uttakTjeneste;
    @Inject
    private BehandlingRepository behandlingRepository;
    private RevurderingTjeneste revurderingTjeneste;
    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;
    @Inject
    private SvangerskapspengerUttakResultatRepository uttakRepository;
    @Inject
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
    @Inject
    private DekningsgradTjeneste dekningsgradTjeneste;
    @Mock
    private BeregningTjeneste beregningTjeneste;
    private RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutleder;
    private final boolean erVarselOmRevurderingSendt = true;

    private Behandling behandlingSomSkalRevurderes;
    private Behandling revurdering;
    private final LocalDate endringsdato = LocalDate.now().minusMonths(3);

    @BeforeEach
    void setUp() {
        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        scenario.medBehandlingVedtak()
                .medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SJEKK_TERMINBEKREFTELSE,
                BehandlingStegType.KONTROLLER_FAKTA);
        behandlingSomSkalRevurderes = scenario.lagre(repositoryProvider);
        repositoryProvider.getOpptjeningRepository()
                .lagreOpptjeningsperiode(behandlingSomSkalRevurderes, LocalDate.now().minusYears(1), LocalDate.now(),
                        false);
        revurderingTestUtil.avsluttBehandling(behandlingSomSkalRevurderes);

        revurderingBehandlingsresultatutleder = new RevurderingBehandlingsresultatutlederFelles(repositoryProvider,
                grunnlagRepositoryProvider,
                beregningTjeneste,
                medlemTjeneste, dekningsgradTjeneste, uttakTjeneste);

        var behandlingskontrollTjeneste = new BehandlingskontrollTjenesteImpl(serviceProvider);
        var revurderingTjenesteFelles = new RevurderingTjenesteFelles(repositoryProvider, behandlingRevurderingTjeneste, behandlingskontrollTjeneste);
        revurderingTjeneste = new RevurderingTjenesteImpl(repositoryProvider, grunnlagRepositoryProvider, iayTjeneste, revurderingEndring, revurderingTjenesteFelles, vergeRepository);
        revurdering = revurderingTjeneste
                .opprettAutomatiskRevurdering(behandlingSomSkalRevurderes.getFagsak(),
                        BehandlingÅrsakType.RE_HENDELSE_FØDSEL, new OrganisasjonsEnhet("1234", "Test"));
    }

    // Case 1
    // Løpende vedtak: Ja
    // Ikke oppfylt inngangsvilkår på skjæringstidspunktet
    // Ikke oppfylt inngangsvilkår i perioden
    // Endring i uttaksperiode: Ja
    @Test
    void tilfelle_1_behandlingsresultat_lik_opphør_rettentil_lik_nei_foreldrepenger_opphører() {
        // Arrange
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Endring i uttakperiode (ulik lengde)
        var opprinneligePerioder = List.of(
                new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));
        var revurderingPerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                opprinneligePerioder, List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeIkkeOppfyltÅrsak.INGEN));
        lagUttakResultatPlanForBehandling(revurdering,
                revurderingPerioder);

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårAvslått(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallMerknad.VM_1035)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);


        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(
                KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 2
    // Løpende vedtak: Ja
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Ikkje oppfylt inngangsvilkår i perioden
    // Endring i uttaksperiode: Ja
    @Test
    void tilfelle_2_behandlingsresultat_lik_opphør_rettentil_lik_nei_foreldrepenger_opphører() {
        // Arrange
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Endring i uttakperiode (ulik lengde)
        var opprinneligePerioder = List.of(
                new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));
        var revurderingPerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                opprinneligePerioder);
        lagUttakResultatPlanForBehandling(revurdering,
                revurderingPerioder);

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårAvslått(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallMerknad.VM_1035)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårAvslått(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallMerknad.VM_1025)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);


        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(
                KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 3
    // Løpende vedtak: Ja
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Oppfylt inngangsvilkår i perioden
    // Siste uttaksperiode avslått med opphørsårsak
    // Endring i uttaksperiode: Ja
    @Test
    void tilfelle_3_behandlingsresultat_lik_opphør_rettentil_lik_nei_foreldrepenger_opphører() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Endring i uttakperiode (ulik lengde)
        var opprinneligePerioder = List.of(
                new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));
        var revurderingPerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak og endring i uttak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes, opprinneligePerioder);

        // Siste periode avslått med opphørsårsak og endring
        lagUttakResultatPlanForBehandling(revurdering,
                revurderingPerioder, List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305));
        lagreEndringsdato(endringsdato);

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(
                KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 4
    // Løpende vedtak: Nei
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Oppfylt inngangsvilkår i perioden
    // Siste uttaksperiode IKKJE avslått med opphørsårsak
    // Endring i uttaksperiode: Ja
    @Test
    void tilfelle_4_med_endring_i_uttak_behandlingsresultat_lik_innvilget_rettentil_lik_ja_konsekvens_endring_i_uttak() {
        // Arrange
        var endringsdato = LocalDate.of(2018, 1, 1);
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);
        lagreEndringsdato(endringsdato);

        // Endring i uttakperiode (ulik lengde)
        var opprinneligePerioder = List.of(
                new LocalDateInterval(endringsdato.minusDays(10), endringsdato.minusDays(5)));
        var revurderingPerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak og endring i uttak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                opprinneligePerioder);

        // Siste periode ikkje avslått med opphørsårsak
        lagUttakResultatPlanForBehandling(revurdering,
                revurderingPerioder);

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_UTTAK);
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
    void tilfelle_5_behandlingsresultat_lik_FPEndret_rettentil_lik_ja_foreldrepenger_konsekvens_Endring_i_beregning() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        var opprinneligePerioder = List.of(
                new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                opprinneligePerioder);

        // Like perioder, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
                opprinneligePerioder);

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning
        var bgPeriode = List.of(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, true, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(
                BehandlingResultatType.FORELDREPENGER_ENDRET);
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
    void tilfelle_6_behandlingsresultat_lik_FPEndret_rettentil_lik_ja_foreldrepenger_konsekvens_endring_i_beregning_og_uttak() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        var opprinneligePerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                opprinneligePerioder);

        // Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
                opprinneligePerioder, List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeIkkeOppfyltÅrsak.INGEN),
                List.of(50));

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning
        var bgPeriode = List.of(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, true, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(
                BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_BEREGNING,
                KonsekvensForYtelsen.ENDRING_I_UTTAK);
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
    void tilfelle_7_behandlingsresultat_lik_FPEndret_rettentil_lik_ja_foreldrepenger_konsekven_endring_i_uttak() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        var opprinneligePerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                opprinneligePerioder);

        // Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
                opprinneligePerioder, List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeIkkeOppfyltÅrsak.INGEN),
                List.of(50));

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning
        var bgPeriode = List.of(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(
                BehandlingResultatType.FORELDREPENGER_ENDRET);
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
    void tilfelle_8_behandlingsresultat_lik_FPEndret_rettentil_lik_ja_foreldrepenger_konsekven_endring_i_fordeling_av_ytelsen() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        var opprinneligePerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                opprinneligePerioder);

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
                opprinneligePerioder);

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning: kun endring i fordeling av ytelsen
        var bgPeriode = List.of(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, true, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(
                BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(
                KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
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
    void tilfelle_9_behandlingsresultat_lik_ingenEndring_rettentil_lik_ja_foreldrepenger_konsekvens_ingenEndring() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        var opprinneligePerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                opprinneligePerioder);

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
                opprinneligePerioder);

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);

        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning: Ingen endring
        var bgPeriode = List.of(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

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
    // Endring i beregning: Nei (endring i rekkefølge av andeler, men ikkje endring
    // i fordeling)
    // Endring i uttaksperiode: Nei
    @Test
    void tilfelle_9_ulik_rekkefølge_av_andeler_behandlingsresultat_lik_ingenEndring_rettentil_lik_ja_foreldrepenger_konsekvens_ingenEndring() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        var opprinneligePerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                opprinneligePerioder);

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
                opprinneligePerioder);

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning: Ingen endring, kun endring i rekkefølge av andeler
        var bgPeriode = List.of(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, true, bgPeriode,
                new LagToAndelerTjeneste());
        byggBeregningsgrunnlagForBehandling(revurdering, false, true, bgPeriode,
                new LagToAndelerMotsattRekkefølgeTjeneste());

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(uendretUtfall).isTrue();
    }

    @Test
    void skal_gi_ingen_vedtaksbrev_når_ingen_endring_og_varsel_om_revurdering_ikke_er_sendt() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        var opprinneligePerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                opprinneligePerioder, List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeIkkeOppfyltÅrsak.INGEN));

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering,
                opprinneligePerioder, List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeIkkeOppfyltÅrsak.INGEN));

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Endring i beregning: kun endring i fordeling av ytelsen
        var bgPeriode = List.of(
                ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(behandlingSomSkalRevurderes, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, false);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(bhResultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.INGEN);
        assertThat(uendretUtfall).isTrue();
    }

    @Test
    void skal_gi_ingen_endring_når_original_revurdering_også_hadde_avslått_siste_uttaksperiode() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Uttaksperiode som brukes for begge behandlinger
        var revurderingPerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                revurderingPerioder,
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305));

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering,
                revurderingPerioder,
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305));

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(uendretUtfall).isTrue();
    }

    @Test
    void skal_gi_opphør_når_det_er_flere_perioder_som_avslås() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Uttaksperiode som brukes for begge behandlinger
        var revurderingPerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                revurderingPerioder,
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8304));

        // Alle perioder avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering,
                revurderingPerioder,
                List.of(PeriodeResultatType.AVSLÅTT, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeIkkeOppfyltÅrsak._8305, PeriodeIkkeOppfyltÅrsak._8306));
        lagreEndringsdato(endringsdato);

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(
                KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
        assertThat(uendretUtfall).isFalse();
    }

    @Test
    void skal_gi_endring_når_original_revurdering_ikke_har_samme_skalHindreTilbakketrekk() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Uttaksperiode som brukes for begge behandlinger
        var revurderingPerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                revurderingPerioder,
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305));

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering,
                revurderingPerioder,
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305));

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        leggTilTilbaketrekk(true, false);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(
                BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(
                KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(uendretUtfall).isFalse();
    }

    @Test
    void skal_gi_endring_når_original_revurdering_mangler_skalHindreTilbakketrekk() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Uttaksperiode som brukes for begge behandlinger
        var revurderingPerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                revurderingPerioder,
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305));

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering,
                revurderingPerioder,
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305));

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        leggTilTilbaketrekk(null, false);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(
                BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(
                KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(uendretUtfall).isFalse();
    }

    @Test
    void skal_gi_ingen_endring_når_original_revurdering_har_samme_skalHindreTilbakketrekk() {
        // Arrange
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato);

        // Uttaksperiode som brukes for begge behandlinger
        var revurderingPerioder = List.of(
                new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(behandlingSomSkalRevurderes,
                revurderingPerioder,
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305));

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering,
                revurderingPerioder,
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeIkkeOppfyltÅrsak.INGEN, PeriodeIkkeOppfyltÅrsak._8305));

        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
                .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
                .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
                .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
                .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        leggTilTilbaketrekk(false, false);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(uendretUtfall).isTrue();
    }

    private void bestemBehandlingsresultatForRevurdering(Behandling revurdering,
                                                         boolean erVarselOmRevurderingSendt) {
        var ref = BehandlingReferanse.fra(revurdering);
        revurderingBehandlingsresultatutleder.bestemBehandlingsresultatForRevurdering(ref, erVarselOmRevurderingSendt);
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    private void lagUttakResultatPlanForBehandling(Behandling behandling,
                                                   List<LocalDateInterval> perioder) {
        var uttakresultat = LagUttakResultatPlanTjeneste.lagUttakResultatPlanSVPTjeneste(
                behandling,
                perioder, Collections.nCopies(perioder.size(), PeriodeResultatType.INNVILGET),
                Collections.nCopies(perioder.size(), PeriodeIkkeOppfyltÅrsak.INGEN),
                Collections.nCopies(perioder.size(), 100));
        uttakRepository.lagre(behandling.getId(), uttakresultat);
    }

    private SvangerskapspengerUttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
            List<LocalDateInterval> perioder,
            List<PeriodeResultatType> periodeResultatTyper,
            List<PeriodeIkkeOppfyltÅrsak> periodeResultatÅrsak) {
        var uttakresultat = LagUttakResultatPlanTjeneste.lagUttakResultatPlanSVPTjeneste(
                behandling,
                perioder, periodeResultatTyper, periodeResultatÅrsak, Collections.nCopies(perioder.size(), 100));
        uttakRepository.lagre(behandling.getId(), uttakresultat);
        return uttakresultat;
    }

    private SvangerskapspengerUttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
            List<LocalDateInterval> perioder,
            List<PeriodeResultatType> periodeResultatTyper,
            List<PeriodeIkkeOppfyltÅrsak> periodeResultatÅrsak,
            List<Integer> utbetalingsgrad) {
        var uttakresultat = LagUttakResultatPlanTjeneste.lagUttakResultatPlanSVPTjeneste(
                behandling,
                perioder, periodeResultatTyper, periodeResultatÅrsak, utbetalingsgrad);
        uttakRepository.lagre(behandling.getId(), uttakresultat);
        return uttakresultat;
    }

    private void lagBeregningsresultatperiodeMedEndringstidspunkt(LocalDate endringsdato) {
        var originaltresultat = LagBeregningsresultatTjeneste.lagBeregningsresultatperiodeMedEndringstidspunkt(
                endringsdato, true, ORGNR);
        var revurderingsresultat = LagBeregningsresultatTjeneste.lagBeregningsresultatperiodeMedEndringstidspunkt(
                endringsdato, false, ORGNR);
        beregningsresultatRepository.lagre(revurdering, revurderingsresultat);
        beregningsresultatRepository.lagre(behandlingSomSkalRevurderes, originaltresultat);
    }

    private void byggBeregningsgrunnlagForBehandling(Behandling behandling,
            boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker,
            List<ÅpenDatoIntervallEntitet> perioder) {
        byggBeregningsgrunnlagForBehandling(behandling, medOppjustertDagsat,
                skalDeleAndelMellomArbeidsgiverOgBruker, perioder, new LagEnAndelTjeneste());
    }

    private void byggBeregningsgrunnlagForBehandling(Behandling behandling,
            boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker,
            List<ÅpenDatoIntervallEntitet> perioder,
            LagAndelTjeneste lagAndelTjeneste) {
        var bg = LagBeregningsgrunnlagTjeneste.lagBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING,
                medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker, perioder, lagAndelTjeneste);
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).build(BeregningsgrunnlagTilstand.FASTSATT);
        when(beregningTjeneste.hent(BehandlingReferanse.fra(behandling))).thenReturn(Optional.of(gr));
    }

    private void lagreEndringsdato(LocalDate endringsdato) {
        var avklarteDatoer = new AvklarteUttakDatoerEntitet.Builder()
            .medOpprinneligEndringsdato(endringsdato)
            .build();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(revurdering.getId())
            .medAvklarteDatoer(avklarteDatoer)
            .build();
        ytelsesFordelingRepository.lagre(revurdering.getId(), ytelseFordelingAggregat);
    }

    private void leggTilTilbaketrekk(Boolean behandlingMedTilbaketrekk, Boolean originalBehandlingMedTilbaketrekk) {
        if (behandlingMedTilbaketrekk != null) {
            beregningsresultatRepository.lagreMedTilbaketrekk(revurdering, behandlingMedTilbaketrekk);
        }
        if (originalBehandlingMedTilbaketrekk != null) {
            beregningsresultatRepository.lagreMedTilbaketrekk(behandlingSomSkalRevurderes,
                    originalBehandlingMedTilbaketrekk);
        }
    }
}
