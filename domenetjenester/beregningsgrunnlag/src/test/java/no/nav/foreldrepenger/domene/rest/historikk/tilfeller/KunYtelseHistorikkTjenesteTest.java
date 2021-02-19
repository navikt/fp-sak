package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;


import static no.nav.foreldrepenger.domene.modell.AktivitetStatus.BRUKERS_ANDEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.FastsattBrukersAndel;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBgKunYtelseDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.modell.Inntektskategori;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class KunYtelseHistorikkTjenesteTest {

    private static final Long ANDELSNR = 1L;
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);

    private HistorikkTjenesteAdapter historikkAdapter;
    private KunYtelseHistorikkTjeneste kunYtelseHistorikkTjeneste;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @BeforeEach
    public void setup(EntityManager entityManager) {
        var arbeidsgiverTjeneste = new ArbeidsgiverTjeneste(null, mock(VirksomhetTjeneste.class));
        ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste = new ArbeidsgiverHistorikkinnslag(
            arbeidsgiverTjeneste);
        kunYtelseHistorikkTjeneste = new KunYtelseHistorikkTjeneste(arbeidsgiverHistorikkinnslagTjeneste);
        beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(ANDELSNR)
            .medAktivitetStatus(BRUKERS_ANDEL)
            .build(periode1);
        historikkAdapter = new HistorikkTjenesteAdapter(new HistorikkRepository(entityManager), mock(DokumentArkivTjeneste.class));
    }

    @Test
    public void skal_lage_historikk_for_andel_som_eksisterte_fra_før_i_grunnlag_ved_første_utførelse_av_aksjonspunkt() {
        // Arrange
        boolean nyAndel = false;
        boolean lagtTilAvSaksbehandler = false;
        Integer fastsatt = 100000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsattBrukersAndel andel = new FastsattBrukersAndel(nyAndel, ANDELSNR, lagtTilAvSaksbehandler, fastsatt,
            inntektskategori);
        FastsettBgKunYtelseDto kunYtelseDto = new FastsettBgKunYtelseDto(Collections.singletonList(andel), null);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(
            Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE), kunYtelseDto);

        // Act
        HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder();
        kunYtelseHistorikkTjeneste.lagHistorikk(null, dto, tekstBuilder, beregningsgrunnlag, Optional.empty(),
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        assertHistorikkinnslagFordeling(fastsatt, null, "Brukers andel");
    }

    @Test
    public void skal_lage_historikk_for_andel_som_eksisterte_fra_før_i_grunnlag_med_fastsatt_lik_overstyrt_i_forrige_utførelse_av_aksonspunkt() {
        // Arrange
        boolean nyAndel = false;
        boolean lagtTilAvSaksbehandler = false;
        Integer fastsatt = 100000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsattBrukersAndel brukersAndel = new FastsattBrukersAndel(nyAndel, ANDELSNR, lagtTilAvSaksbehandler,
            fastsatt, inntektskategori);
        FastsettBgKunYtelseDto kunYtelseDto = new FastsettBgKunYtelseDto(Collections.singletonList(brukersAndel), null);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(
            Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE), kunYtelseDto);

        BeregningsgrunnlagEntitet forrigeBg = new BeregningsgrunnlagEntitet(beregningsgrunnlag);
        forrigeBg.getBeregningsgrunnlagPerioder()
            .forEach(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList()
                .forEach(andel -> BeregningsgrunnlagPrStatusOgAndel.builder(andel)
                    .medBeregnetPrÅr(BigDecimal.valueOf(fastsatt * 12))));

        BeregningsgrunnlagGrunnlagEntitet forrigeGrunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(
            Optional.empty()).medBeregningsgrunnlag(forrigeBg).build(1L, BeregningsgrunnlagTilstand.KOFAKBER_UT);

        // Act
        HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder();
        kunYtelseHistorikkTjeneste.lagHistorikk(null, dto, tekstBuilder, beregningsgrunnlag,
            Optional.of(forrigeGrunnlag), InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        assertHistorikkinnslagFordeling(fastsatt, fastsatt * 12, "Brukers andel");
    }

    private void assertHistorikkinnslagFordeling(Integer fastsatt, Integer overstyrt, String andelsInfo) {
        List<HistorikkinnslagDel> deler = historikkAdapter.tekstBuilder().getHistorikkinnslagDeler();
        List<HistorikkinnslagDel> andelHistorikkinnslag = deler.stream()
            .filter(del -> del != null && del.getTema().isPresent() && andelsInfo.equals(
                del.getTema().get().getNavnVerdi()))
            .collect(Collectors.toList());
        Optional<HistorikkinnslagDel> fordelingInnslag = andelHistorikkinnslag.stream()
            .filter(del -> del.getEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_ANDEL).isPresent())
            .findFirst();
        Integer fastsattÅrsbeløp = fastsatt * 12;
        if (overstyrt != null && overstyrt.equals(fastsattÅrsbeløp)) {
            assertThat(fordelingInnslag.isPresent()).isFalse();
        } else if (overstyrt == null) {
            assertThat(fordelingInnslag).isPresent();
            assertThat(fordelingInnslag.get()
                .getEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_ANDEL)
                .get()
                .getFraVerdi()).isNull();
            assertThat(fordelingInnslag.get()
                .getEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_ANDEL)
                .get()
                .getTilVerdi()).isEqualTo(fastsatt.toString());
        } else {
            assertThat(fordelingInnslag).isPresent();
            assertThat(fordelingInnslag.get()
                .getEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_ANDEL)
                .get()
                .getFraVerdi()).isEqualTo(overstyrt.toString());
            assertThat(fordelingInnslag.get()
                .getEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_ANDEL)
                .get()
                .getTilVerdi()).isEqualTo(fastsatt.toString());
        }
    }

}
