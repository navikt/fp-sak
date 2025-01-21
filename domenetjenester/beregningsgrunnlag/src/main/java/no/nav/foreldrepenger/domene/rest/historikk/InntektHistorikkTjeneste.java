package no.nav.foreldrepenger.domene.rest.historikk;

import static no.nav.foreldrepenger.domene.rest.historikk.FordelBeregningsgrunnlagHistorikkUtil.fraInntektskategori;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBeløp;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;

@ApplicationScoped
public class InntektHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    public InntektHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public InntektHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
    }

    public List<HistorikkinnslagLinjeBuilder> lagHistorikk(List<Lønnsendring> lønnsendringList,
                                                           InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        var arbeidsforholdOverstyringer = inntektArbeidYtelseGrunnlag.getArbeidsforholdOverstyringer();
        List<HistorikkinnslagLinjeBuilder> alleLinjer = new ArrayList<>();
        lønnsendringList.forEach(lønnsendring -> {
            var linjer = linjerForLønnsendring(lønnsendring, arbeidsforholdOverstyringer);
            alleLinjer.addAll(linjer);
        });
        return alleLinjer;
    }

    private List<HistorikkinnslagLinjeBuilder> linjerForLønnsendring(Lønnsendring lønnsendring,
                                                                     List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        var linjer = new ArrayList<HistorikkinnslagLinjeBuilder>();
        lagHistorikkForInntektEndring(lønnsendring, arbeidsforholdOverstyringer).ifPresent(linjer::add);
        lagInntektskategoriInnslagHvisEndret(lønnsendring).ifPresent(linjer::add);
        if (!linjer.isEmpty()) {
            linjer.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);
        }
        return linjer;
    }

    private Optional<HistorikkinnslagLinjeBuilder> lagHistorikkForInntektEndring(Lønnsendring lønnsendring,
                                                                                 List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        var nyArbeidsinntekt = lønnsendring.getNyArbeidsinntekt();
        var gammelArbeidsinntekt = lønnsendring.getGammelArbeidsinntekt();
        if (nyArbeidsinntekt != null && !nyArbeidsinntekt.equals(gammelArbeidsinntekt)) {
            var linjeBuilder = new HistorikkinnslagLinjeBuilder();
            if (AktivitetStatus.FRILANSER.equals(lønnsendring.getAktivitetStatus())) {
                linjeBuilder.fraTil("Frilansinntekt", HistorikkBeløp.of(gammelArbeidsinntekt), HistorikkBeløp.of(nyArbeidsinntekt));
            } else {
                var arbeidsforholdInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(
                    lønnsendring.getAktivitetStatus(), lønnsendring.getArbeidsgiver(), lønnsendring.getArbeidsforholdRef(),
                    arbeidsforholdOverstyringer);
                linjeBuilder.fraTil("Inntekt fra " + arbeidsforholdInfo, HistorikkBeløp.ofNullable(gammelArbeidsinntekt), HistorikkBeløp.of(nyArbeidsinntekt));
            }
            return Optional.of(linjeBuilder);
        }
        return Optional.empty();
    }

    private Optional<HistorikkinnslagLinjeBuilder> lagInntektskategoriInnslagHvisEndret(Lønnsendring endring) {
        var nyInntektskategori = endring.getNyInntektskategori();
        if (nyInntektskategori != null && !nyInntektskategori.equals(endring.getGammelInntektskategori())) {
            return Optional.of(new HistorikkinnslagLinjeBuilder().fraTil("Inntektskategori", null, fraInntektskategori(nyInntektskategori)));
        }
        return Optional.empty();
    }


}
