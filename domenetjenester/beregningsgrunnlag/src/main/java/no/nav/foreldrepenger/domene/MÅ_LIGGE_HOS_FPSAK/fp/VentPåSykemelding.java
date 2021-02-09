package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp;

import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class VentPåSykemelding {
    private static final List<Arbeidskategori> ARBEIDSKATEGORI_DAGPENGER = List.of(Arbeidskategori.DAGPENGER, Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER);


    public static Optional<LocalDate> utledVenteFrist(YtelseFilter filter, LocalDate skjæringstidspunkt, LocalDate dagensDato) {
        Collection<Ytelse> løpendeSykepenger = filter.før(skjæringstidspunkt).filter(y -> RelatertYtelseTilstand.LØPENDE.equals(y.getStatus())
            && RelatertYtelseType.SYKEPENGER.equals(y.getRelatertYtelseType())).getFiltrertYtelser();

        List<Ytelse> løpendeSPBasertPåDagpenger = løpendeSykepenger.stream()
            .filter(yt -> erBasertPåDagpenger(yt.getYtelseGrunnlag()))
            .collect(Collectors.toList());

        boolean finnesNødvendigSykemelding = finnesSykemeldingPåSkjæringstidspunktet(løpendeSPBasertPåDagpenger, skjæringstidspunkt);
        return finnesNødvendigSykemelding ? Optional.empty() : utledFrist(skjæringstidspunkt, dagensDato);

    }

    private static Optional<LocalDate> utledFrist(LocalDate skjæringstidspunktOpptjening, LocalDate dagensdato) {
        if (!dagensdato.isAfter(skjæringstidspunktOpptjening)) {
            return Optional.of(skjæringstidspunktOpptjening.plusDays(1));
        }
        if (!dagensdato.isAfter(skjæringstidspunktOpptjening.plusDays(14))) {
            return Optional.of(dagensdato.plusDays(1));
        }
        return Optional.empty();
    }

    private static boolean finnesSykemeldingPåSkjæringstidspunktet(List<Ytelse> løpendeSPBasertPåDagpenger, LocalDate skjæringstidspunkt) {
        return løpendeSPBasertPåDagpenger.stream()
            .map(Ytelse::getYtelseAnvist)
            .flatMap(Collection::stream)
            .anyMatch(ya -> ya.getAnvistFOM().isBefore(skjæringstidspunkt) && !ya.getAnvistTOM().isBefore(skjæringstidspunkt));
    }

    private static boolean erBasertPåDagpenger(Optional<YtelseGrunnlag> ytelseGrunnlag) {
        return ytelseGrunnlag.map(yg -> ARBEIDSKATEGORI_DAGPENGER.contains(yg.getArbeidskategori().orElse(Arbeidskategori.UDEFINERT)))
            .orElse(false);
    }
}
