package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil.ORGNR;
import static no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil.SKJÆRINGSTIDSPUNKT_BEREGNING;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.FASTSATT;
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
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagAndelTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagBeregningsresultatTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagEnAndelTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagToAndelerMotsattRekkefølgeTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagToAndelerTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagUttakResultatPlanTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.RevurderingBehandlingsresultatutlederFelles;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
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
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@CdiDbAwareTest
class RevurderingBehandlingsresultatutlederTest {

    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;
    @Inject
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private MedlemTjeneste medlemTjeneste;
    @Inject
    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
    private RevurderingTjeneste revurderingTjeneste;
    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;
    @Inject
    private FpUttakRepository fpUttakRepository;
    @Inject
    private OpptjeningRepository opptjeningRepository;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider;
    @Inject
    private UttakTjeneste uttakTjeneste;
    @Inject
    private DekningsgradTjeneste dekningsgradTjeneste;
    @Mock
    private BeregningTjeneste beregningTjeneste;

    private RevurderingBehandlingsresultatutlederFelles revurderingBehandlingsresultatutleder;
    private final boolean erVarselOmRevurderingSendt = true;
    private final LocalDate endringsdato = LocalDate.now().minusMonths(3);

    @BeforeEach
    void setup() {
        revurderingBehandlingsresultatutleder = new RevurderingBehandlingsresultatutlederFelles(repositoryProvider, grunnlagRepositoryProvider,
            beregningTjeneste, medlemTjeneste, dekningsgradTjeneste, uttakTjeneste);

    }

    private Behandling opprettRevurdering(Behandling førstegangsbehandling) {
        return revurderingTjeneste.opprettAutomatiskRevurdering(førstegangsbehandling.getFagsak(), BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
            new OrganisasjonsEnhet("1234", "Test"));
    }

