package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.AA_REGISTER_TYPER;
import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.PermisjonOgMangelDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.AksjonspunktÅrsak;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;

public class HåndterePermisjoner {
    private static final int PERMISJON_PROSENTSATS_NØDVENDIG_FOR_Å_UTLØSTE_AKSJONSPUNKT = 100;
    private static final Set<PermisjonsbeskrivelseType> PERMISJONTYPER_SOM_IKKE_ER_RELEVANTE = Set.of(
        PermisjonsbeskrivelseType.UTDANNINGSPERMISJON,
        PermisjonsbeskrivelseType.PERMISJON_MED_FORELDREPENGER);

    public static List<ArbeidsforholdMangel> finnArbForholdMedPermisjonUtenSluttdatoMangel(BehandlingReferanse behandlingReferanse,
                                                                                           InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<ArbeidsforholdMangel> arbForholdMedPermUtenSluttdato = new ArrayList<>();

        var stp = behandlingReferanse.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt();
        var aktørId = behandlingReferanse.aktørId();
        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId)).før(stp);

        for (var ya : filter.getYrkesaktiviteter()) {
            boolean harArbeidsforholdetPermisjonUtenSluttdato = harArbeidsforholdetPermisjonUtenSluttdato(filter, List.of(ya), stp);
            if (harArbeidsforholdetPermisjonUtenSluttdato) {
                arbForholdMedPermUtenSluttdato.add(new ArbeidsforholdMangel(ya.getArbeidsgiver(), ya.getArbeidsforholdRef(), AksjonspunktÅrsak.PERMISJON_UTEN_SLUTTDATO));
            }
        }
        return arbForholdMedPermUtenSluttdato;
    }

    static boolean harArbeidsforholdetPermisjonUtenSluttdato(YrkesaktivitetFilter filter, Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate stp) {
        return yrkesaktiviteter.stream()
            .filter(ya -> AA_REGISTER_TYPER.contains(ya.getArbeidType()))
            .filter(ya -> harAnsettelsesPerioderSomInkludererStp(filter, stp, ya))
            .map(Yrkesaktivitet::getPermisjon)
            .flatMap(Collection::stream)
            .filter(HåndterePermisjoner::har100ProsentPermisjonEllerMer)
            .filter(p -> fomErFørStp(stp, p) && tomErLikEllerEtterStp(stp, p))
            .filter(p -> !PERMISJONTYPER_SOM_IKKE_ER_RELEVANTE.contains(p.getPermisjonsbeskrivelseType()))
            .anyMatch(p-> p.getTilOgMed() == null || TIDENES_ENDE.equals(p.getTilOgMed()));
    }

    public static boolean harRelevantPermisjonSomOverlapperSkjæringstidspunkt(Yrkesaktivitet yrkesaktivitet, LocalDate stp) {
        return yrkesaktivitet.getPermisjon().stream()
            .filter(HåndterePermisjoner::har100ProsentPermisjonEllerMer)
            .filter(p -> fomErFørStp(stp, p) && tomErLikEllerEtterStp(stp, p))
            .anyMatch(p -> !PERMISJONTYPER_SOM_IKKE_ER_RELEVANTE.contains(p.getPermisjonsbeskrivelseType()));
    }



    public static Optional<PermisjonOgMangelDto> hentPermisjonOgMangel(Yrkesaktivitet yrkesaktivitet, LocalDate stp, AksjonspunktÅrsak årsak, BekreftetPermisjonStatus status) {
        return yrkesaktivitet.getPermisjon().stream()
            .filter(HåndterePermisjoner::har100ProsentPermisjonEllerMer)
            .filter(p -> fomErFørStp(stp, p) && tomErLikEllerEtterStp(stp, p))
            .filter(p -> !PERMISJONTYPER_SOM_IKKE_ER_RELEVANTE.contains(p.getPermisjonsbeskrivelseType()))
            .max(Comparator.comparing(Permisjon::getPeriode))
            .map(p -> byggPermisjonOgMangelDto(p, årsak, status ));
    }

    private static boolean harAnsettelsesPerioderSomInkludererStp(YrkesaktivitetFilter filter, LocalDate stp, Yrkesaktivitet ya) {
        return filter.getAnsettelsesPerioder(ya).stream().anyMatch(avtale -> avtale.getPeriode().inkluderer(stp));
    }
    private static PermisjonOgMangelDto byggPermisjonOgMangelDto(Permisjon permisjon, AksjonspunktÅrsak årsak, BekreftetPermisjonStatus bekreftetPermisjonStatus) {
        return new PermisjonOgMangelDto(
            permisjon.getFraOgMed(),
            (permisjon.getTilOgMed() == null) || TIDENES_ENDE.equals(permisjon.getTilOgMed()) ? null : permisjon.getTilOgMed(),
            permisjon.getPermisjonsbeskrivelseType(),
            årsak,
            bekreftetPermisjonStatus);
    }
    private static boolean har100ProsentPermisjonEllerMer(Permisjon p) {
        return PERMISJON_PROSENTSATS_NØDVENDIG_FOR_Å_UTLØSTE_AKSJONSPUNKT <= p.getProsentsats().getVerdi().intValue();
    }
    private static boolean fomErFørStp(LocalDate stp, Permisjon p) {
        return p.getFraOgMed().isBefore(stp);
    }
    private static boolean tomErLikEllerEtterStp(LocalDate stp, Permisjon p) {
        return (p.getTilOgMed() == null) || p.getTilOgMed().isAfter(stp) || p.getTilOgMed().isEqual(stp);
    }
}
