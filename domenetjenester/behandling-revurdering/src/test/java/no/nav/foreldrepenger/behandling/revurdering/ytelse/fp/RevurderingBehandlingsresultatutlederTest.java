package no.nav.foreldrepenger.behandling.revurdering.ytelse.fp;

import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand.FASTSATT;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.BeregningRevurderingTestUtil;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagAndelTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagBeregningsresultatTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagEnAndelTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagToAndelerMotsattRekkefølgeTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagToAndelerTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.felles.LagUttakResultatPlanTjeneste;
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
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@CdiDbAwareTest
public class RevurderingBehandlingsresultatutlederTest {

    public static final String ORGNR = "987123987";
    private static final LocalDate SKJÆRINGSTIDSPUNKT_BEREGNING = LocalDate.now();
    public static final List<InternArbeidsforholdRef> ARBEIDSFORHOLDLISTE = List
            .of(InternArbeidsforholdRef.nyRef(), InternArbeidsforholdRef.nyRef(), InternArbeidsforholdRef.nyRef(),
                    InternArbeidsforholdRef.nyRef());
    public static final BigDecimal TOTAL_ANDEL_NORMAL = BigDecimal.valueOf(300000);
    public static final BigDecimal TOTAL_ANDEL_OPPJUSTERT = BigDecimal.valueOf(350000);

    @Inject
    private BeregningRevurderingTestUtil revurderingTestUtil;
    @Inject
    private YtelsesFordelingRepository ytelsesFordelingRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    @FagsakYtelseTypeRef("FP")
    private RevurderingTjeneste revurderingTjeneste;
    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;
    @Inject
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    @Inject
    private FpUttakRepository fpUttakRepository;
    @Inject
    private OpptjeningRepository opptjeningRepository;
    @Inject
    @FagsakYtelseTypeRef("FP")
    private RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    private final boolean erVarselOmRevurderingSendt = true;
    private final LocalDate endringsdato = LocalDate.now().minusMonths(3);

    private Behandling opprettRevurdering(Behandling førstegangsbehandling) {
        return revurderingTjeneste
                .opprettAutomatiskRevurdering(førstegangsbehandling.getFagsak(), BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
                        new OrganisasjonsEnhet("1234", "Test"));
    }

