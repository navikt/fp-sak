package no.nav.foreldrepenger.domene.rest.historikk;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkBeløp;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.aksjonspunkt.BeregningsgrunnlagPeriodeEndring;
import no.nav.foreldrepenger.domene.modell.kodeverk.Inntektskategori;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.domene.rest.dto.fordeling.FordelBeregningsgrunnlagPeriodeDto;
import no.nav.foreldrepenger.domene.typer.AktørId;

public final class FordelBeregningsgrunnlagHistorikkUtil {

    public static Optional<Historikkinnslag.Builder> lagHistorikkInnslag(AksjonspunktOppdaterParameter param,
                                                                         List<HistorikkinnslagLinjeBuilder> linjeBuilder) {
        Historikkinnslag.Builder historikkinnslagBuilder = null;
        if (!linjeBuilder.isEmpty()) {
            historikkinnslagBuilder = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
                .medBehandlingId(param.getBehandlingId())
                .medFagsakId(param.getFagsakId())
                .medTittel(SkjermlenkeType.FAKTA_OM_FORDELING)
                .medLinjer(linjeBuilder);
        }
        return Optional.ofNullable(historikkinnslagBuilder);
    }

    public static BeregningsgrunnlagPeriodeEndring getKorrektPeriodeEndring(List<BeregningsgrunnlagPeriodeEndring> perioder,
                                                                            FordelBeregningsgrunnlagPeriodeDto endretPeriode) {
        return perioder.stream()
            .filter(periode -> periode.getPeriode().getFomDato().equals(endretPeriode.getFom()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Finner ikke periode"));
    }

    public static List<HistorikkinnslagLinjeBuilder> leggTilArbeidsforholdHistorikkinnslag(Lønnsendring endring,
                                                                                           LocalDate korrektPeriodeFom,
                                                                                           String arbeidsforholdInfo) {

        if (!harEndringSomGirHistorikk(endring)) {
            return new ArrayList<>();
        }
        List<HistorikkinnslagLinjeBuilder> linjeBuilder = new ArrayList<>();
        linjeBuilder.add(gjeldendeFraLinje(endring, arbeidsforholdInfo, korrektPeriodeFom));
        linjeBuilder.add(refusjonLinje(endring));
        linjeBuilder.add(fraTilEquals("Inntekt", HistorikkBeløp.ofNullable(endring.getGammelArbeidsinntektPrÅr()),
            HistorikkBeløp.ofNullable(endring.getNyArbeidsinntektPrÅr())));
        linjeBuilder.add(inntektskategoriLinje(endring));

        linjeBuilder.add(HistorikkinnslagLinjeBuilder.LINJESKIFT);

        return linjeBuilder;
    }

    public static String fraInntektskategori(Inntektskategori inntektskategori) {
        return switch (inntektskategori) {
            case ARBEIDSTAKER -> "Arbeidstaker";
            case FRILANSER -> "Frilanser";
            case SELVSTENDIG_NÆRINGSDRIVENDE -> "Selvstendig næringsdrivende";
            case DAGPENGER -> "Dagpenger";
            case ARBEIDSAVKLARINGSPENGER -> "Arbeidsavklaringspenger";
            case SJØMANN -> "Arbeidstaker - Sjømann";
            case DAGMAMMA -> "Selvstendig næringsdrivende - Dagmamma";
            case JORDBRUKER -> "Selvstendig næringsdrivende - Jordbruker";
            case FISKER -> "Selvstendig næringsdrivende - Fisker";
            case ARBEIDSTAKER_UTEN_FERIEPENGER -> "Arbeidstaker uten feriepenger";
            case UDEFINERT -> throw new IllegalStateException("Unexpected value inntektskategori: " + inntektskategori);
        };
    }

    private static HistorikkinnslagLinjeBuilder inntektskategoriLinje(Lønnsendring endring) {
        var nyInntektskategori = endring.getNyInntektskategori();
        if (nyInntektskategori != null) {
            return fraTilEquals("Inntektskategori", null, fraInntektskategori(nyInntektskategori));
        }
        return null;
    }

    private static HistorikkinnslagLinjeBuilder refusjonLinje(Lønnsendring endring) {
        if (endring.getNyTotalRefusjonPrÅr() != null && endring.getArbeidsgiver().isPresent() && endring.getArbeidsforholdRef().isPresent()) {
            var forrigeRefusjon = endring.getGammelRefusjonPrÅr();
            if (!endring.getNyTotalRefusjonPrÅr().equals(forrigeRefusjon)) {

                return fraTilEquals("Nytt refusjonskrav", HistorikkBeløp.ofNullable(forrigeRefusjon),
                    HistorikkBeløp.of(endring.getNyTotalRefusjonPrÅr()));
            }
        }
        return null;
    }

    private static HistorikkinnslagLinjeBuilder gjeldendeFraLinje(Lønnsendring endring, String arbeidsforholdInfo, LocalDate dato) {
        var endretFeltTekst = new HistorikkinnslagLinjeBuilder().tekst(endring.isNyAndel() ? "Det er lagt til ny aktivitet for" : "Ny fordeling")
            .bold(arbeidsforholdInfo);

        return endretFeltTekst.tekst("gjeldende fra").bold(dato);
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
