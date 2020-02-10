package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.AA_REGISTER_TYPER;
import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.domene.arbeidsforhold.dto.PermisjonDto;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;

final class UtledPermisjonSomFørerTilAksjonspunkt {

    private static final int PERMISJON_PROSENTSATS_NØDVENDIG_FOR_Å_UTLØSTE_AKSJONSPUNKT = 100;

    private static final Set<PermisjonsbeskrivelseType> PERMISJONTYPER_SOM_ER_URELEVANT_FOR_5080 = Set.of(
        PermisjonsbeskrivelseType.UTDANNINGSPERMISJON,
        PermisjonsbeskrivelseType.PERMISJON_MED_FORELDREPENGER
    );

    private UtledPermisjonSomFørerTilAksjonspunkt(){
        // Skjul default constructor
    }

    static List<PermisjonDto> utled(YrkesaktivitetFilter filter, Yrkesaktivitet yrkesaktiviteter, LocalDate stp){
        return utled(filter, List.of(yrkesaktiviteter), stp);
    }

    static List<PermisjonDto> utled(YrkesaktivitetFilter filter, Collection<Yrkesaktivitet> yrkesaktiviteter, LocalDate stp){
        return yrkesaktiviteter.stream()
            .filter(ya -> AA_REGISTER_TYPER.contains(ya.getArbeidType()))
            .filter(ya -> harAnsettelsesPerioderSomInkludererStp(filter, stp, ya))
            .map(Yrkesaktivitet::getPermisjon)
            .flatMap(Collection::stream)
            .filter(UtledPermisjonSomFørerTilAksjonspunkt::har100ProsentPermisjonEllerMer)
            .filter(p -> fomErFørStp(stp, p) && tomErLikEllerEtterStp(stp, p))
            .filter(p -> !PERMISJONTYPER_SOM_ER_URELEVANT_FOR_5080.contains(p.getPermisjonsbeskrivelseType()))
            .map(UtledPermisjonSomFørerTilAksjonspunkt::byggPermisjonDto)
            .collect(Collectors.toList());
    }

    private static boolean fomErFørStp(LocalDate stp, Permisjon p) {
        return p.getFraOgMed().isBefore(stp);
    }

    private static boolean tomErLikEllerEtterStp(LocalDate stp, Permisjon p) {
        return p.getTilOgMed() == null || p.getTilOgMed().isAfter(stp) || p.getTilOgMed().isEqual(stp);
    }

    private static boolean har100ProsentPermisjonEllerMer(Permisjon p) {
        return PERMISJON_PROSENTSATS_NØDVENDIG_FOR_Å_UTLØSTE_AKSJONSPUNKT <= p.getProsentsats().getVerdi().intValue();
    }

    private static boolean harAnsettelsesPerioderSomInkludererStp(YrkesaktivitetFilter filter, LocalDate stp, Yrkesaktivitet ya) {
        return filter.getAnsettelsesPerioder(ya).stream().anyMatch(avtale -> avtale.getPeriode().inkluderer(stp));
    }

    private static PermisjonDto byggPermisjonDto(Permisjon permisjon) {
        return new PermisjonDto(
            permisjon.getFraOgMed(),
            permisjon.getTilOgMed() == null || TIDENES_ENDE.equals(permisjon.getTilOgMed()) ? null : permisjon.getTilOgMed(),
            permisjon.getProsentsats().getVerdi(),
            permisjon.getPermisjonsbeskrivelseType()
        );
    }
}
