package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.tilfeller;

import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.Inntektskategori.ARBEIDSTAKER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.dto.RefusjonskravPrArbeidsgiverVurderingDto;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningRefusjonOverstyringerEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.historikk.dto.HistorikkInnslagKonverter;

public class VurderRefusjonHistorikkTjenesteTest {
    private static final String NAV_ORGNR = "889640782";
    private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet(NAV_ORGNR);
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private EntityManager em = repositoryRule.getEntityManager();
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private VurderRefusjonHistorikkTjeneste vurderRefusjonHistorikkTjeneste;
    private Historikkinnslag historikkinnslag = new Historikkinnslag();

    @Before
    public void setUp() {
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        KodeverkRepository kodeverkRepository = new KodeverkRepository(em);
        DokumentArkivTjeneste dokumentArkivTjeneste = new DokumentArkivTjeneste(null,
            new FagsakRepository(em), kodeverkRepository);
        HistorikkInnslagKonverter historikkinnslagKonverter = new HistorikkInnslagKonverter();
        historikkTjenesteAdapter = new HistorikkTjenesteAdapter(new HistorikkRepository(em), historikkinnslagKonverter, dokumentArkivTjeneste);
        VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        when(virksomhetTjeneste.hentOrganisasjon(VIRKSOMHET.getIdentifikator()))
            .thenReturn(new Virksomhet.Builder().medOrgnr(VIRKSOMHET.getOrgnr()).build());
        ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag = new ArbeidsgiverHistorikkinnslag(new ArbeidsgiverTjeneste(null, virksomhetTjeneste));
        vurderRefusjonHistorikkTjeneste = new VurderRefusjonHistorikkTjeneste(arbeidsgiverHistorikkinnslag);
    }

    @Test
    public void lag_historikk_når_ikkje_gyldig_utvidelse() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(false);

        // Act
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, dto, historikkInnslagTekstBuilder, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.empty(), InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.FALSE);
    }

    @Test
    public void oppdater_når_gyldig_utvidelse() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(true);

        // Act
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, dto, historikkInnslagTekstBuilder, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.empty(), InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.TRUE);
    }

    @Test
    public void oppdater_når_gyldig_utvidelse_med_forrige_satt_til_false() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet forrige = lagBeregningsgrunnlagMedOverstyring(SKJÆRINGSTIDSPUNKT.plusMonths(1));
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(true);

        // Act
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, dto, historikkInnslagTekstBuilder, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.of(forrige), InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void oppdater_når_gyldig_utvidelse_med_forrige_satt_til_true() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet forrige = lagBeregningsgrunnlagMedOverstyring(SKJÆRINGSTIDSPUNKT);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(true);

        // Act
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, dto, historikkInnslagTekstBuilder, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.of(forrige), InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void oppdater_når_ikkje_gyldig_utvidelse_og_forrige_satt_til_ikkje_gyldig() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet forrige = lagBeregningsgrunnlagMedOverstyring(SKJÆRINGSTIDSPUNKT.plusMonths(1));
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(false);

        // Act
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, dto, historikkInnslagTekstBuilder, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.of(forrige), InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void oppdater_når_ikkje_gyldig_utvidelse_og_forrige_satt_til_gyldig() {
        // Arrange
        BeregningsgrunnlagGrunnlagEntitet forrige = lagBeregningsgrunnlagMedOverstyring(SKJÆRINGSTIDSPUNKT);
        BeregningsgrunnlagGrunnlagEntitet grunnlag = lagBeregningsgrunnlag();
        FaktaBeregningLagreDto dto = lagDto(false);

        // Act
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, dto, historikkInnslagTekstBuilder, grunnlag.getBeregningsgrunnlag().orElseThrow(), Optional.of(forrige), InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.FALSE, Boolean.TRUE);
    }


    private void assertHistorikk(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder, Boolean tilVerdi) {
        List<HistorikkinnslagDel> deler = historikkInnslagTekstBuilder.build(historikkinnslag);
        assertThat(deler.size()).isEqualTo(1);
        HistorikkinnslagDel del = deler.get(0);
        List<HistorikkinnslagFelt> endredeFelt = del.getEndredeFelt();
        assertThat(endredeFelt.size()).isEqualTo(1);
        assertThat(endredeFelt.get(0).getNavn()).isEqualTo(HistorikkEndretFeltType.NY_REFUSJONSFRIST.getKode());
        assertThat(endredeFelt.get(0).getFraVerdi()).isNull();
        assertThat(endredeFelt.get(0).getTilVerdi()).isEqualTo(tilVerdi.toString());
    }

    private void assertHistorikk(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder, Boolean tilVerdi, Boolean fraVerdi) {
        List<HistorikkinnslagDel> deler = historikkInnslagTekstBuilder.build(historikkinnslag);
        assertThat(deler.size()).isEqualTo(1);
        HistorikkinnslagDel del = deler.get(0);
        List<HistorikkinnslagFelt> endredeFelt = del.getEndredeFelt();
        assertThat(endredeFelt.size()).isEqualTo(1);
        assertThat(endredeFelt.get(0).getNavn()).isEqualTo(HistorikkEndretFeltType.NY_REFUSJONSFRIST.getKode());
        assertThat(endredeFelt.get(0).getFraVerdi()).isEqualTo(fraVerdi.toString());
        assertThat(endredeFelt.get(0).getTilVerdi()).isEqualTo(tilVerdi.toString());
    }

    private FaktaBeregningLagreDto lagDto(boolean skalUtvideGyldighet) {
        FaktaBeregningLagreDto dto = new FaktaBeregningLagreDto(List.of(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT));

        RefusjonskravPrArbeidsgiverVurderingDto ref1 = new RefusjonskravPrArbeidsgiverVurderingDto();
        ref1.setArbeidsgiverId(VIRKSOMHET.getIdentifikator());
        ref1.setSkalUtvideGyldighet(skalUtvideGyldighet);
        dto.setRefusjonskravGyldighet(List.of(ref1));
        return dto;
    }

    private BeregningsgrunnlagGrunnlagEntitet lagBeregningsgrunnlagMedOverstyring(LocalDate dato) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilFaktaOmBeregningTilfeller(List.of(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT))
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(VIRKSOMHET))
            .medInntektskategori(ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode1);
        BeregningRefusjonOverstyringerEntitet overstyring = BeregningRefusjonOverstyringerEntitet.builder().leggTilOverstyring(new BeregningRefusjonOverstyringEntitet(VIRKSOMHET, dato)).build();
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medRefusjonOverstyring(overstyring)
            .build(1L, BeregningsgrunnlagTilstand.KOFAKBER_UT);
    }

    private BeregningsgrunnlagGrunnlagEntitet lagBeregningsgrunnlag() {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medGrunnbeløp(GRUNNBELØP)
            .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
            .leggTilFaktaOmBeregningTilfeller(List.of(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT))
            .build();
        BeregningsgrunnlagPeriode periode1 = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(SKJÆRINGSTIDSPUNKT, SKJÆRINGSTIDSPUNKT.plusMonths(2).minusDays(1))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medBGAndelArbeidsforhold(BGAndelArbeidsforhold.builder().medArbeidsgiver(VIRKSOMHET))
            .medInntektskategori(ARBEIDSTAKER)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(periode1);
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty()).medBeregningsgrunnlag(beregningsgrunnlag).build(1L, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
    }

}
