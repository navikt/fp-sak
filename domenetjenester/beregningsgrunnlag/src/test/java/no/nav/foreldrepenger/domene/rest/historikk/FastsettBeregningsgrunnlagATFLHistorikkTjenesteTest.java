package no.nav.foreldrepenger.domene.rest.historikk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeløpEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.dto.InntektPrAndelDto;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class FastsettBeregningsgrunnlagATFLHistorikkTjenesteTest {
    private static final String NAV_ORGNR = "889640782";
    private static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(90000);
    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final int BRUTTO_PR_AR = 150000;
    private static final int OVERSTYRT_PR_AR = 200000;
    private static final int FRILANSER_INNTEKT = 4000;

    private BehandlingRepositoryProvider repositoryProvider;

    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private final VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);

    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);

    private FastsettBeregningsgrunnlagATFLHistorikkTjeneste fastsettBeregningsgrunnlagATFLHistorikkTjeneste;

    private Behandling behandling;
    private Virksomhet virk;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var arbeidsgiverHistorikkinnslagTjeneste = new ArbeidsgiverHistorikkinnslag(new ArbeidsgiverTjeneste(null, virksomhetTjeneste));
        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste = new FastsettBeregningsgrunnlagATFLHistorikkTjeneste(lagMockHistory(),
            arbeidsgiverHistorikkinnslagTjeneste, inntektArbeidYtelseTjeneste);
        virk = new Virksomhet.Builder().medOrgnr(NAV_ORGNR).medNavn("AF1").build();
        when(virksomhetTjeneste.hentOrganisasjon(NAV_ORGNR)).thenReturn(virk);
    }

    @Test
    public void skal_generere_historikkinnslag_ved_fastsettelse_av_brutto_beregningsgrunnlag_AT() {
        // Arrange
        var bgEndring = buildBeregningsgrunnlagEndring(false);

        //Dto
        var dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", Collections.singletonList(new InntektPrAndelDto(OVERSTYRT_PR_AR, 1L)), null);

        // Act
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto), dto,
            bgEndring);
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(1);

        var del = historikkInnslag.get(0);
        var feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        assertThat(feltList.get(0)).satisfies(felt -> {
            assertThat(felt.getNavn()).as("navn").isEqualTo(HistorikkEndretFeltType.INNTEKT_FRA_ARBEIDSFORHOLD.getKode());
            assertThat(felt.getNavnVerdi()).as("navnVerdi")
                .contains("AF1 (" + NAV_ORGNR + ") ..." + ARBEIDSFORHOLD_ID.getReferanse().substring(ARBEIDSFORHOLD_ID.getReferanse().length() - 4));
            assertThat(felt.getFraVerdi()).as("fraVerdi").isNull();
            assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo("200000");
        });
        assertThat(del.getBegrunnelse()).hasValueSatisfying(begrunnelse -> assertThat(begrunnelse).isEqualTo("begrunnelse"));
    }


    @Test
    public void skal_generere_historikkinnslag_ved_fastsettelse_av_brutto_beregningsgrunnlag_FL() {
        // Arrange
        var bgEndring = buildBeregningsgrunnlagEndring(true);

        //Dto
        var dto = new FastsettBeregningsgrunnlagATFLDto("begrunnelse", FRILANSER_INNTEKT);

        // Act
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto), dto,
            bgEndring);
        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        var historikkInnslag = tekstBuilder.build(historikkinnslag);

        // Assert
        assertThat(historikkInnslag).hasSize(1);

        var del = historikkInnslag.get(0);
        var feltList = del.getEndredeFelt();
        assertThat(feltList).hasSize(1);
        assertThat(feltList.get(0)).satisfies(felt -> {
            assertThat(felt.getNavn()).as("navn").isEqualTo(HistorikkEndretFeltType.FRILANS_INNTEKT.getKode());
            assertThat(felt.getFraVerdi()).as("fraVerdi").isNull();
            assertThat(felt.getTilVerdi()).as("tilVerdi").isEqualTo("4000");
        });
        assertThat(del.getBegrunnelse()).hasValueSatisfying(begrunnelse -> assertThat(begrunnelse).isEqualTo("begrunnelse"));
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        var mockHistory = mock(HistorikkTjenesteAdapter.class);
        when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

    private BeregningsgrunnlagEndring buildBeregningsgrunnlagEndring(boolean erFrilans) {
        AbstractTestScenario<?> scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        behandling = scenario.lagre(repositoryProvider);
        var fom = LocalDate.now().minusDays(20);
        var periodeEndring = lagTilBeregningsgrunnlagPeriodeEndring(fom, erFrilans);
        return new BeregningsgrunnlagEndring(List.of(periodeEndring));
    }

    private BeregningsgrunnlagPeriodeEndring lagTilBeregningsgrunnlagPeriodeEndring(LocalDate fomDato, boolean erFrilans) {
        var andelEndring = lagTilBeregningsgrunnlagPrStatusOgAndelEndring(virk, erFrilans);
        return new BeregningsgrunnlagPeriodeEndring(List.of(andelEndring), DatoIntervallEntitet.fraOgMed(fomDato));
    }

    private BeregningsgrunnlagPrStatusOgAndelEndring lagTilBeregningsgrunnlagPrStatusOgAndelEndring(Virksomhet virksomheten, boolean erFrilans) {

        var endring = erFrilans ? new BeregningsgrunnlagPrStatusOgAndelEndring(1L,
            no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.FRILANSER) : new BeregningsgrunnlagPrStatusOgAndelEndring(1L,
            Arbeidsgiver.virksomhet(virksomheten.getOrgnr()), ARBEIDSFORHOLD_ID);
        endring.setBeløpEndring(new BeløpEndring(null, erFrilans ? BigDecimal.valueOf(FRILANSER_INNTEKT) : BigDecimal.valueOf(OVERSTYRT_PR_AR)));
        return endring;
    }
}
