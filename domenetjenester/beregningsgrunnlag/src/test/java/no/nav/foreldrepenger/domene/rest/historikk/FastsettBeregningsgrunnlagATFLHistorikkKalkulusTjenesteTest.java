package no.nav.foreldrepenger.domene.rest.historikk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeløpEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.dto.InntektPrAndelDto;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

@CdiDbAwareTest
class FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjenesteTest {
    private static final String NAV_ORGNR = "889640782";
    private static final String ARBEIDSGIVER_NAVN = "AF1";
    private static final Virksomhet VIRKSOMHET = new Virksomhet.Builder().medOrgnr(NAV_ORGNR).medNavn(ARBEIDSGIVER_NAVN).build();
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final int OVERSTYRT_PR_AR = 200000;
    private static final int FRILANSER_INNTEKT = 4000;
    public static final LocalDate STP = LocalDate.now().minusDays(5);

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Test
    void skal_generere_historikkinnslag_ved_fastsettelse_av_brutto_beregningsgrunnlag_AT() {
        var arbeidsgiverTjeneste = mock(ArbeidsgiverTjeneste.class);
        when(arbeidsgiverTjeneste.hent(any())).thenReturn(new ArbeidsgiverOpplysninger(NAV_ORGNR, ARBEIDSGIVER_NAVN));
        var inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var fastsettBeregningsgrunnlagATFLHistorikkTjeneste = new FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste(
            new ArbeidsgiverHistorikkinnslag(arbeidsgiverTjeneste), inntektArbeidYtelseTjeneste, repositoryProvider.getHistorikkinnslagRepository());

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(),
            InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER));

        // Arrange
        var andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            new BeløpEndring(null, BigDecimal.valueOf(OVERSTYRT_PR_AR)), null, null, AktivitetStatus.ARBEIDSTAKER, OpptjeningAktivitetType.ARBEID,
            Arbeidsgiver.fra(VIRKSOMHET), ARBEIDSFORHOLD_ID);
        var perioder = List.of(new BeregningsgrunnlagPeriodeEndring(List.of(andelEndring),
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, Tid.TIDENES_ENDE)));
        var bge = new BeregningsgrunnlagEndring(perioder);
        var endringsaggregat = new OppdaterBeregningsgrunnlagResultat(bge, null, null, null, List.of());

        var dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.singletonList(new InntektPrAndelDto(OVERSTYRT_PR_AR, 1L)), null);

        // Act
        var ref = BehandlingReferanse.fra(behandling);
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(new AksjonspunktOppdaterParameter(ref, dto), dto, endringsaggregat);

        // Assert
        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getSkjermlenke()).isEqualTo(SkjermlenkeType.BEREGNING_FORELDREPENGER);
        assertThat(historikkinnslag.getLinjer()).hasSize(3);
        assertThat(historikkinnslag.getLinjer().getFirst().getTekst()).isEqualTo("Grunnlag for beregnet årsinntekt:");
        assertThat(historikkinnslag.getLinjer().get(1).getTekst()).isEqualTo("__Inntekt fra AF1 (889640782) ...050d__ er satt til __200 000 kr__.");
        assertThat(historikkinnslag.getLinjer().get(2).getTekst()).contains(dto.getBegrunnelse());
    }

    @Test
    void skal_generere_historikkinnslag_ved_fastsettelse_av_brutto_beregningsgrunnlag_FL() {
        var inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var fastsettBeregningsgrunnlagATFLHistorikkTjeneste = new FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste(
            new ArbeidsgiverHistorikkinnslag(null), inntektArbeidYtelseTjeneste, repositoryProvider.getHistorikkinnslagRepository());

        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(repositoryProvider);
        inntektArbeidYtelseTjeneste.lagreIayAggregat(behandling.getId(),
            InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER));

        var andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            new BeløpEndring(null, BigDecimal.valueOf(OVERSTYRT_PR_AR)), null, null, AktivitetStatus.FRILANSER, OpptjeningAktivitetType.FRILANS, null, null);
        var perioder = List.of(new BeregningsgrunnlagPeriodeEndring(List.of(andelEndring),
            DatoIntervallEntitet.fraOgMedTilOgMed(STP, Tid.TIDENES_ENDE)));
        var bge = new BeregningsgrunnlagEndring(perioder);
        var endringsaggregat = new OppdaterBeregningsgrunnlagResultat(bge, null, null, null, List.of());

        var dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", FRILANSER_INNTEKT);

        // Act
        var ref = BehandlingReferanse.fra(behandling);
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(new AksjonspunktOppdaterParameter(ref, dto), dto, endringsaggregat);

        // Assert
        var historikkinnslag = repositoryProvider.getHistorikkinnslagRepository().hent(behandling.getSaksnummer()).getFirst();
        assertThat(historikkinnslag.getSkjermlenke()).isEqualTo(SkjermlenkeType.BEREGNING_FORELDREPENGER);
        assertThat(historikkinnslag.getLinjer()).hasSize(3);
        assertThat(historikkinnslag.getLinjer().getFirst().getTekst()).isEqualTo("Grunnlag for beregnet årsinntekt:");
        assertThat(historikkinnslag.getLinjer().get(1).getTekst()).isEqualTo("__Frilansinntekt__ er satt til __4 000 kr__.");
        assertThat(historikkinnslag.getLinjer().get(2).getTekst()).contains(dto.getBegrunnelse());

    }
}
