package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.tilfeller;


import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus.BRUKERS_ANDEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FastsattBrukersAndel;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FastsettBgKunYtelseDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Inntektskategori;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjenesteImpl;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagKonverter;
import no.nav.vedtak.felles.integrasjon.journal.v3.JournalConsumerImpl;

public class KunYtelseHistorikkTjenesteTest {

    private static final Long ANDELSNR = 1L;
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private EntityManager em = repositoryRule.getEntityManager();

    private KodeverkRepository kodeverkRepository = new KodeverkRepository(em);
    private final DokumentArkivTjeneste dokumentArkivTjeneste = new DokumentArkivTjeneste(mock(JournalConsumerImpl.class), new FagsakRepository(em), kodeverkRepository);
    private HistorikkTjenesteAdapter historikkAdapter = new HistorikkTjenesteAdapter(
        new HistorikkRepository(repositoryRule.getEntityManager()), new HistorikkInnslagKonverter(), dokumentArkivTjeneste);
    private KunYtelseHistorikkTjeneste kunYtelseHistorikkTjeneste;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private BeregningsgrunnlagEntitet beregningsgrunnlag;

    @Before
    public void setup() {
        VirksomhetRepository virksomhetRepo = new VirksomhetRepository();
        var virksomhetTjeneste = new VirksomhetTjeneste(null, virksomhetRepo);
        var arbeidsgiverTjeneste = new ArbeidsgiverTjenesteImpl(null, virksomhetTjeneste);
        this.arbeidsgiverHistorikkinnslagTjeneste = new ArbeidsgiverHistorikkinnslag(arbeidsgiverTjeneste);
        this.kunYtelseHistorikkTjeneste = new KunYtelseHistorikkTjeneste(arbeidsgiverHistorikkinnslagTjeneste);
        beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT).build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(ANDELSNR)
            .medLagtTilAvSaksbehandler(false)
            .medAktivitetStatus(BRUKERS_ANDEL)
            .build(periode1);
    }

    @Test
    public void skal_lage_historikk_for_andel_som_eksisterte_fra_før_i_grunnlag_ved_første_utførelse_av_aksjonspunkt() {
        // Arrange
        boolean nyAndel = false;
        boolean lagtTilAvSaksbehandler = false;
        Integer fastsatt = 100000;
        Inntektskategori inntektskategori = Inntektskategori.SJØMANN;
        FastsattBrukersAndel andel = new FastsattBrukersAndel(nyAndel, ANDELSNR, lagtTilAvSaksbehandler, fastsatt, inntektskategori);
        FastsettBgKunYtelseDto kunYtelseDto = new FastsettBgKunYtelseDto(Collections.singletonList(andel), null);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE),
            kunYtelseDto);

        // Act
        HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder();
        kunYtelseHistorikkTjeneste.lagHistorikk(null, dto, tekstBuilder, beregningsgrunnlag, Optional.empty(), InntektArbeidYtelseGrunnlagBuilder.nytt().build());
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
        FastsattBrukersAndel brukersAndel = new FastsattBrukersAndel(nyAndel, ANDELSNR, lagtTilAvSaksbehandler, fastsatt, inntektskategori);
        FastsettBgKunYtelseDto kunYtelseDto = new FastsettBgKunYtelseDto(Collections.singletonList(brukersAndel), null);
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(Collections.singletonList(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE),
            kunYtelseDto);

        BeregningsgrunnlagEntitet forrigeBg = beregningsgrunnlag.dypKopi();
        forrigeBg.getBeregningsgrunnlagPerioder().forEach(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().forEach(andel ->
            BeregningsgrunnlagPrStatusOgAndel.builder(andel).medBeregnetPrÅr(BigDecimal.valueOf(fastsatt * 12))));

        BeregningsgrunnlagGrunnlagEntitet forrigeGrunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(forrigeBg)
            .build(1L, BeregningsgrunnlagTilstand.KOFAKBER_UT);

        // Act
        HistorikkInnslagTekstBuilder tekstBuilder = historikkAdapter.tekstBuilder();
        kunYtelseHistorikkTjeneste.lagHistorikk(null, dto, tekstBuilder, beregningsgrunnlag, Optional.of(forrigeGrunnlag), InntektArbeidYtelseGrunnlagBuilder.nytt().build());
        tekstBuilder.ferdigstillHistorikkinnslagDel();

        // Assert
        assertHistorikkinnslagFordeling(fastsatt, fastsatt * 12, "Brukers andel");
    }

    private void assertHistorikkinnslagFordeling(Integer fastsatt, Integer overstyrt, String andelsInfo) {
        List<HistorikkinnslagDel> deler = historikkAdapter.tekstBuilder().getHistorikkinnslagDeler();
        List<HistorikkinnslagDel> andelHistorikkinnslag = deler.stream().filter(del ->
            del != null &&
                del.getTema().isPresent() &&
                andelsInfo.equals(del.getTema().get().getNavnVerdi()))
            .collect(Collectors.toList());
        Optional<HistorikkinnslagDel> fordelingInnslag = andelHistorikkinnslag.stream().filter(del -> del.getEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_ANDEL).isPresent()).findFirst();
        Integer fastsattÅrsbeløp = fastsatt * 12;
        if (overstyrt != null && overstyrt.equals(fastsattÅrsbeløp)) {
            assertThat(fordelingInnslag.isPresent()).isFalse();
        } else if (overstyrt == null) {
            assertThat(fordelingInnslag.isPresent()).isTrue();
            assertThat(fordelingInnslag.get().getEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_ANDEL).get().getFraVerdi()).isNull();
            assertThat(fordelingInnslag.get().getEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_ANDEL).get().getTilVerdi()).isEqualTo(fastsatt.toString());
        } else {
            assertThat(fordelingInnslag.isPresent()).isTrue();
            assertThat(fordelingInnslag.get().getEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_ANDEL).get().getFraVerdi()).isEqualTo(overstyrt.toString());
            assertThat(fordelingInnslag.get().getEndretFelt(HistorikkEndretFeltType.FORDELING_FOR_ANDEL).get().getTilVerdi()).isEqualTo(fastsatt.toString());
        }
    }

}