    private Behandling opprettFørstegangsbehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medDefaultSøknadTerminbekreftelse()
            .medDefaultFordeling(endringsdato)
            .medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(endringsdato).build());
        scenario.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now()).medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.SJEKK_TERMINBEKREFTELSE, BehandlingStegType.KONTROLLER_FAKTA);
        var førstegangsbehandling = scenario.lagre(repositoryProvider);
        opptjeningRepository.lagreOpptjeningsperiode(førstegangsbehandling, LocalDate.now().minusYears(1), LocalDate.now(), false);
        revurderingTestUtil.avsluttBehandling(førstegangsbehandling);
        return førstegangsbehandling;
    }

    // Case 1
    // Løpende vedtak: Ja
    // Ikke oppfylt inngangsvilkår på skjæringstidspunktet
    // Ikke oppfylt inngangsvilkår i perioden
    // Endring i uttaksperiode: Ja
    @Test
    void tilfelle_1_behandlingsresultat_lik_opphør_rettentil_lik_nei_foreldrepenger_opphører() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Endring i uttakperiode (ulik lengde)
        var opprinneligePerioder = List.of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));
        var revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder, UttakPeriodeType.FEDREKVOTE);

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårAvslått(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallMerknad.VM_1035)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårAvslått(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallMerknad.VM_1025)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Endring i uttakperiode (ulik lengde)
        var opprinneligePerioder = List.of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));
        var revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder, UttakPeriodeType.FEDREKVOTE);

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårAvslått(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallMerknad.VM_1020)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
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
    void tilfelle_3_behandlingsresultat_lik_opphør_rettentil_lik_nei_foreldrepenger_opphører() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Endring i uttakperiode (ulik lengde)
        var opprinneligePerioder = List.of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));
        var revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak og endring i uttak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        // Siste periode avslått med opphørsårsak og endring
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder, Collections.nCopies(revurderingPerioder.size(), false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.BARNET_ER_DØD), Collections.nCopies(revurderingPerioder.size(), true),
            List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));
        lagreEndringsdato(endringsdato, revurdering.getId());

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
        assertThat(uendretUtfall).isFalse();
    }

    // Case 4
    // Løpende vedtak: Nei, men skal håndtere endring tilbake i tid. Behandlingsresulat skal i disse tilfellen settes til Endret
    // Oppfylt inngangsvilkår på skjæringstidspunktet
    // Oppfylt inngangsvilkår i perioden
    // Siste uttaksperiode IKKJE avslått med opphørsårsak
    // Endring i uttaksperiode: Ja
    @Test
    void tilfelle_4_med_endring_i_uttak_behandlingsresultat_lik_innvilget_rettentil_lik_ja_konsekvens_endring_i_uttak() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.of(2018, 1, 1);
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);
        lagreEndringsdato(endringsdato, revurdering.getId());

        // Endring i uttakperiode (ulik lengde)
        var opprinneligePerioder = List.of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.minusDays(5)));
        var revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak og endring i uttak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        // Siste periode ikkje avslått med opphørsårsak
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder, Collections.nCopies(revurderingPerioder.size(), false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.UKJENT),
            Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_UTTAK);
        assertThat(uendretUtfall).isFalse();
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        var opprinneligePerioder = List.of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        // Like perioder, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning
        var bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, true, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        var opprinneligePerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        // Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(50), List.of(100), List.of(new Trekkdager(10)),
            List.of(UttakPeriodeType.FELLESPERIODE));

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning
        var bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, true, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        var opprinneligePerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        // Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(50), List.of(100), List.of(new Trekkdager(10)),
            List.of(UttakPeriodeType.FELLESPERIODE));

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning
        var bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        var opprinneligePerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning: kun endring i fordeling av ytelsen
        var bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, true, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
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
    void tilfelle_9_behandlingsresultat_lik_ingenEndring_rettentil_lik_ja_foreldrepenger_konsekvens_ingenEndring() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        var opprinneligePerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning: Ingen endring
        var bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        var opprinneligePerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning: Ingen endring, kun endring i rekkefølge av andeler
        var bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, true, bgPeriode, new LagToAndelerTjeneste());
        byggBeregningsgrunnlagForBehandling(revurdering, false, true, bgPeriode, new LagToAndelerMotsattRekkefølgeTjeneste());

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(uendretUtfall).isTrue();
    }

    @Test
    void skal_gi_ingen_vedtaksbrev_når_ingen_endring_og_varsel_om_revurdering_ikke_er_sendt() {
        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        var opprinneligePerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false), List.of(PeriodeResultatType.INNVILGET),
            List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100), List.of(100), List.of(new Trekkdager(12)),
            List.of(UttakPeriodeType.FORELDREPENGER));

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning: kun endring i fordeling av ytelsen
        var bgPeriode = List.of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, false);

        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(bhResultat.getVedtaksbrev()).isEqualTo(Vedtaksbrev.INGEN);
        assertThat(uendretUtfall).isTrue();
    }

    @Test
    void skal_gi_ingen_endring_når_original_revurdering_også_hadde_avslått_siste_uttaksperiode() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Uttaksperiode som brukes for begge behandlinger
        var revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(førstegangsbehandling, revurderingPerioder, Collections.nCopies(revurderingPerioder.size(), false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.BARNET_ER_DØD), Collections.nCopies(revurderingPerioder.size(), true),
            List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder, Collections.nCopies(revurderingPerioder.size(), false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.BARNET_ER_DØD), Collections.nCopies(revurderingPerioder.size(), true),
            List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(uendretUtfall).isTrue();
    }

    @Test
    void skal_gi_opphør_når_det_er_flere_perioder_som_avslås() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Uttaksperiode som brukes for begge behandlinger
        var revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(førstegangsbehandling, revurderingPerioder, Collections.nCopies(revurderingPerioder.size(), false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.BARNET_ER_DØD), Collections.nCopies(revurderingPerioder.size(), true),
            List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Alle perioder avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder, Collections.nCopies(revurderingPerioder.size(), false),
            List.of(PeriodeResultatType.AVSLÅTT, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeResultatÅrsak.BARNET_ER_DØD, PeriodeResultatÅrsak.BARNET_ER_DØD), Collections.nCopies(revurderingPerioder.size(), true),
            List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));
        lagreEndringsdato(endringsdato, revurdering.getId());

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
        assertThat(uendretUtfall).isFalse();
    }

    @Test
    void skal_gi_endring_når_original_revurdering_ikke_har_samme_skalHindreTilbakketrekk() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Uttaksperiode som brukes for begge behandlinger
        var revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(førstegangsbehandling, revurderingPerioder, Collections.nCopies(revurderingPerioder.size(), false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.BARNET_ER_DØD), Collections.nCopies(revurderingPerioder.size(), true),
            List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder, Collections.nCopies(revurderingPerioder.size(), false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.BARNET_ER_DØD), Collections.nCopies(revurderingPerioder.size(), true),
            List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        leggTilTilbaketrekk(true, false, førstegangsbehandling, revurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(uendretUtfall).isFalse();
    }

    private void bestemBehandlingsresultatForRevurdering(Behandling revurdering, boolean erVarselOmRevurderingSendt) {
        var ref = BehandlingReferanse.fra(revurdering);
        revurderingBehandlingsresultatutleder.bestemBehandlingsresultatForRevurdering(ref, erVarselOmRevurderingSendt);
    }

    @Test
    void skal_gi_endring_når_original_revurdering_mangler_skalHindreTilbakketrekk() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Uttaksperiode som brukes for begge behandlinger
        var revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(førstegangsbehandling, revurderingPerioder, Collections.nCopies(revurderingPerioder.size(), false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.BARNET_ER_DØD), Collections.nCopies(revurderingPerioder.size(), true),
            List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder, Collections.nCopies(revurderingPerioder.size(), false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.BARNET_ER_DØD), Collections.nCopies(revurderingPerioder.size(), true),
            List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        leggTilTilbaketrekk(null, false, førstegangsbehandling, revurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(uendretUtfall).isFalse();
    }

    @Test
    void skal_gi_ingen_endring_når_original_revurdering_har_samme_skalHindreTilbakketrekk() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Uttaksperiode som brukes for begge behandlinger
        var revurderingPerioder = List.of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
            new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(førstegangsbehandling, revurderingPerioder, Collections.nCopies(revurderingPerioder.size(), false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.BARNET_ER_DØD), Collections.nCopies(revurderingPerioder.size(), true),
            List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder, Collections.nCopies(revurderingPerioder.size(), false),
            List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
            List.of(PeriodeResultatÅrsak.UKJENT, PeriodeResultatÅrsak.BARNET_ER_DØD), Collections.nCopies(revurderingPerioder.size(), true),
            List.of(100), List.of(100), List.of(new Trekkdager(12)), List.of(UttakPeriodeType.FORELDREPENGER));

        var vilkårResultat = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);

        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        leggTilTilbaketrekk(false, false, førstegangsbehandling, revurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        var bhResultat = getBehandlingsresultat(revurdering);
        var uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getRettenTil()).isEqualTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(uendretUtfall).isTrue();
    }

    @Test
    void skal_gi_ingen_endring_når_det_er_avslag_på_avslag_selv_om_det_var_et_beslutningsvedtak_imellom() {
        // Arrange førstegangsbehandling
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var låsFgb = behandlingRepository.taSkriveLås(førstegangsbehandling);
        var vilkårResultatFgb = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
            .leggTilVilkårAvslått(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallMerknad.VM_1035)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .buildFor(førstegangsbehandling);
        behandlingRepository.lagre(vilkårResultatFgb, låsFgb);

        var resultatbuilderFgb = Behandlingsresultat.builderEndreEksisterende(førstegangsbehandling.getBehandlingsresultat());
        resultatbuilderFgb.medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        resultatbuilderFgb.medRettenTil(RettenTil.HAR_IKKE_RETT_TIL_FP);
        resultatbuilderFgb.buildFor(førstegangsbehandling);
        behandlingRepository.lagre(førstegangsbehandling, låsFgb);

        // Arrange revurdering 1 (beslutningsvedtak)
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
            .leggTilVilkårAvslått(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallMerknad.VM_1035)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        var resultatbuilderRevurdering = Behandlingsresultat.builderEndreEksisterende(revurdering.getBehandlingsresultat());
        resultatbuilderRevurdering.medBehandlingResultatType(BehandlingResultatType.INGEN_ENDRING);
        resultatbuilderRevurdering.medRettenTil(RettenTil.HAR_IKKE_RETT_TIL_FP);
        resultatbuilderRevurdering.buildFor(revurdering);
        behandlingRepository.lagre(revurdering, låsRevurdering);

        revurderingTestUtil.avsluttBehandling(revurdering);

        // Arrange revurdering 2
        var revurdering2 = revurderingTjeneste.opprettAutomatiskRevurdering(revurdering.getFagsak(), BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
            new OrganisasjonsEnhet("1234", "Test"));
        var låsRevurdering2 = behandlingRepository.taSkriveLås(revurdering2);
        var vilkårResultatRevurdering2 = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
            .leggTilVilkårAvslått(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallMerknad.VM_1035)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering2);
        behandlingRepository.lagre(vilkårResultatRevurdering2, låsRevurdering2);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering2, erVarselOmRevurderingSendt);

        // Assert
        var bhResultat = getBehandlingsresultat(revurdering2);
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering2)).isTrue();
    }

    @Test
    void skal_gi_innvilget_dersom_forrige_revurdering_var_avslått() {
        // Arrange førstegangsbehandling
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var låsFgb = behandlingRepository.taSkriveLås(førstegangsbehandling);
        var vilkårResultatFgb = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .buildFor(førstegangsbehandling);
        behandlingRepository.lagre(vilkårResultatFgb, låsFgb);

        var resultatbuilderFgb = Behandlingsresultat.builderEndreEksisterende(førstegangsbehandling.getBehandlingsresultat());
        resultatbuilderFgb.medBehandlingResultatType(BehandlingResultatType.INNVILGET);
        resultatbuilderFgb.medRettenTil(RettenTil.HAR_IKKE_RETT_TIL_FP);
        resultatbuilderFgb.buildFor(førstegangsbehandling);
        behandlingRepository.lagre(førstegangsbehandling, låsFgb);

        // Arrange revurdering 1 (beslutningsvedtak)
        var revurdering = opprettRevurdering(førstegangsbehandling);
        var låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        var vilkårResultatRevurdering = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
            .leggTilVilkårAvslått(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallMerknad.VM_1035)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        var resultatbuilderRevurdering = Behandlingsresultat.builderEndreEksisterende(revurdering.getBehandlingsresultat());
        resultatbuilderRevurdering.medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        resultatbuilderRevurdering.medRettenTil(RettenTil.HAR_IKKE_RETT_TIL_FP);
        resultatbuilderRevurdering.buildFor(revurdering);
        behandlingRepository.lagre(revurdering, låsRevurdering);

        revurderingTestUtil.avsluttBehandling(revurdering);

        // Arrange revurdering 2
        var revurdering2 = revurderingTjeneste.opprettAutomatiskRevurdering(revurdering.getFagsak(), BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER,
            new OrganisasjonsEnhet("1234", "Test"));
        var låsRevurdering2 = behandlingRepository.taSkriveLås(revurdering2);
        var vilkårResultatRevurdering2 = VilkårResultat.builder()
            .leggTilVilkårOppfylt(VilkårType.FØDSELSVILKÅRET_MOR)
            .leggTilVilkårOppfylt(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTilVilkårOppfylt(VilkårType.SØKERSOPPLYSNINGSPLIKT)
            .leggTilVilkårOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET)
            .buildFor(revurdering2);
        behandlingRepository.lagre(vilkårResultatRevurdering2, låsRevurdering2);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering2, erVarselOmRevurderingSendt);

        // Assert
        var bhResultat = getBehandlingsresultat(revurdering2);
        assertThat(bhResultat.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INNVILGET);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering2)).isTrue();
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
                                                                   List<LocalDateInterval> perioder,
                                                                   UttakPeriodeType stønadskontoType) {
        return lagUttakResultatPlanForBehandling(behandling, perioder, Collections.nCopies(perioder.size(), false),
            Collections.nCopies(perioder.size(), PeriodeResultatType.INNVILGET), Collections.nCopies(perioder.size(), PeriodeResultatÅrsak.UKJENT),
            Collections.nCopies(perioder.size(), true), List.of(100), List.of(100), List.of(Trekkdager.ZERO), List.of(stønadskontoType));
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
                                                                   List<LocalDateInterval> perioder,
                                                                   List<Boolean> samtidigUttak,
                                                                   List<PeriodeResultatType> periodeResultatTyper,
                                                                   List<PeriodeResultatÅrsak> periodeResultatÅrsak,
                                                                   List<Boolean> graderingInnvilget,
                                                                   List<Integer> andelIArbeid,
                                                                   List<Integer> utbetalingsgrad,
                                                                   List<Trekkdager> trekkdager,
                                                                   List<UttakPeriodeType> stønadskontoTyper) {
        var uttakresultat = LagUttakResultatPlanTjeneste.lagUttakResultatPlanTjeneste(behandling, perioder, samtidigUttak, periodeResultatTyper,
            periodeResultatÅrsak, graderingInnvilget, andelIArbeid, utbetalingsgrad, trekkdager, stønadskontoTyper);
        fpUttakRepository.lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakresultat.getGjeldendePerioder());
        return uttakresultat;
    }

    private void lagBeregningsresultatperiodeMedEndringstidspunkt(LocalDate endringsdato, Behandling førstegangsbehandling, Behandling revurdering) {
        var originaltresultat = LagBeregningsresultatTjeneste.lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, true, ORGNR);
        var revurderingsresultat = LagBeregningsresultatTjeneste.lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, false, ORGNR);
        beregningsresultatRepository.lagre(revurdering, revurderingsresultat);
        beregningsresultatRepository.lagre(førstegangsbehandling, originaltresultat);
    }

    private void byggBeregningsgrunnlagForBehandling(Behandling behandling,
                                                     boolean medOppjustertDagsat,
                                                     boolean skalDeleAndelMellomArbeidsgiverOgBruker,
                                                     List<ÅpenDatoIntervallEntitet> perioder) {
        byggBeregningsgrunnlagForBehandling(behandling, medOppjustertDagsat, skalDeleAndelMellomArbeidsgiverOgBruker, perioder,
            new LagEnAndelTjeneste());
    }

    private void byggBeregningsgrunnlagForBehandling(Behandling behandling,
                                                     boolean medOppjustertDagsat,
                                                     boolean skalDeleAndelMellomArbeidsgiverOgBruker,
                                                     List<ÅpenDatoIntervallEntitet> perioder,
                                                     LagAndelTjeneste lagAndelTjeneste) {
        var beregningsgrunnlag = LagBeregningsgrunnlagTjeneste.lagBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, medOppjustertDagsat,
            skalDeleAndelMellomArbeidsgiverOgBruker, perioder, lagAndelTjeneste);
        var gr = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(FASTSATT);
        when(beregningTjeneste.hent(BehandlingReferanse.fra(behandling))).thenReturn(Optional.of(gr));
    }

    private void lagreEndringsdato(LocalDate endringsdato, Long revurderingId) {
        var avklarteDatoer = new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(endringsdato).build();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(revurderingId).medAvklarteDatoer(avklarteDatoer).build();
        ytelsesFordelingRepository.lagre(revurderingId, ytelseFordelingAggregat);
    }

    private void leggTilTilbaketrekk(Boolean behandlingMedTilbaketrekk,
                                     Boolean originalBehandlingMedTilbaketrekk,
                                     Behandling førstegangsbehandling,
                                     Behandling revurdering) {
        if (behandlingMedTilbaketrekk != null) {
            beregningsresultatRepository.lagreMedTilbaketrekk(revurdering, behandlingMedTilbaketrekk);
        }
        if (originalBehandlingMedTilbaketrekk != null) {
            beregningsresultatRepository.lagreMedTilbaketrekk(førstegangsbehandling, originalBehandlingMedTilbaketrekk);
        }
    }
}
