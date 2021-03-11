package no.nav.foreldrepenger.domene.fp;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;

public class VentPåSykemelding {
    private static final List<Arbeidskategori> ARBEIDSKATEGORI_DAGPENGER = List.of(Arbeidskategori.DAGPENGER,
        Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER);


    /**
     * Utleder om vi skal vente på å motta siste sykemelding fra søker dersom søker går på sykepenger som er løpende
     * og vi ikke har en sykemelding som krysser skjæringstidspunktet
     *
     * @param filter             ytelsefilter med alle søkers ytelser
     * @param skjæringstidspunkt skjæringstidspunkt for opptjening
     * @param dagensDato         dagens dato (tas inn som parameter for enklere test)
     * @return Returnerer tom Optional om vi ikke skal opprette vente punkt.
     * Returnerer en Optional med en LocalDate (frist vi skal vente til) dersom vi skal vente
     */
    public static Optional<LocalDate> utledVenteFrist(YtelseFilter filter,
                                                      LocalDate skjæringstidspunkt,
                                                      LocalDate dagensDato) {
        var løpendeSykepenger = filter.før(skjæringstidspunkt)
            .filter(y -> RelatertYtelseTilstand.LØPENDE.equals(y.getStatus()) && RelatertYtelseType.SYKEPENGER.equals(
                y.getRelatertYtelseType()))
            .getFiltrertYtelser();

        var løpendeSPBasertPåDagpenger = løpendeSykepenger.stream()
            .filter(yt -> erBasertPåDagpenger(yt.getYtelseGrunnlag()))
            .collect(Collectors.toList());

        if (løpendeSPBasertPåDagpenger.isEmpty()) {
            return Optional.empty();
        }

        var finnesNødvendigSykemelding = finnesSykemeldingPåSkjæringstidspunktet(løpendeSPBasertPåDagpenger,
            skjæringstidspunkt);
        return finnesNødvendigSykemelding ? Optional.empty() : utledFrist(skjæringstidspunkt, dagensDato);

    }

    private static Optional<LocalDate> utledFrist(LocalDate skjæringstidspunkt, LocalDate dagensdato) {
        if (!dagensdato.isAfter(skjæringstidspunkt)) {
            return Optional.of(skjæringstidspunkt.plusDays(1));
        }
        if (!dagensdato.isAfter(skjæringstidspunkt.plusDays(14))) {
            return Optional.of(dagensdato.plusDays(1));
        }
        return Optional.empty();
    }

    private static boolean finnesSykemeldingPåSkjæringstidspunktet(List<Ytelse> løpendeSPBasertPåDagpenger,
                                                                   LocalDate skjæringstidspunkt) {
        return løpendeSPBasertPåDagpenger.stream()
            .map(Ytelse::getYtelseAnvist)
            .flatMap(Collection::stream)
            .anyMatch(ya -> ya.getAnvistFOM().isBefore(skjæringstidspunkt) && !ya.getAnvistTOM()
                .isBefore(skjæringstidspunkt));
    }

    private static boolean erBasertPåDagpenger(Optional<YtelseGrunnlag> ytelseGrunnlag) {
        return ytelseGrunnlag.map(
            yg -> ARBEIDSKATEGORI_DAGPENGER.contains(yg.getArbeidskategori().orElse(Arbeidskategori.UDEFINERT)))
            .orElse(false);
    }
}
