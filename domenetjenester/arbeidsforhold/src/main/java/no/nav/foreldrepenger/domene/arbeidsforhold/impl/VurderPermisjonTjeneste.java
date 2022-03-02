package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdMangel;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.PermisjonDto;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;

public final class VurderPermisjonTjeneste {

    private VurderPermisjonTjeneste() {
        // Skjul default constructor
    }

    /**
     * Legger til arbeidsforhold i resultatet hvor permisjonen er relevant
     *
     * @param behandlingReferanse Referanse til behandlingen
     * @param result              Map av arbeidsgivere med et set av referanser til
     *                            arbeidsforhold, arbeidsforhold blir lagt til her
     * @param grunnlag            Inntekt, arbeids, og ytelse grunnlaget
     */
    public static void leggTilArbeidsforholdMedRelevantPermisjon(BehandlingReferanse behandlingReferanse,
            Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result,
            InntektArbeidYtelseGrunnlag grunnlag) {
        var stp = behandlingReferanse.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        var aktørId = behandlingReferanse.getAktørId();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId)).før(stp);

        for (var ya : filter.getYrkesaktiviteter()) {
            Collection<PermisjonDto> utledetPermisjoner = UtledPermisjonSomFørerTilAksjonspunkt.utled(filter, ya, stp);
            if (utledetPermisjoner.isEmpty()) {
                continue;
            }
            if (harAlleredeTattStillingTilPermisjon(grunnlag, ya, utledetPermisjoner)) {
                continue;
            }
            LeggTilResultat.leggTil(result, AksjonspunktÅrsak.PERMISJON, ya.getArbeidsgiver(), Set.of(ya.getArbeidsforholdRef()));
        }
    }

    public static List<ArbeidsforholdMangel> finnArbForholdMedPermisjonUtenSluttdatoMangel(BehandlingReferanse behandlingReferanse,
                                                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<ArbeidsforholdMangel> arbForholdMedPermUtenSluttdato = new ArrayList<>();

        var stp = behandlingReferanse.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        var aktørId = behandlingReferanse.getAktørId();
        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId)).før(stp);

        for (var ya : filter.getYrkesaktiviteter()) {
            boolean harArbeidsforholdetPermisjonUtenSluttdato = UtledPermisjonSomFørerTilAksjonspunkt.harArbeidsforholdetPermisjonUtenSluttdato(filter, List.of(ya), stp);
            if (harArbeidsforholdetPermisjonUtenSluttdato) {
                arbForholdMedPermUtenSluttdato.add(new ArbeidsforholdMangel(ya.getArbeidsgiver(), ya.getArbeidsforholdRef(), AksjonspunktÅrsak.PERMISJON_UTEN_SLUTTDATO));
            }
        }
        return arbForholdMedPermUtenSluttdato;
    }

    private static boolean harAlleredeTattStillingTilPermisjon(InntektArbeidYtelseGrunnlag grunnlag,
            Yrkesaktivitet ya,
            Collection<PermisjonDto> utledetPermisjoner) {
        return grunnlag.getArbeidsforholdOverstyringer().stream()
                .filter(ov -> gjelderSammeArbeidsforhold(ya, ov))
                .anyMatch(ov -> harFortsattUgyldigePerioder(ov, utledetPermisjoner) || harSammePermisjonsperiode(ya, ov));
    }

    private static boolean harFortsattUgyldigePerioder(ArbeidsforholdOverstyring ov, Collection<PermisjonDto> utledetPermisjoner) {
        var bekreftetPermisjonOpt = ov.getBekreftetPermisjon();
        if (bekreftetPermisjonOpt.isPresent()) {
            var bekreftetPermisjon = bekreftetPermisjonOpt.get();
            return Objects.equals(bekreftetPermisjon.getStatus(), BekreftetPermisjonStatus.UGYLDIGE_PERIODER)
                    && (utledetPermisjoner.size() > 1);
        }
        return false;
    }

    private static boolean harSammePermisjonsperiode(Yrkesaktivitet ya, ArbeidsforholdOverstyring ov) {
        var bekreftetPermisjonOpt = ov.getBekreftetPermisjon();
        if (bekreftetPermisjonOpt.isPresent()) {
            var bekreftetPermisjon = bekreftetPermisjonOpt.get();
            return ya.getPermisjon().stream().anyMatch(permisjon -> harSammeFomOgTom(bekreftetPermisjon, permisjon));
        }
        return false;
    }

    private static boolean harSammeFomOgTom(BekreftetPermisjon bekreftetPermisjon, Permisjon permisjon) {
        var bekreftetPermisjonPeriode = bekreftetPermisjon.getPeriode();
        return (bekreftetPermisjonPeriode != null)
                && Objects.equals(permisjon.getFraOgMed(), bekreftetPermisjonPeriode.getFomDato())
                && Objects.equals(permisjon.getTilOgMed(), bekreftetPermisjonPeriode.getTomDato());
    }

    private static boolean gjelderSammeArbeidsforhold(Yrkesaktivitet ya, ArbeidsforholdOverstyring ov) {
        return ya.gjelderFor(ov.getArbeidsgiver(), ov.getArbeidsforholdRef());
    }
}
