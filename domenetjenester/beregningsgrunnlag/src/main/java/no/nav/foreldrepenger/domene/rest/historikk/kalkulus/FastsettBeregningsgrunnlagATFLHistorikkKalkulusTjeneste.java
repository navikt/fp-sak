package no.nav.foreldrepenger.domene.rest.historikk.kalkulus;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBelop;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPrStatusOgAndelEndring;
import no.nav.foreldrepenger.domene.aksjonspunkt.OppdaterBeregningsgrunnlagResultat;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.rest.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.foreldrepenger.domene.rest.dto.InntektPrAndelDto;
import no.nav.foreldrepenger.domene.rest.historikk.ArbeidsgiverHistorikkinnslag;

@ApplicationScoped
public class FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private HistorikkinnslagRepository historikkRepo;

    FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste() {
        // CDI
    }

    @Inject
    public FastsettBeregningsgrunnlagATFLHistorikkKalkulusTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste,
                                                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                                   HistorikkinnslagRepository historikkRepo) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.historikkRepo = historikkRepo;
    }

    /**
     * For bergninger som skjer i fp-kalkulus
     * @param param
     * @param dto
     * @param oppdaterBeregningsgrunnlagResultat
     */
    public void lagHistorikk(AksjonspunktOppdaterParameter param,
                             FastsettBeregningsgrunnlagATFLDto dto,
                             OppdaterBeregningsgrunnlagResultat oppdaterBeregningsgrunnlagResultat) {
        var bgPerioder = oppdaterBeregningsgrunnlagResultat.getBeregningsgrunnlagEndring()
            .map(BeregningsgrunnlagEndring::getBeregningsgrunnlagPeriodeEndringer)
            .orElse(Collections.emptyList());
        var andelerIFørstePeriode = bgPerioder.stream()
            .min(Comparator.comparing(bgp -> bgp.getPeriode().getFom()))
            .map(BeregningsgrunnlagPeriodeEndring::getBeregningsgrunnlagPrStatusOgAndelEndringer)
            .orElse(Collections.emptyList());

        var atAndeler = andelerIFørstePeriode.stream().filter(a -> a.getAktivitetStatus().erArbeidstaker()).toList();
        var flAndeler = andelerIFørstePeriode.stream().filter(a -> a.getAktivitetStatus().erFrilanser()).toList();

        var historikkBuilder = new Historikkinnslag.Builder();

        oppdaterEndringer(param.getBehandlingId(), dto.getInntektPrAndelList(), atAndeler, flAndeler,
            dto.getInntektFrilanser(), historikkBuilder);

        ferdigStillHistorikkInnslag(dto, param, historikkBuilder);
    }

    private void oppdaterEndringer(Long behandlingId,
                                   List<InntektPrAndelDto> inntektPrAndelList,
                                   List<BeregningsgrunnlagPrStatusOgAndelEndring> atAndeler,
                                   List<BeregningsgrunnlagPrStatusOgAndelEndring> flAndeler,
                                   Integer inntektFrilanser,
                                   Historikkinnslag.Builder historikkBuilder) {
        if (atAndeler.stream()
            .noneMatch(bgpsa -> bgpsa.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))) {
            historikkBuilder.addLinje("Grunnlag for beregnet årsinntekt:");
        }

        if (inntektFrilanser != null && !flAndeler.isEmpty()) {
            historikkBuilder.addLinje(HistorikkinnslagLinjeBuilder.fraTilEquals("Frilansinntekt", null, HistorikkBelop.valueOf(inntektFrilanser)));
        }

        if (inntektPrAndelList != null) {
            oppdaterEndringVedOverstyrt(behandlingId, atAndeler, historikkBuilder);
        }
    }

    /**
     * For bergninger som skjer i fp-sak
     *
     * @param dto
     * @param param
     * @param historikkBuilder
     */

    private void ferdigStillHistorikkInnslag(FastsettBeregningsgrunnlagATFLDto dto, AksjonspunktOppdaterParameter param,
                                             Historikkinnslag.Builder historikkBuilder) {
        var ref = param.getRef();
        historikkBuilder.addLinje(dto.getBegrunnelse())
            .medTittel(SkjermlenkeType.BEREGNING_FORELDREPENGER)
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(ref.fagsakId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER);
        historikkRepo.lagre(historikkBuilder.build());

    }

    private void oppdaterEndringVedOverstyrt(Long behandlingId,
                                             List<BeregningsgrunnlagPrStatusOgAndelEndring> arbeidstakerList,
                                             Historikkinnslag.Builder historikkBuilder) {
        var arbeidsforholOverstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingId)
            .getArbeidsforholdOverstyringer();
        for (var endretAndel : arbeidstakerList) {
            if (endretAndel != null) {
                endretAndel.getInntektEndring().ifPresent(inntektEndring -> {
                    var visningsNavn = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
                        endretAndel.getAktivitetStatus(), endretAndel.getArbeidsgiver(), Optional.ofNullable(endretAndel.getArbeidsforholdRef()),
                        arbeidsforholOverstyringer);
                    var fra = inntektEndring.fraBeløp();
                    var til = inntektEndring.tilBeløp();
                    if (fra == null || fra.compareTo(til) != 0) {
                        var textBuilder = new HistorikkinnslagLinjeBuilder();
                        historikkBuilder.addLinje(textBuilder.fraTil(String.format("Inntekt fra %s", visningsNavn), HistorikkBelop.valueOf(fra), HistorikkBelop.valueOf(til)));
                    }
                });
            }
        }
    }

}
