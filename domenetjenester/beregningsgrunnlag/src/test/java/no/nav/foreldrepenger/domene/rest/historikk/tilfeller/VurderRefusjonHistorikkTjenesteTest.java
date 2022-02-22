package no.nav.foreldrepenger.domene.rest.historikk.tilfeller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.oppdateringresultat.FaktaOmBeregningVurderinger;
import no.nav.foreldrepenger.domene.oppdateringresultat.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.oppdateringresultat.RefusjonskravGyldighetEndring;
import no.nav.foreldrepenger.domene.oppdateringresultat.ToggleEndring;
import no.nav.foreldrepenger.domene.rest.dto.FaktaBeregningLagreDto;
import no.nav.foreldrepenger.domene.rest.dto.RefusjonskravPrArbeidsgiverVurderingDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class VurderRefusjonHistorikkTjenesteTest {
    private static final String NAV_ORGNR = "889640782";
    private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet(NAV_ORGNR);
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private VurderRefusjonHistorikkTjeneste vurderRefusjonHistorikkTjeneste;
    private final Historikkinnslag historikkinnslag = new Historikkinnslag();

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        historikkinnslag.setType(HistorikkinnslagType.FAKTA_ENDRET);
        historikkTjenesteAdapter = new HistorikkTjenesteAdapter(new HistorikkRepository(entityManager), mock(DokumentArkivTjeneste.class),
            new BehandlingRepository(entityManager));
        var virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        when(virksomhetTjeneste.hentOrganisasjon(VIRKSOMHET.getIdentifikator())).thenReturn(
            new Virksomhet.Builder().medOrgnr(VIRKSOMHET.getOrgnr()).build());
        var arbeidsgiverHistorikkinnslag = new ArbeidsgiverHistorikkinnslag(new ArbeidsgiverTjeneste(null, virksomhetTjeneste));
        vurderRefusjonHistorikkTjeneste = new VurderRefusjonHistorikkTjeneste(arbeidsgiverHistorikkinnslag);
    }

    @Test
    public void lag_historikk_når_ikkje_gyldig_utvidelse() {
        // Arrange
        var dto = lagDto(false);

        var oppdaterResultat = new OppdaterBeregningsgrunnlagResultat(null, null, lagVurderRefusjonEndring(VIRKSOMHET, false, null), null, null,
            null);

        // Act
        var historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, oppdaterResultat, dto, historikkInnslagTekstBuilder,
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.FALSE);
    }

    private FaktaOmBeregningVurderinger lagVurderRefusjonEndring(Arbeidsgiver arbeidsgiver, boolean erUtvidet, Boolean forrigeUtvidet) {
        var faktaOmBeregningVurderinger = new FaktaOmBeregningVurderinger();
        faktaOmBeregningVurderinger.setVurderRefusjonskravGyldighetEndringer(
            List.of(new RefusjonskravGyldighetEndring(new ToggleEndring(forrigeUtvidet, erUtvidet), arbeidsgiver)));
        return faktaOmBeregningVurderinger;
    }

    @Test
    public void oppdater_når_gyldig_utvidelse() {
        // Arrange
        var dto = lagDto(true);
        var oppdaterResultat = new OppdaterBeregningsgrunnlagResultat(null, null, lagVurderRefusjonEndring(VIRKSOMHET, true, null), null, null, null);

        // Act
        var historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, oppdaterResultat, dto, historikkInnslagTekstBuilder,
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.TRUE);
    }

    @Test
    public void oppdater_når_gyldig_utvidelse_med_forrige_satt_til_false() {
        // Arrange
        var dto = lagDto(true);
        var oppdaterResultat = new OppdaterBeregningsgrunnlagResultat(null, null, lagVurderRefusjonEndring(VIRKSOMHET, true, false), null, null,
            null);

        // Act
        var historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, oppdaterResultat, dto, historikkInnslagTekstBuilder,
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.TRUE, Boolean.FALSE);
    }

    @Test
    public void oppdater_når_gyldig_utvidelse_med_forrige_satt_til_true() {
        // Arrange
        var dto = lagDto(true);
        var oppdaterResultat = new OppdaterBeregningsgrunnlagResultat(null, null, lagVurderRefusjonEndring(VIRKSOMHET, true, true), null, null, null);
        // Act
        var historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, oppdaterResultat, dto, historikkInnslagTekstBuilder,
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    public void oppdater_når_ikkje_gyldig_utvidelse_og_forrige_satt_til_ikkje_gyldig() {
        // Arrange
        var dto = lagDto(false);
        var oppdaterResultat = new OppdaterBeregningsgrunnlagResultat(null, null, lagVurderRefusjonEndring(VIRKSOMHET, false, false), null, null,
            null);

        // Act
        var historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, oppdaterResultat, dto, historikkInnslagTekstBuilder,
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.FALSE, Boolean.FALSE);
    }

    @Test
    public void oppdater_når_ikkje_gyldig_utvidelse_og_forrige_satt_til_gyldig() {
        // Arrange
        var dto = lagDto(false);
        var oppdaterResultat = new OppdaterBeregningsgrunnlagResultat(null, null, lagVurderRefusjonEndring(VIRKSOMHET, false, true), null, null,
            null);

        // Act
        var historikkInnslagTekstBuilder = historikkTjenesteAdapter.tekstBuilder();
        vurderRefusjonHistorikkTjeneste.lagHistorikk(1L, oppdaterResultat, dto, historikkInnslagTekstBuilder,
            InntektArbeidYtelseGrunnlagBuilder.nytt().build());

        // Assert
        assertHistorikk(historikkInnslagTekstBuilder, Boolean.FALSE, Boolean.TRUE);
    }


    private void assertHistorikk(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder, Boolean tilVerdi) {
        var deler = historikkInnslagTekstBuilder.build(historikkinnslag);
        assertThat(deler).hasSize(1);
        var del = deler.get(0);
        var endredeFelt = del.getEndredeFelt();
        assertThat(endredeFelt).hasSize(1);
        assertThat(endredeFelt.get(0).getNavn()).isEqualTo(HistorikkEndretFeltType.NY_REFUSJONSFRIST.getKode());
        assertThat(endredeFelt.get(0).getFraVerdi()).isNull();
        assertThat(endredeFelt.get(0).getTilVerdi()).isEqualTo(tilVerdi.toString());
    }

    private void assertHistorikk(HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder, Boolean tilVerdi, Boolean fraVerdi) {
        var deler = historikkInnslagTekstBuilder.build(historikkinnslag);
        assertThat(deler).hasSize(1);
        var del = deler.get(0);
        var endredeFelt = del.getEndredeFelt();
        assertThat(endredeFelt).hasSize(1);
        assertThat(endredeFelt.get(0).getNavn()).isEqualTo(HistorikkEndretFeltType.NY_REFUSJONSFRIST.getKode());
        assertThat(endredeFelt.get(0).getFraVerdi()).isEqualTo(fraVerdi.toString());
        assertThat(endredeFelt.get(0).getTilVerdi()).isEqualTo(tilVerdi.toString());
    }

    private FaktaBeregningLagreDto lagDto(boolean skalUtvideGyldighet) {
        var dto = new FaktaBeregningLagreDto(List.of(FaktaOmBeregningTilfelle.VURDER_REFUSJONSKRAV_SOM_HAR_KOMMET_FOR_SENT));

        var ref1 = new RefusjonskravPrArbeidsgiverVurderingDto();
        ref1.setArbeidsgiverId(VIRKSOMHET.getIdentifikator());
        ref1.setSkalUtvideGyldighet(skalUtvideGyldighet);
        dto.setRefusjonskravGyldighet(List.of(ref1));
        return dto;
    }

}