    private Behandling opprettFørstegangsbehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel().medDefaultSøknadTerminbekreftelse()
                .medDefaultOppgittFordeling(endringsdato).medAvklarteUttakDatoer(
                        new AvklarteUttakDatoerEntitet.Builder().medOpprinneligEndringsdato(endringsdato).build());
        scenario.medBehandlingVedtak().medVedtakstidspunkt(LocalDateTime.now())
                .medVedtakResultatType(VedtakResultatType.INNVILGET);
        scenario
                .leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE, BehandlingStegType.KONTROLLER_FAKTA);
        var førstegangsbehandling = scenario.lagre(repositoryProvider);
        opptjeningRepository
                .lagreOpptjeningsperiode(førstegangsbehandling, LocalDate.now().minusYears(1), LocalDate.now(),
                        false);
        revurderingTestUtil.avsluttBehandling(førstegangsbehandling);
        return førstegangsbehandling;
    }

    // Case 1
    // Løpende vedtak: Ja
    // Ikke oppfylt inngangsvilkår på skjæringstidspunktet
    // Ikke oppfylt inngangsvilkår i perioden
    // Endring i uttaksperiode: Ja
    @Test
    public void tilfelle_1_behandlingsresultat_lik_opphør_rettentil_lik_nei_foreldrepenger_opphører() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Endring i uttakperiode (ulik lengde)
        List<LocalDateInterval> opprinneligePerioder = List
                .of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));
        List<LocalDateInterval> revurderingPerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                        new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder, StønadskontoType.FEDREKVOTE);

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen())
                .containsExactly(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Endring i uttakperiode (ulik lengde)
        List<LocalDateInterval> opprinneligePerioder = List
                .of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));
        List<LocalDateInterval> revurderingPerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                        new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder, StønadskontoType.FEDREKVOTE);

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkårResultatManueltIkkeOppfylt(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE,
                        VilkårUtfallMerknad.VM_1020, Avslagsårsak.SØKER_ER_IKKE_MEDLEM)
                .buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen())
                .containsExactly(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Endring i uttakperiode (ulik lengde)
        List<LocalDateInterval> opprinneligePerioder = List
                .of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));
        List<LocalDateInterval> revurderingPerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                        new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak og endring i uttak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Siste periode avslått med opphørsårsak og endring
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeResultatÅrsak.UKJENT, IkkeOppfyltÅrsak.BARNET_ER_DØD),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));
        lagreEndringsdato(endringsdato, revurdering.getId());

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen())
                .containsExactly(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.of(2018, 1, 1);
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);
        lagreEndringsdato(endringsdato, revurdering.getId());

        // Endring i uttakperiode (ulik lengde)
        List<LocalDateInterval> opprinneligePerioder = List
                .of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.minusDays(5)));
        List<LocalDateInterval> revurderingPerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                        new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak og endring i uttak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Siste periode ikkje avslått med opphørsårsak
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET),
                List.of(PeriodeResultatÅrsak.UKJENT, IkkeOppfyltÅrsak.UKJENT),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.of(2018, 1, 1);
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Endring i uttakperiode (ulik lengde)
        List<LocalDateInterval> opprinneligePerioder = List
                .of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.minusDays(5)));
        List<LocalDateInterval> revurderingPerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                        new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Løpende vedtak og endring i uttak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Endring i beregning
        List<ÅpenDatoIntervallEntitet> bgPeriode = List
                .of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, true, false, bgPeriode);

        // Siste periode ikkje avslått med opphørsårsak
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.INNVILGET),
                List.of(PeriodeResultatÅrsak.UKJENT, IkkeOppfyltÅrsak.UKJENT),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INNVILGET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen())
                .containsExactly(KonsekvensForYtelsen.ENDRING_I_BEREGNING, KonsekvensForYtelsen.ENDRING_I_UTTAK);
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
    public void tilfelle_5_behandlingsresultat_lik_FPEndret_rettentil_lik_ja_foreldrepenger_konsekvens_Endring_i_beregning() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        List<LocalDateInterval> opprinneligePerioder = List
                .of(new LocalDateInterval(endringsdato.minusDays(10), endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Like perioder, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning
        List<ÅpenDatoIntervallEntitet> bgPeriode = List
                .of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, true, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType())
                .isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        List<LocalDateInterval> opprinneligePerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(50),
                List.of(100), List.of(new Trekkdager(10)), List.of(StønadskontoType.FELLESPERIODE));

        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning
        List<ÅpenDatoIntervallEntitet> bgPeriode = List
                .of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, true, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType())
                .isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen())
                .containsExactly(KonsekvensForYtelsen.ENDRING_I_BEREGNING, KonsekvensForYtelsen.ENDRING_I_UTTAK);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        List<LocalDateInterval> opprinneligePerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(50),
                List.of(100), List.of(new Trekkdager(10)), List.of(StønadskontoType.FELLESPERIODE));

        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning
        List<ÅpenDatoIntervallEntitet> bgPeriode = List
                .of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, false, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType())
                .isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        List<LocalDateInterval> opprinneligePerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning: kun endring i fordeling av ytelsen
        List<ÅpenDatoIntervallEntitet> bgPeriode = List
                .of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
        byggBeregningsgrunnlagForBehandling(revurdering, false, true, bgPeriode);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType())
                .isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen())
                .containsExactly(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        List<LocalDateInterval> opprinneligePerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning: Ingen endring
        List<ÅpenDatoIntervallEntitet> bgPeriode = List
                .of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
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
    // Endring i beregning: Nei (endring i rekkefølge av andeler, men ikkje endring
    // i fordeling)
    // Endring i uttaksperiode: Nei
    @Test
    public void tilfelle_9_ulik_rekkefølge_av_andeler_behandlingsresultat_lik_ingenEndring_rettentil_lik_ja_foreldrepenger_konsekvens_ingenEndring() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        List<LocalDateInterval> opprinneligePerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning: Ingen endring, kun endring i rekkefølge av andeler
        List<ÅpenDatoIntervallEntitet> bgPeriode = List
                .of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, true, bgPeriode,
                new LagToAndelerTjeneste());
        byggBeregningsgrunnlagForBehandling(revurdering, false, true, bgPeriode,
                new LagToAndelerMotsattRekkefølgeTjeneste());

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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        List<LocalDateInterval> opprinneligePerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(5)));

        // Løpende vedtak
        lagUttakResultatPlanForBehandling(førstegangsbehandling, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Ingen Endring i periode, siste periode ikkje avslått
        lagUttakResultatPlanForBehandling(revurdering, opprinneligePerioder, List.of(false),
                List.of(PeriodeResultatType.INNVILGET), List.of(PeriodeResultatÅrsak.UKJENT), List.of(true), List.of(100),
                List.of(100), List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Endring i beregning: kun endring i fordeling av ytelsen
        List<ÅpenDatoIntervallEntitet> bgPeriode = List
                .of(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT_BEREGNING, null));
        byggBeregningsgrunnlagForBehandling(førstegangsbehandling, false, false, bgPeriode);
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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Uttaksperiode som brukes for begge behandlinger
        List<LocalDateInterval> revurderingPerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                        new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(førstegangsbehandling, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeResultatÅrsak.UKJENT, IkkeOppfyltÅrsak.BARNET_ER_DØD),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeResultatÅrsak.UKJENT, IkkeOppfyltÅrsak.BARNET_ER_DØD),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

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
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Uttaksperiode som brukes for begge behandlinger
        List<LocalDateInterval> revurderingPerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                        new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(førstegangsbehandling, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeResultatÅrsak.UKJENT, IkkeOppfyltÅrsak.BARNET_ER_DØD),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Alle perioder avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.AVSLÅTT, PeriodeResultatType.AVSLÅTT),
                List.of(IkkeOppfyltÅrsak.BARNET_ER_DØD, IkkeOppfyltÅrsak.BARNET_ER_DØD),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));
        lagreEndringsdato(endringsdato, revurdering.getId());

        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.OPPHØR);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_IKKE_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen())
                .containsExactly(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
        assertThat(uendretUtfall).isFalse();
    }

    @Test
    public void skal_gi_endring_når_original_revurdering_ikke_har_samme_skalHindreTilbakketrekk() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Uttaksperiode som brukes for begge behandlinger
        List<LocalDateInterval> revurderingPerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                        new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(førstegangsbehandling, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeResultatÅrsak.UKJENT, IkkeOppfyltÅrsak.BARNET_ER_DØD),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeResultatÅrsak.UKJENT, IkkeOppfyltÅrsak.BARNET_ER_DØD),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Oppfylt inngangsvilkår på skjæringstidspunkt
        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        leggTilTilbaketrekk(true, false, førstegangsbehandling, revurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType())
                .isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen())
                .containsExactly(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(uendretUtfall).isFalse();
    }

    private Behandlingsresultat bestemBehandlingsresultatForRevurdering(Behandling revurdering,
            boolean erVarselOmRevurderingSendt) {
        var ref = BehandlingReferanse.fra(revurdering, SKJÆRINGSTIDSPUNKT_BEREGNING);
        return revurderingBehandlingsresultatutleder
                .bestemBehandlingsresultatForRevurdering(ref, erVarselOmRevurderingSendt);
    }

    @Test
    public void skal_gi_endring_når_original_revurdering_mangler_skalHindreTilbakketrekk() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Uttaksperiode som brukes for begge behandlinger
        List<LocalDateInterval> revurderingPerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                        new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(førstegangsbehandling, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeResultatÅrsak.UKJENT, IkkeOppfyltÅrsak.BARNET_ER_DØD),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeResultatÅrsak.UKJENT, IkkeOppfyltÅrsak.BARNET_ER_DØD),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        leggTilTilbaketrekk(null, false, førstegangsbehandling, revurdering);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering, erVarselOmRevurderingSendt);
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering);
        boolean uendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering);

        // Assert
        assertThat(bhResultat.getBehandlingResultatType())
                .isEqualByComparingTo(BehandlingResultatType.FORELDREPENGER_ENDRET);
        assertThat(bhResultat.getRettenTil()).isEqualByComparingTo(RettenTil.HAR_RETT_TIL_FP);
        assertThat(bhResultat.getKonsekvenserForYtelsen())
                .containsExactly(KonsekvensForYtelsen.ENDRING_I_FORDELING_AV_YTELSEN);
        assertThat(uendretUtfall).isFalse();
    }

    @Test
    public void skal_gi_ingen_endring_når_original_revurdering_har_samme_skalHindreTilbakketrekk() {

        // Arrange
        var førstegangsbehandling = opprettFørstegangsbehandling();
        var revurdering = opprettRevurdering(førstegangsbehandling);
        LocalDate endringsdato = LocalDate.now();
        lagreEndringsdato(endringsdato, revurdering.getId());
        lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, førstegangsbehandling, revurdering);

        // Uttaksperiode som brukes for begge behandlinger
        List<LocalDateInterval> revurderingPerioder = List
                .of(new LocalDateInterval(endringsdato, endringsdato.plusDays(10)),
                        new LocalDateInterval(endringsdato.plusDays(11), endringsdato.plusDays(20)));

        // Siste periode avslått med opphørsårsak for original behandling
        lagUttakResultatPlanForBehandling(førstegangsbehandling, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeResultatÅrsak.UKJENT, IkkeOppfyltÅrsak.BARNET_ER_DØD),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        // Siste periode avslått med opphørsårsak for revurdering
        lagUttakResultatPlanForBehandling(revurdering, revurderingPerioder,
                Collections.nCopies(revurderingPerioder.size(), false),
                List.of(PeriodeResultatType.INNVILGET, PeriodeResultatType.AVSLÅTT),
                List.of(PeriodeResultatÅrsak.UKJENT, IkkeOppfyltÅrsak.BARNET_ER_DØD),
                Collections.nCopies(revurderingPerioder.size(), true), List.of(100), List.of(100),
                List.of(new Trekkdager(12)), List.of(StønadskontoType.FORELDREPENGER));

        VilkårResultat vilkårResultat = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT).buildFor(revurdering);

        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(vilkårResultat, lås);

        leggTilTilbaketrekk(false, false, førstegangsbehandling, revurdering);

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
    public void skal_gi_ingen_endring_når_det_er_avslag_på_avslag_selv_om_det_var_et_beslutningsvedtak_imellom() {
        // Arrange førstegangsbehandling
        var førstegangsbehandling = opprettFørstegangsbehandling();
        BehandlingLås låsFgb = behandlingRepository.taSkriveLås(førstegangsbehandling);
        VilkårResultat vilkårResultatFgb = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .medVilkårResultatType(VilkårResultatType.AVSLÅTT).buildFor(førstegangsbehandling);
        behandlingRepository.lagre(vilkårResultatFgb, låsFgb);

        Behandlingsresultat.Builder resultatbuilderFgb = Behandlingsresultat
                .builderEndreEksisterende(førstegangsbehandling.getBehandlingsresultat());
        resultatbuilderFgb.medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
        resultatbuilderFgb.medRettenTil(RettenTil.HAR_IKKE_RETT_TIL_FP);
        resultatbuilderFgb.buildFor(førstegangsbehandling);
        behandlingRepository.lagre(førstegangsbehandling, låsFgb);

        // Arrange revurdering 1 (beslutningsvedtak)
        var revurdering = opprettRevurdering(førstegangsbehandling);
        BehandlingLås låsRevurdering = behandlingRepository.taSkriveLås(revurdering);
        VilkårResultat vilkårResultatRevurdering = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .medVilkårResultatType(VilkårResultatType.AVSLÅTT).buildFor(revurdering);
        behandlingRepository.lagre(vilkårResultatRevurdering, låsRevurdering);

        Behandlingsresultat.Builder resultatbuilderRevurdering = Behandlingsresultat
                .builderEndreEksisterende(revurdering.getBehandlingsresultat());
        resultatbuilderRevurdering.medBehandlingResultatType(BehandlingResultatType.INGEN_ENDRING);
        resultatbuilderRevurdering.medRettenTil(RettenTil.HAR_IKKE_RETT_TIL_FP);
        resultatbuilderRevurdering.buildFor(revurdering);
        behandlingRepository.lagre(revurdering, låsRevurdering);

        revurderingTestUtil.avsluttBehandling(revurdering);

        // Arrange revurdering 2
        Behandling revurdering2 = revurderingTjeneste
                .opprettAutomatiskRevurdering(revurdering.getFagsak(), BehandlingÅrsakType.RE_HENDELSE_FØDSEL,
                        new OrganisasjonsEnhet("1234", "Test"));
        BehandlingLås låsRevurdering2 = behandlingRepository.taSkriveLås(revurdering2);
        VilkårResultat vilkårResultatRevurdering2 = VilkårResultat.builder()
                .leggTilVilkår(VilkårType.FØDSELSVILKÅRET_MOR, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT)
                .leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.OPPFYLT)
                .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT)
                .medVilkårResultatType(VilkårResultatType.AVSLÅTT).buildFor(revurdering2);
        behandlingRepository.lagre(vilkårResultatRevurdering2, låsRevurdering2);

        // Act
        bestemBehandlingsresultatForRevurdering(revurdering2, erVarselOmRevurderingSendt);

        // Assert
        Behandlingsresultat bhResultat = getBehandlingsresultat(revurdering2);
        assertThat(bhResultat.getBehandlingResultatType()).isEqualByComparingTo(BehandlingResultatType.INGEN_ENDRING);
        assertThat(bhResultat.getKonsekvenserForYtelsen()).containsExactly(KonsekvensForYtelsen.INGEN_ENDRING);
        assertThat(revurderingTjeneste.erRevurderingMedUendretUtfall(revurdering2)).isTrue();
    }

    private UttakResultatEntitet lagUttakResultatPlanForBehandling(Behandling behandling,
            List<LocalDateInterval> perioder,
            StønadskontoType stønadskontoType) {
        return lagUttakResultatPlanForBehandling(behandling, perioder, Collections.nCopies(perioder.size(), false),
                Collections.nCopies(perioder.size(), PeriodeResultatType.INNVILGET),
                Collections.nCopies(perioder.size(), PeriodeResultatÅrsak.UKJENT),
                Collections.nCopies(perioder.size(), true), List.of(100), List.of(100), List.of(Trekkdager.ZERO),
                List.of(stønadskontoType));
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
            List<StønadskontoType> stønadskontoTyper) {
        UttakResultatEntitet uttakresultat = LagUttakResultatPlanTjeneste
                .lagUttakResultatPlanTjeneste(behandling, perioder, samtidigUttak, periodeResultatTyper,
                        periodeResultatÅrsak, graderingInnvilget, andelIArbeid, utbetalingsgrad, trekkdager, stønadskontoTyper);
        fpUttakRepository
                .lagreOpprinneligUttakResultatPerioder(behandling.getId(), uttakresultat.getGjeldendePerioder());
        return uttakresultat;
    }

    private void lagBeregningsresultatperiodeMedEndringstidspunkt(LocalDate endringsdato,
            Behandling førstegangsbehandling,
            Behandling revurdering) {
        BeregningsresultatEntitet originaltresultat = LagBeregningsresultatTjeneste
                .lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, true, ORGNR);
        BeregningsresultatEntitet revurderingsresultat = LagBeregningsresultatTjeneste
                .lagBeregningsresultatperiodeMedEndringstidspunkt(endringsdato, false, ORGNR);
        beregningsresultatRepository.lagre(revurdering, revurderingsresultat);
        beregningsresultatRepository.lagre(førstegangsbehandling, originaltresultat);
    }

    private BeregningsgrunnlagEntitet byggBeregningsgrunnlagForBehandling(Behandling behandling,
            boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker,
            List<ÅpenDatoIntervallEntitet> perioder) {
        return byggBeregningsgrunnlagForBehandling(behandling, medOppjustertDagsat,
                skalDeleAndelMellomArbeidsgiverOgBruker, perioder, new LagEnAndelTjeneste());
    }

    private BeregningsgrunnlagEntitet byggBeregningsgrunnlagForBehandling(Behandling behandling,
            boolean medOppjustertDagsat,
            boolean skalDeleAndelMellomArbeidsgiverOgBruker,
            List<ÅpenDatoIntervallEntitet> perioder,
            LagAndelTjeneste lagAndelTjeneste) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = LagBeregningsgrunnlagTjeneste
                .lagBeregningsgrunnlag(SKJÆRINGSTIDSPUNKT_BEREGNING, medOppjustertDagsat,
                        skalDeleAndelMellomArbeidsgiverOgBruker, perioder, lagAndelTjeneste);
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, FASTSATT);
        return beregningsgrunnlag;
    }

    private void lagreEndringsdato(LocalDate endringsdato, Long revurderingId) {
        AvklarteUttakDatoerEntitet avklarteDatoer = new AvklarteUttakDatoerEntitet.Builder()
                .medOpprinneligEndringsdato(endringsdato).build();
        ytelsesFordelingRepository.lagre(revurderingId, avklarteDatoer);
    }

    private void leggTilTilbaketrekk(Boolean behandlingMedTilbaketrekk,
            Boolean originalBehandlingMedTilbaketrekk,
            Behandling førstegangsbehandling,
            Behandling revurdering) {
        if (behandlingMedTilbaketrekk != null) {
            beregningsresultatRepository.lagreMedTilbaketrekk(revurdering, behandlingMedTilbaketrekk);
        }
        if (originalBehandlingMedTilbaketrekk != null) {
            beregningsresultatRepository
                    .lagreMedTilbaketrekk(førstegangsbehandling, originalBehandlingMedTilbaketrekk);
        }
    }
}
