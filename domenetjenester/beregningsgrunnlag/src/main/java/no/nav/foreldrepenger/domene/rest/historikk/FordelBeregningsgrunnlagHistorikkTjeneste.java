package no.nav.foreldrepenger.domene.rest.historikk;

import static no.nav.foreldrepenger.domene.rest.historikk.FordelBeregningsgrunnlagHistorikkUtil.lagEndringsoppsummeringForHistorikk;
import static no.nav.foreldrepenger.domene.rest.historikk.FordelBeregningsgrunnlagHistorikkUtil.leggTilArbeidsforholdHistorikkinnslag;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagPeriodeDto;

@ApplicationScoped
public class FordelBeregningsgrunnlagHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    FordelBeregningsgrunnlagHistorikkTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FordelBeregningsgrunnlagHistorikkTjeneste(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                     ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                     InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                     HistorikkinnslagRepository historikkinnslagRepository) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    public OppdateringResultat lagHistorikk(FordelBeregningsgrunnlagDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetAggregatForBehandling(behandlingId);
        var perioder = beregningsgrunnlag.getBeregningsgrunnlagPerioder();
        var arbeidsforholdOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId).getArbeidsforholdOverstyringer();
        List<HistorikkinnslagLinjeBuilder> linjerBuilder = new ArrayList<>();

        linjerBuilder.add(new HistorikkinnslagLinjeBuilder().tekst(dto.getBegrunnelse()));
        for (var endretPeriode : dto.getEndretBeregningsgrunnlagPerioder()) {
            var korrektPeriodeFom = FordelBeregningsgrunnlagHistorikkUtil.getKorrektPeriode(perioder, endretPeriode).getBeregningsgrunnlagPeriodeFom();
            linjerBuilder = lagHistorikk(endretPeriode, korrektPeriodeFom, arbeidsforholdOverstyringer);
        }

        var historikkinnslagBuilder = FordelBeregningsgrunnlagHistorikkUtil.lagHistorikkInnslag(param, linjerBuilder);
        historikkinnslagBuilder.ifPresent(builder -> historikkinnslagRepository.lagre(builder.build()));

        return OppdateringResultat.utenOverhopp();
    }

    private List<HistorikkinnslagLinjeBuilder> lagHistorikk(FordelBeregningsgrunnlagPeriodeDto endretPeriode,
                                                            LocalDate korrektPeriodeFom,
                                                            List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        List<HistorikkinnslagLinjeBuilder> linjerBuilder = new ArrayList<>();
        for (var endretAndel : endretPeriode.getAndeler()) {
            var endring = lagEndringsoppsummeringForHistorikk(endretAndel).build();
            var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(endring.getAktivitetStatus(),
                endring.getArbeidsgiver(), endring.getArbeidsforholdRef(), arbeidsforholdOverstyringer);
            linjerBuilder.addAll(leggTilArbeidsforholdHistorikkinnslag(endring, korrektPeriodeFom, arbeidsforholdInfo));
        }
        return linjerBuilder;
    }
}
