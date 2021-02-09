package no.nav.foreldrepenger.økonomistøtte.dagytelse.opphør;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.minBy;
import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.OppdragKvitteringTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;

public class OpphørUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpphørUtil.class);

    static Set<KodeKlassifik> finnKlassekoderSomIkkeErOpphørt(List<Oppdrag110> oppdrag110Liste) {
        return førsteAktiveDatoPrKodeKlassifik(oppdrag110Liste, false, null).keySet();
    }

    private static Map<KodeKlassifik, LocalDate> førsteAktiveDatoPrKodeKlassifik(List<Oppdrag110> oppdrag110Liste, boolean kunBruker, String refunderesId) {
        Map<KodeKlassifik, LocalDate> tidligsteDato = new HashMap<>();
        Map<FeriepengerNøkkel, LocalDate> feriepengerTidligstDato = new HashMap<>();

        for (Oppdrag110 oppdrag110 : sorterEtterOpprettetTidspunk(oppdrag110Liste)) {
            for (Oppdragslinje150 linje : sorterEtterDelytelseId(oppdrag110.getOppdragslinje150Liste())) {
                if (kunBruker && linje.getUtbetalesTilId() == null) {
                    continue;
                }
                if (!kunBruker && refunderesId != null && linje.getRefusjonsinfo156() != null && !Objects.equals(linje.getRefusjonsinfo156().getRefunderesId(), refunderesId)) {
                    continue;
                }
                KodeKlassifik kodeKlassifik = linje.getKodeKlassifik();

                if (kodeKlassifik.gjelderFerie()) {
                    FeriepengerNøkkel feriepengerNøkkel = FeriepengerNøkkel.fra(linje.getDatoVedtakFom(), kodeKlassifik);
                    LocalDate tidligste = feriepengerTidligstDato.get(feriepengerNøkkel);

                    if (linje.gjelderOpphør()) {
                       LocalDate opphørsdato = linje.getDatoStatusFom();
                        if (!opphørsdato.isAfter(tidligste)) {
                            feriepengerTidligstDato.remove(feriepengerNøkkel);
                        }
                    } else {
                        LocalDate fom = linje.getDatoVedtakFom();
                        if (tidligste == null || fom.isBefore(tidligste)) {
                            feriepengerTidligstDato.put(feriepengerNøkkel, fom);
                        }
                    }
                } else {
                    LocalDate tidligste = tidligsteDato.get(kodeKlassifik);
                    if (linje.gjelderOpphør()) {
                        LocalDate opphørsdato = linje.getDatoStatusFom();
                        if (opphørsdato == null || tidligste == null) {
                            LOGGER.warn("Opphør uten noe å opphøre: delytelse {} klasseKode {} fom {} tom {} opphørsdato {} tidligste {}",
                                linje.getDelytelseId(), linje.getKodeKlassifik(), linje.getDatoVedtakFom(), linje.getDatoVedtakTom(), linje.getDatoStatusFom(), tidligste);
                        }
                        if (!opphørsdato.isAfter(tidligste)) {
                            tidligsteDato.remove(kodeKlassifik);
                        }
                    }
                    if (!linje.gjelderOpphør()) {
                        LocalDate fom = linje.getDatoVedtakFom();
                        if (tidligste == null || fom.isBefore(tidligste)) {
                            tidligsteDato.put(kodeKlassifik, fom);
                        }
                    }
                }
            }
        }

        Map<KodeKlassifik, Optional<FeriepengerNøkkel>> minFeriepengerFomDatoPerKodeklasifikk = feriepengerTidligstDato.keySet().stream().collect(groupingBy(FeriepengerNøkkel::getKodeKlassifik,
            minBy(Comparator.comparing(FeriepengerNøkkel::getÅrDetGjelderFor))));

        minFeriepengerFomDatoPerKodeklasifikk.values().forEach(entry -> tidligsteDato.put(entry.map(FeriepengerNøkkel::getKodeKlassifik).orElseThrow(), entry.map(FeriepengerNøkkel::getÅrDetGjelderFor).orElseThrow()));

        return tidligsteDato;
    }

    private static List<Oppdragslinje150> sorterEtterDelytelseId(Collection<Oppdragslinje150> input) {
        return input.stream()
            .sorted(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())))
            .collect(toList());
    }

    private static List<Oppdrag110> sorterEtterOpprettetTidspunk(Collection<Oppdrag110> input) {
        return input.stream()
            .sorted(Comparator.comparing(Oppdrag110::getOpprettetTidspunkt))
            .collect(toList());
    }

    static boolean erBrukerAllredeFullstendigOpphørt(OppdragInput behandlingInfo) {
        List<Oppdrag110> oppdragForBruker = behandlingInfo.getAlleTidligereOppdrag110().stream()
            .filter(OpphørUtil::gjelderBruker)
            .filter(o -> o.venterKvittering() || OppdragKvitteringTjeneste.harPositivKvittering(o))
            .collect(toList());

        return førsteAktiveDatoPrKodeKlassifik(oppdragForBruker, true, null).isEmpty();
    }

    static boolean erBrukerAllredeFullstendigOpphørtForKlassekode(OppdragInput behandlingInfo, KodeKlassifik klassekode) {
        List<Oppdrag110> oppdragForBruker = behandlingInfo.getAlleTidligereOppdrag110().stream()
            .filter(OpphørUtil::gjelderBruker)
            .filter(o -> o.venterKvittering() || OppdragKvitteringTjeneste.harPositivKvittering(o))
            .collect(toList());

        var brukersKlassekoder = førsteAktiveDatoPrKodeKlassifik(oppdragForBruker, true, null);
        return brukersKlassekoder.get(klassekode) == null;
    }

    static boolean erArbeidsgiverAllredeFullstendigOpphørt(OppdragInput behandlingInfo, String refunderesId) {
        List<Oppdrag110> oppdragForAG = behandlingInfo.getAlleTidligereOppdrag110().stream()
            .filter(o -> gjelderArbeidsgiver(o, refunderesId))
            .filter(o -> o.venterKvittering() || OppdragKvitteringTjeneste.harPositivKvittering(o))
            .collect(toList());

        return førsteAktiveDatoPrKodeKlassifik(oppdragForAG, false, refunderesId).isEmpty();
    }

    private static boolean gjelderBruker(Oppdrag110 oppdrag110) {
        return oppdrag110.getOppdragslinje150Liste().stream().anyMatch(ol -> ol.getUtbetalesTilId() != null);
    }

    private static boolean gjelderArbeidsgiver(Oppdrag110 oppdrag110, String refunderesId) {
        return oppdrag110.getOppdragslinje150Liste().stream().anyMatch(ol -> ol.getRefusjonsinfo156() != null && Objects.equals(ol.getRefusjonsinfo156().getRefunderesId(), refunderesId));
    }

    static class FeriepengerNøkkel {
        private final LocalDate årDetGjelderFor;
        private final KodeKlassifik kodeKlassifik;

        public FeriepengerNøkkel(LocalDate årDetGjelderFor, KodeKlassifik kodeKlassifik) {
            this.årDetGjelderFor = årDetGjelderFor;
            this.kodeKlassifik = kodeKlassifik;
        }

        public static FeriepengerNøkkel fra(LocalDate årDetGjelderFor, KodeKlassifik kodeKlassifik) {
            return new FeriepengerNøkkel(årDetGjelderFor, kodeKlassifik);
        }

        public LocalDate getÅrDetGjelderFor() {
            return årDetGjelderFor;
        }

        public KodeKlassifik getKodeKlassifik() {
            return kodeKlassifik;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FeriepengerNøkkel that = (FeriepengerNøkkel) o;
            return Objects.equals(getÅrDetGjelderFor(), that.getÅrDetGjelderFor()) && getKodeKlassifik() == that.getKodeKlassifik();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getÅrDetGjelderFor(), getKodeKlassifik());
        }
    }

}

