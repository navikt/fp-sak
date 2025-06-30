package no.nav.foreldrepenger.domene.rest.historikk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeløpEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.InntektskategoriEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.aksjonspunkt.RefusjonEndring;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinje;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelFastsatteVerdierDto;
import no.nav.foreldrepenger.domene.typer.AktørId;

class FordelBeregningsgrunnlagHistorikkKalkulusTjenesteTest {
    private static final String ARBEIDSFORHOLDINFO = "DYNAMISK OPPSTEMT HAMSTER KF (311343483)";

    private final ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste = mock(ArbeidsgiverHistorikkinnslag.class);
    private final HistorikkinnslagRepository historikkRepository = mock(HistorikkinnslagRepository.class);
    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);

    private FordelBeregningsgrunnlagHistorikkKalkulusTjeneste fordelBeregningsgrunnlagHistorikkTjeneste;

    @BeforeEach
    void setup() {
        fordelBeregningsgrunnlagHistorikkTjeneste = new FordelBeregningsgrunnlagHistorikkKalkulusTjeneste(arbeidsgiverHistorikkinnslagTjeneste, historikkRepository, inntektArbeidYtelseTjeneste);

        when(arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(any(), any(), any(), anyList())).thenReturn(ARBEIDSFORHOLDINFO);

        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(InntektArbeidYtelseGrunnlagBuilder.nytt().build());
    }

    @Test
    void skal_lage_tekstlinje_i_historikkinnslag() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsak.setId(25L);
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandling.setId(12L);

        var fom = LocalDate.of(2024, 2, 1);
        var tom = LocalDate.of(2024, 4, 22);
        var andelEndring = new BeregningsgrunnlagPrStatusOgAndelEndring(
            new BeløpEndring(null, BigDecimal.valueOf(2231)), new InntektskategoriEndring(null, Inntektskategori.ARBEIDSTAKER), new RefusjonEndring(null, BigDecimal.valueOf(5045)), AktivitetStatus.ARBEIDSTAKER, OpptjeningAktivitetType.ARBEID,
            Arbeidsgiver.virksomhet("999999999"), null);
        var perioder = List.of(new BeregningsgrunnlagPeriodeEndring(List.of(andelEndring),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)));
        var bge = new BeregningsgrunnlagEndring(perioder);
        var endringsaggregat = new OppdaterBeregningsgrunnlagResultat(bge, null, null, null, List.of());

        var dto = lagFordelBeregningsgrunnlagDto();
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);
        var resultat = fordelBeregningsgrunnlagHistorikkTjeneste.lagHistorikk(dto, Optional.of(endringsaggregat), param);
        var captor = ArgumentCaptor.forClass(Historikkinnslag.class);

        assertThat(resultat).isNotNull();
        verify(historikkRepository, times(1)).lagre(captor.capture());
        var historikkinnslag = captor.getValue();

        assertThat(historikkinnslag.getLinjer().stream().map(HistorikkinnslagLinje::getTekst)).satisfies(l -> {
            assertThat(l.get(0)).isEqualTo("Det er lagt til ny aktivitet for __" + ARBEIDSFORHOLDINFO + "__ gjeldende fra __01.02.2024__.");
            assertThat(l.get(1)).isEqualTo("__Inntekt__ er satt til __2 231 kr__.");
            assertThat(l.get(2)).isEqualTo("__Inntektskategori__ er satt til __Arbeidstaker__.");
        });
    }

    private FordelBeregningsgrunnlagDto lagFordelBeregningsgrunnlagDto() {
        var fom = LocalDate.of(2024, 2, 1);
        var tom = LocalDate.of(2024, 4, 22);

        var fastsattVerdier = new FordelFastsatteVerdierDto(5045, 2231, Inntektskategori.ARBEIDSTAKER);
        var fordelBeregningsgrunnlagAndelDto = new FordelBeregningsgrunnlagAndelDto(fastsattVerdier, Inntektskategori.ARBEIDSTAKER, 4045, 600000);
        fordelBeregningsgrunnlagAndelDto.setNyAndel(true);

        var fordelBeregningsgrunnlagPeriodeDto = new FordelBeregningsgrunnlagPeriodeDto(
                List.of(fordelBeregningsgrunnlagAndelDto), fom, tom);
        return new FordelBeregningsgrunnlagDto(Collections.singletonList(fordelBeregningsgrunnlagPeriodeDto), "Test testesen");
    }
}
