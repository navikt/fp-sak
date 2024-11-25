package no.nav.foreldrepenger.domene.rest.historikk;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.*;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.domene.typer.AktørId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstBuilderFormater.formatDate;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstBuilderFormater.formatString;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagTekstlinjeBuilder.fraTilEquals;

public final class FordelBeregningsgrunnlagHistorikkUtil {

    public static Optional<Historikkinnslag2.Builder> lagHistorikkInnslag(AksjonspunktOppdaterParameter param,
                                                                          List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder) {
        Historikkinnslag2.Builder historikkinnslagBuilder = null;
        if (!tekstlinjerBuilder.isEmpty()) {
            historikkinnslagBuilder = new Historikkinnslag2.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medBehandlingId(param.getBehandlingId())
                .medFagsakId(param.getRef().fagsakId())
                .medTittel(SkjermlenkeType.FAKTA_OM_FORDELING)
                .medTekstlinjer(tekstlinjerBuilder);
        }
        return Optional.ofNullable(historikkinnslagBuilder);
    }

    public static BeregningsgrunnlagPeriode getKorrektPeriode(List<BeregningsgrunnlagPeriode> perioder,
                                                              FordelBeregningsgrunnlagPeriodeDto endretPeriode) {
        return perioder.stream()
            .filter(periode -> periode.getBeregningsgrunnlagPeriodeFom().equals(endretPeriode.getFom()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Finner ikke periode"));
    }

    public static BeregningsgrunnlagPeriodeEndring getKorrektPeriodeEndring(List<BeregningsgrunnlagPeriodeEndring> perioder,
                                                                            FordelBeregningsgrunnlagPeriodeDto endretPeriode) {
        return perioder.stream()
            .filter(periode -> periode.getPeriode().getFom().equals(endretPeriode.getFom()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Finner ikke periode"));
    }

    public static List<HistorikkinnslagTekstlinjeBuilder> leggTilArbeidsforholdHistorikkinnslag(Lønnsendring endring,
                                                                                                LocalDate korrektPeriodeFom,
                                                                                                String arbeidsforholdInfo) {

        if (!harEndringSomGirHistorikk(endring)) {
            return new ArrayList<>();
        }
        var endretFeltType = finnEndretFeltType(endring);
        List<HistorikkinnslagTekstlinjeBuilder> tekstlinjerBuilder = new ArrayList<>();
        tekstlinjerBuilder.add(gjeldendeFraTekstlinje(endretFeltType, arbeidsforholdInfo, korrektPeriodeFom));
        tekstlinjerBuilder.add(refusjonTekstlinje(endring));
        tekstlinjerBuilder.add(
            fraTilEquals(HistorikkEndretFeltType.INNTEKT.getNavn(), endring.getGammelArbeidsinntektPrÅr(), endring.getNyArbeidsinntektPrÅr()));
        tekstlinjerBuilder.add(inntektskategoriTekstlinje(endring));

        tekstlinjerBuilder.add(new HistorikkinnslagTekstlinjeBuilder().linjeskift());

        return tekstlinjerBuilder;
    }

    private static HistorikkinnslagTekstlinjeBuilder inntektskategoriTekstlinje(Lønnsendring endring) {
        var nyInntektskategori = endring.getNyInntektskategori();
        if (nyInntektskategori != null) {
            return fraTilEquals(HistorikkEndretFeltType.INNTEKTSKATEGORI.getNavn(), null, nyInntektskategori);
        }
        return null;
    }

    private static HistorikkinnslagTekstlinjeBuilder refusjonTekstlinje(Lønnsendring endring) {
        if (endring.getNyTotalRefusjonPrÅr() != null && endring.getArbeidsgiver().isPresent() && endring.getArbeidsforholdRef().isPresent()) {
            var forrigeRefusjon = endring.getGammelRefusjonPrÅr();
            if (!endring.getNyTotalRefusjonPrÅr().equals(forrigeRefusjon)) {

                return new HistorikkinnslagTekstlinjeBuilder().bold(HistorikkEndretFeltType.NYTT_REFUSJONSKRAV.getNavn())
                    .tekst("endret fra " + BigDecimal.valueOf(forrigeRefusjon) + " til ")
                    .bold(formatString(endring.getNyTotalRefusjonPrÅr()));
            }
        }
        return null;
    }

    private static HistorikkinnslagTekstlinjeBuilder gjeldendeFraTekstlinje(HistorikkEndretFeltType endretFeltType,
                                                                            String arbeidsforholdInfo,
                                                                            LocalDate dato) {
        var tekstNyFordeling = new HistorikkinnslagTekstlinjeBuilder().tekst("Ny fordeling").bold(arbeidsforholdInfo);
        var tekstNyAktivitet = new HistorikkinnslagTekstlinjeBuilder().tekst("Det er lagt til ny aktivitet for").bold(arbeidsforholdInfo);
        var endretFeltTekst = HistorikkEndretFeltType.NY_FORDELING.equals(endretFeltType) ? tekstNyFordeling : tekstNyAktivitet;

        return endretFeltTekst.tekst(HistorikkinnslagFeltType.GJELDENDE_FRA.getNavn()).bold(formatDate(dato));
    }


    private static HistorikkEndretFeltType finnEndretFeltType(Lønnsendring endring) {
        return endring.isNyAndel() ? HistorikkEndretFeltType.NY_AKTIVITET : HistorikkEndretFeltType.NY_FORDELING;
    }

    private static boolean harEndringSomGirHistorikk(Lønnsendring endring) {
        var harEndringIRefusjon =
            endring.getNyTotalRefusjonPrÅr() != null && !endring.getNyTotalRefusjonPrÅr().equals(endring.getGammelRefusjonPrÅr());
        var harEndringIInntektskategori =
            endring.getNyInntektskategori() != null && !endring.getNyInntektskategori().equals(endring.getGammelInntektskategori());
        var harEndringIInntekt =
            endring.getGammelArbeidsinntekt() == null || !endring.getGammelArbeidsinntekt().equals(endring.getNyArbeidsinntektPrÅr());
        return harEndringIInntekt || harEndringIRefusjon || harEndringIInntektskategori || endring.isNyAndel();
    }

    public static Lønnsendring.Builder lagEndringsoppsummeringForHistorikk(FordelBeregningsgrunnlagAndelDto endretAndel) {
        var fastsatteVerdier = endretAndel.getFastsatteVerdier();
        var endring = new Lønnsendring.Builder().medAktivitetStatus(endretAndel.getAktivitetStatus())
            .medNyInntektskategori(fastsatteVerdier.getInntektskategori())
            .medNyArbeidsinntektPrÅr(fastsatteVerdier.getFastsattÅrsbeløpInklNaturalytelse())
            .medNyAndel(endretAndel.getNyAndel());
        if (gjelderArbeidsforhold(endretAndel)) {
            settArbeidsforholdVerdier(endretAndel, endring);
        }
        if (!endretAndel.getNyAndel()) {
            settEndretFraVerdier(endretAndel, endring);
            endring.medNyTotalRefusjonPrÅr(fastsatteVerdier.getRefusjonPrÅr());
        }
        return endring;
    }

    private static boolean gjelderArbeidsforhold(FordelBeregningsgrunnlagAndelDto endretAndel) {
        return endretAndel.getArbeidsgiverId() != null;
    }


    private static void settArbeidsforholdVerdier(FordelBeregningsgrunnlagAndelDto endretAndel, Lønnsendring.Builder endring) {
        endring.medArbeidsforholdRef(endretAndel.getArbeidsforholdId()).medArbeidsgiver(finnArbeidsgiver(endretAndel));
    }

    private static Arbeidsgiver finnArbeidsgiver(FordelBeregningsgrunnlagAndelDto endretAndel) {
        Arbeidsgiver arbeidsgiver;
        if (OrgNummer.erGyldigOrgnr(endretAndel.getArbeidsgiverId())) {
            arbeidsgiver = Arbeidsgiver.virksomhet(endretAndel.getArbeidsgiverId());
        } else {
            arbeidsgiver = Arbeidsgiver.person(new AktørId(endretAndel.getArbeidsgiverId()));
        }
        return arbeidsgiver;
    }

    private static void settEndretFraVerdier(FordelBeregningsgrunnlagAndelDto endretAndel, Lønnsendring.Builder endring) {
        endring.medGammelArbeidsinntektPrÅr(endretAndel.getForrigeArbeidsinntektPrÅr())
            .medGammelInntektskategori(endretAndel.getForrigeInntektskategori())
            .medGammelRefusjonPrÅr(endretAndel.getForrigeRefusjonPrÅr());
    }
}
