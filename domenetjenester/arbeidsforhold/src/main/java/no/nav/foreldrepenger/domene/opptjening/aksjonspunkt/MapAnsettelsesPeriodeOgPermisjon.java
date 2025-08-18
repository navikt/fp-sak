package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import no.nav.foreldrepenger.domene.arbeidsforhold.impl.HentBekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class MapAnsettelsesPeriodeOgPermisjon {

    private MapAnsettelsesPeriodeOgPermisjon() {
        // skjul public constructor
    }

    static Collection<AktivitetsAvtale> ansettelsesPerioderUtenomFullPermisjon(InntektArbeidYtelseGrunnlag grunnlag, Yrkesaktivitet yrkesaktivitet) {
        var bekreftetPermisjonOpt = HentBekreftetPermisjon.hent(grunnlag, yrkesaktivitet);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), yrkesaktivitet);
        var ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet);
        if (bekreftetPermisjonOpt.isEmpty()) {
            return ansettelsesPerioder;
        }
        var bekreftetPermisjon = bekreftetPermisjonOpt.get();
        var erFullPermisjon = yrkesaktivitet.getPermisjon()
            .stream()
            .filter(perm -> perm.getPeriode().equals(bekreftetPermisjon.getPeriode()))
            .findFirst()
            .map(perm -> perm.getProsentsats() != null && perm.getProsentsats().getVerdi().compareTo(BigDecimal.valueOf(100)) >= 0)
            .orElse(false);
        if (bekreftetPermisjon.getStatus().equals(BekreftetPermisjonStatus.BRUK_PERMISJON) && erFullPermisjon) {
            // Fjerne delperioder fra ansettelsesperioder som overlapper med permisjon - knekkes opp ved behov
            // Vurder å lage ikke-overlappende ansettelsesperioder ved å slå sammen til tidslinje med coalesceLeft/Right
            return ansettelsesPerioder.stream()
                .flatMap(ap -> fjernPermisjon(ap, bekreftetPermisjon.getPeriode()))
                .sorted(Comparator.comparing(p -> p.getPeriode().getFomDato()))
                .toList();
        }
        return ansettelsesPerioder;
    }

    static AktivitetsAvtale aktivitetsAvtaleFraSegment(LocalDateSegment<AktivitetsAvtale> seg) {
        return AktivitetsAvtaleBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(seg.getFom(), seg.getTom()))
            .medProsentsats(seg.getValue().getProsentsats())
            .medSisteLønnsendringsdato(seg.getValue().getSisteLønnsendringsdato())
            .medBeskrivelse(seg.getValue().getBeskrivelse())
            .build();
    }

    // Kan bli litt enklere dersom man lager en tidslinje (obs coalesceL/RHS) av ansettelsesperioder
    private static Stream<AktivitetsAvtale> fjernPermisjon(AktivitetsAvtale ansettelsesPeriode, DatoIntervallEntitet permisjonPeriode) {
        if (ansettelsesPeriode.getPeriode().overlapper(permisjonPeriode)) {
            var permisjonSegment = new LocalDateSegment<>(permisjonPeriode.getFomDato(), permisjonPeriode.getTomDato(),
                AktivitetsAvtaleBuilder.ny().medPeriode(permisjonPeriode).build());
            var permisjonTidslinje = new LocalDateTimeline<>(List.of(permisjonSegment));
            var ansettelsesSegment = new LocalDateSegment<>(ansettelsesPeriode.getPeriode().getFomDato(),
                ansettelsesPeriode.getPeriode().getTomDato(), ansettelsesPeriode);
            var ansettelsesTidslinje = new LocalDateTimeline<>(List.of(ansettelsesSegment));
            return ansettelsesTidslinje.disjoint(permisjonTidslinje).stream().map(MapAnsettelsesPeriodeOgPermisjon::aktivitetsAvtaleFraSegment);
        } else {
            return Stream.of(ansettelsesPeriode);
        }
    }

}
