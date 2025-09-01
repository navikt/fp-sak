package no.nav.foreldrepenger.domene.prosess;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

@ApplicationScoped
public class FullAapAnnenStatusLoggTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FullAapAnnenStatusLoggTjeneste.class);

    private BeregningTjeneste beregningTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    public FullAapAnnenStatusLoggTjeneste() {
    }

    @Inject
    public FullAapAnnenStatusLoggTjeneste(BeregningTjeneste beregningTjeneste,
                                          InntektArbeidYtelseTjeneste iayTjeneste) {
        this.beregningTjeneste = beregningTjeneste;
        this.iayTjeneste = iayTjeneste;
    }

    public void loggVedFullAapOgAnnenStatus(BehandlingReferanse referanse) {
        var beregningsgrunnlag = beregningTjeneste.hent(referanse).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
        beregningsgrunnlag.ifPresent(bg -> {
            if (harAapOgAnnenStatus(bg)) {
                if (harFullAap(referanse, bg.getSkjæringstidspunkt())) {
                    LOG.info("FULL_AAP_LOGG: Saksnummer {} mottar full AAP i kombinasjon med annen status i beregningsgrunnlaget",
                        referanse.saksnummer());
                }
            }
        });
    }

    private boolean harFullAap(BehandlingReferanse referanse, LocalDate skjæringstidspunkt) {
        var iayGrunnlag = iayTjeneste.finnGrunnlag(referanse.behandlingId());
        var ytelser = iayGrunnlag.flatMap(gr -> gr.getAktørYtelseFraRegister(referanse.aktørId())).map(AktørYtelse::getAlleYtelser).orElse(List.of());
        var filter = new YtelseFilter(ytelser, skjæringstidspunkt, true);
        var aapVedtak = filter.filter(yt -> yt.getKilde().equals(Fagsystem.ARENA) && yt.getRelatertYtelseType().equals(RelatertYtelseType.ARBEIDSAVKLARINGSPENGER)).getFiltrertYtelser();
        var utbetalingsprosentSisteMK = aapVedtak.stream()
            .map(Ytelse::getYtelseAnvist)
            .flatMap(Collection::stream)
            .filter(mk -> !mk.getAnvistTOM().isAfter(skjæringstidspunkt))
            .max(Comparator.comparing(YtelseAnvist::getAnvistTOM))
            .flatMap(YtelseAnvist::getUtbetalingsgradProsent);
        return utbetalingsprosentSisteMK.filter(up -> up.compareTo(new Stillingsprosent(200)) == 0).isPresent();
    }

    private boolean harAapOgAnnenStatus(Beregningsgrunnlag bg) {
        var erAap = bg.getAktivitetStatuser().stream().anyMatch(s -> s.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER));
        var harAnnenStatus = bg.getAktivitetStatuser().size() > 1;
        return erAap && harAnnenStatus;
    }
}
