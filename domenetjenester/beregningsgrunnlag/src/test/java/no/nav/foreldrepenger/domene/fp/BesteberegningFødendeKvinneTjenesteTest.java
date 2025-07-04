package no.nav.foreldrepenger.domene.fp;

import static no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet.fraOgMedTilOgMed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.HendelseVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitet;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;

@ExtendWith(MockitoExtension.class)
class BesteberegningFødendeKvinneTjenesteTest {

    private static final String ORGNR = OrgNummer.KUNSTIG_ORG;
    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.of(2018, 7, 1);
    private BehandlingReferanse behandlingReferanse;
    private static final Skjæringstidspunkt STP = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
    private static final ÅpenDatoIntervallEntitet OPPTJENINGSPERIODE = fraOgMedTilOgMed(SKJÆRINGSTIDSPUNKT.minusYears(1),
        SKJÆRINGSTIDSPUNKT.plusYears(10));

    @Mock
    private FamilieHendelseRepository familieHendelseRepository;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    @Mock
    private BeregningTjeneste beregningTjeneste;
    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private BeregningsresultatRepository beregningsresultatRepository;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private AbakusInMemoryInntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    @BeforeEach
    void setUp() {
        var behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        behandlingReferanse = BehandlingReferanse.fra(behandling);
        inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandlingReferanse.behandlingId(),
            InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER));
        besteberegningFødendeKvinneTjeneste = new BesteberegningFødendeKvinneTjeneste(familieHendelseRepository, opptjeningForBeregningTjeneste,
            inntektArbeidYtelseTjeneste, beregningTjeneste, behandlingRepository, beregningsresultatRepository, fagsakRepository);
    }

    @Test
    void skal_ikkje_gi_fødende_kvinne_for_adopsjon() {
        // Act
        var erFødendeKvinne = BesteberegningFødendeKvinneTjeneste.erFødendeKvinne(RelasjonsRolleType.MORA, FamilieHendelseType.ADOPSJON);

        // Assert
        assertThat(erFødendeKvinne).isFalse();
    }

    @Test
    void skal_ikkje_gi_fødende_kvinne_for_far_fødsel() {
        // Act
        var erFødendeKvinne = BesteberegningFødendeKvinneTjeneste.erFødendeKvinne(RelasjonsRolleType.FARA, FamilieHendelseType.FØDSEL);

        // Assert
        assertThat(erFødendeKvinne).isFalse();
    }

    @Test
    void skal_gi_fødende_kvinne_for_mor_fødsel() {
        // Act
        var erFødendeKvinne = BesteberegningFødendeKvinneTjeneste.erFødendeKvinne(RelasjonsRolleType.MORA, FamilieHendelseType.FØDSEL);

        // Assert
        assertThat(erFødendeKvinne).isTrue();
    }

    @Test
    void skalGiBesteberegningNårDagpengerPåStp() {
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(lagfamilieHendelse()));
        var opptjeningAktiviteter = OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER,
            new Periode(OPPTJENINGSPERIODE.getFomDato(), SKJÆRINGSTIDSPUNKT.plusDays(1)));
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any(), any())).thenReturn(Optional.of(opptjeningAktiviteter));

        // Act
        var resultat = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(behandlingReferanse, STP);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skalGiBesteberegningNårSykepengerMedOvergangFraDagpenger() {
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(lagfamilieHendelse()));
        var opptjeningAktiviteter = OpptjeningAktiviteter.fraOrgnr(OpptjeningAktivitetType.ARBEID,
            new Periode(OPPTJENINGSPERIODE.getFomDato(), OPPTJENINGSPERIODE.getTomDato()), ORGNR);
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any(), any())).thenReturn(Optional.of(opptjeningAktiviteter));

        var oppdatere = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);
        var aktørYtelseBuilder = oppdatere.getAktørYtelseBuilder(behandlingReferanse.aktørId());
        var ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medPeriode(DatoIntervallEntitet.fraOgMed(SKJÆRINGSTIDSPUNKT.minusMonths(10)))
            .medStatus(RelatertYtelseTilstand.LØPENDE)
            .medYtelseType(RelatertYtelseType.SYKEPENGER);
        var grunnlagBuilder = ytelseBuilder.getGrunnlagBuilder();
        grunnlagBuilder.medArbeidskategori(Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER);
        ytelseBuilder.medYtelseGrunnlag(grunnlagBuilder.build());
        aktørYtelseBuilder.leggTilYtelse(ytelseBuilder);
        oppdatere.leggTilAktørYtelse(aktørYtelseBuilder);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandlingReferanse.behandlingId(), oppdatere);

        // Act
        var resultat = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(behandlingReferanse, STP);

        // Assert
        assertThat(resultat).isTrue();
    }

    @Test
    void skalIkkeGiBesteberegningIkkeDagpengerPåStp() {
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(lagfamilieHendelse()));
        var opptjeningAktiviteter = OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER,
            new Periode(OPPTJENINGSPERIODE.getFomDato(), SKJÆRINGSTIDSPUNKT.minusDays(3)));
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any(), any())).thenReturn(Optional.of(opptjeningAktiviteter));
        // Act
        var resultat = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(behandlingReferanse, STP);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void overstyrtBGSkalIkkeGiAutomatiskBesteberegning() {
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(lagfamilieHendelse()));
        var opptjeningAktiviteter = OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER,
            new Periode(OPPTJENINGSPERIODE.getFomDato(), SKJÆRINGSTIDSPUNKT.plusDays(1)));
        var bg = Beregningsgrunnlag.builder().medSkjæringstidspunkt(LocalDate.now()).medOverstyring(true).build();
        var overstyrtGrunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(bg).build(BeregningsgrunnlagTilstand.FORESLÅTT);
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any(), any())).thenReturn(Optional.of(opptjeningAktiviteter));
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(overstyrtGrunnlag));

        // Act
        var resultat = besteberegningFødendeKvinneTjeneste.kvalifisererTilAutomatiskBesteberegning(behandlingReferanse, STP);

        // Assert
        assertThat(resultat).isFalse();
    }

    @Test
    void fjernetDagpengerISaksbehandling() {
        when(familieHendelseRepository.hentAggregatHvisEksisterer(any())).thenReturn(Optional.of(lagfamilieHendelse()));
        var opptjeningAktiviteter = OpptjeningAktiviteter.fra(OpptjeningAktivitetType.DAGPENGER,
            new Periode(OPPTJENINGSPERIODE.getFomDato(), SKJÆRINGSTIDSPUNKT.plusDays(1)));
        var grunnlag = Beregningsgrunnlag.builder().medSkjæringstidspunkt(LocalDate.now()).build();
        var bgGr = BeregningsgrunnlagGrunnlagBuilder.nytt()
            .medBeregningsgrunnlag(grunnlag)
            .medRegisterAktiviteter(lagAggregat(OpptjeningAktivitetType.ARBEID, OpptjeningAktivitetType.DAGPENGER))
            .medSaksbehandletAktiviteter(lagAggregat(OpptjeningAktivitetType.ARBEID))
            .build(BeregningsgrunnlagTilstand.FORESLÅTT);
        when(opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(any(), any(), any())).thenReturn(Optional.of(opptjeningAktiviteter));
        when(beregningTjeneste.hent(any())).thenReturn(Optional.of(bgGr));

        // Act
        var resultat = besteberegningFødendeKvinneTjeneste.kvalifisererTilAutomatiskBesteberegning(behandlingReferanse, STP);

        // Assert
        assertThat(resultat).isFalse();
    }

    private BeregningAktivitetAggregat lagAggregat(OpptjeningAktivitetType... typer) {
        var builder = BeregningAktivitetAggregat.builder();
        Arrays.asList(typer)
            .forEach(type -> builder.leggTilAktivitet(BeregningAktivitet.builder()
                .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(OPPTJENINGSPERIODE.getFomDato(), OPPTJENINGSPERIODE.getTomDato()))
                .medOpptjeningAktivitetType(type)
                .build()));
        return builder.medSkjæringstidspunktOpptjening(OPPTJENINGSPERIODE.getTomDato()).build();
    }


    private FamilieHendelseGrunnlagEntitet lagfamilieHendelse() {
        var build = FamilieHendelseBuilder.oppdatere(Optional.empty(), HendelseVersjonType.BEKREFTET)
            .medAntallBarn(1)
            .medFødselsDato(SKJÆRINGSTIDSPUNKT.plusWeeks(3));
        return FamilieHendelseGrunnlagBuilder.oppdatere(Optional.empty()).medBekreftetVersjon(build).build();
    }

}
