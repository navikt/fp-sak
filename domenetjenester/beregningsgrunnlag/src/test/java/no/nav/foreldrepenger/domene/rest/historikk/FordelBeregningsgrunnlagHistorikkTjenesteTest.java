package no.nav.foreldrepenger.domene.rest.historikk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

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
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelFastsatteVerdierDto;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;

class FordelBeregningsgrunnlagHistorikkTjenesteTest {
    private static final String ARBEIDSFORHOLDINFO = "DYNAMISK OPPSTEMT HAMSTER KF (311343483)";
    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private final Beløp GRUNNBELØP = new Beløp(600000);

    private final HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste = mock(HentOgLagreBeregningsgrunnlagTjeneste.class);
    private final ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste = mock(ArbeidsgiverHistorikkinnslag.class);
    private final HistorikkinnslagRepository historikkRepository = mock(HistorikkinnslagRepository.class);
    private final InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste = mock(InntektArbeidYtelseTjeneste.class);

    private FordelBeregningsgrunnlagHistorikkTjeneste fordelBeregningsgrunnlagHistorikkTjeneste;

    @BeforeEach
    void setup() {
        fordelBeregningsgrunnlagHistorikkTjeneste = new FordelBeregningsgrunnlagHistorikkTjeneste(beregningsgrunnlagTjeneste,
                arbeidsgiverHistorikkinnslagTjeneste, inntektArbeidYtelseTjeneste, historikkRepository);

        when(arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(any(), any(), any(), anyList())).thenReturn(ARBEIDSFORHOLDINFO);

        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(anyLong())).thenReturn(lagBeregningsgrunnlag());

        when(inntektArbeidYtelseTjeneste.hentGrunnlag(anyLong())).thenReturn(InntektArbeidYtelseGrunnlagBuilder.nytt().build());
    }

    @Test
    void skal_lage_tekstlinje_i_historikkinnslag() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsak.setId(25L);
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandling.setId(12L);

        var dto = lagFordelBeregningsgrunnlagDto();
        var param = new AksjonspunktOppdaterParameter(BehandlingReferanse.fra(behandling), dto);
        var resultat = fordelBeregningsgrunnlagHistorikkTjeneste.lagHistorikk(dto, param);
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

    private BeregningsgrunnlagEntitet lagBeregningsgrunnlag() {
        var fom = LocalDate.of(2024, 2, 1);
        var tom = LocalDate.of(2024, 4, 22);

        return BeregningsgrunnlagEntitet.ny()
                .medGrunnbeløp(GRUNNBELØP)
                .medSkjæringstidspunkt(SKJÆRINGSTIDSPUNKT)
                .leggTilBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode.ny().medBeregningsgrunnlagPeriode(fom, tom))
                .build();
    }

}
