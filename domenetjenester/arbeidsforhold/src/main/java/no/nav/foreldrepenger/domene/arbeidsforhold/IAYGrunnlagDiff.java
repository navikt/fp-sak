package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.util.FPDateUtil;

public class IAYGrunnlagDiff {
    private static final Set<RelatertYtelseType> EKSLUSIVE_TYPER = Set.of(RelatertYtelseType.FORELDREPENGER, RelatertYtelseType.ENGANGSSTØNAD);
    private InntektArbeidYtelseGrunnlag grunnlag1;
    private InntektArbeidYtelseGrunnlag grunnlag2;

    public IAYGrunnlagDiff(InntektArbeidYtelseGrunnlag grunnlag1, InntektArbeidYtelseGrunnlag grunnlag2) {
        this.grunnlag1 = grunnlag1;
        this.grunnlag2 = grunnlag2;
    }

    public DiffResult diffResultat(boolean onlyCheckTrackedFields) {
        return new IAYDiffsjekker(onlyCheckTrackedFields).getDiffEntity().diff(grunnlag1, grunnlag2);
    }

    public boolean erEndringPåInntektsmelding() {
        var eksisterende = grunnlag1.getInntektsmeldinger();
        var nye = grunnlag2.getInntektsmeldinger();

        // quick check
        if (eksisterende.isPresent() != nye.isPresent()) {
            return true;
        } else if (!eksisterende.isPresent() && !nye.isPresent()) {
            return false;
        } else {
            if (eksisterende.get().getAlleInntektsmeldinger().size() != nye.get().getAlleInntektsmeldinger().size()) {
                return true;
            } else {
                DiffResult diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterende.get(), nye.get());
                return !diff.isEmpty();
            }
        }
    }

    public boolean erEndringPåAktørArbeidForAktør(LocalDate skjæringstidspunkt, AktørId aktørId) {
        var eksisterendeAktørArbeid = grunnlag1.getAktørArbeidFraRegister(aktørId);
        var nyAktørArbeid = grunnlag2.getAktørArbeidFraRegister(aktørId);

        // quick check
        if (eksisterendeAktørArbeid.isPresent() != nyAktørArbeid.isPresent()) {
            return true;
        } else if (!eksisterendeAktørArbeid.isPresent() && !nyAktørArbeid.isPresent()) {
            return false;
        } else {
            var eksisterendeFilter = new YrkesaktivitetFilter(null, eksisterendeAktørArbeid).før(skjæringstidspunkt);
            var nyFilter = new YrkesaktivitetFilter(null, nyAktørArbeid).før(skjæringstidspunkt);
            if (eksisterendeFilter.getYrkesaktiviteter().size() != nyFilter.getYrkesaktiviteter().size()
                || eksisterendeFilter.getAnsettelsesPerioder().size() != nyFilter.getAnsettelsesPerioder().size()) {
                return true;
            }
        }

        // deep check
        DiffResult diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterendeAktørArbeid.get(), nyAktørArbeid.get());
        return !diff.isEmpty();
    }

    public boolean erEndringPåAktørInntektForAktør(LocalDate skjæringstidspunkt, AktørId aktørId) {

        var eksisterende = grunnlag1.getAktørInntektFraRegister(aktørId);
        var nye = grunnlag2.getAktørInntektFraRegister(aktørId);

        // quick check
        if (eksisterende.isPresent() != nye.isPresent()) {
            return true;
        } else if (!eksisterende.isPresent() && !nye.isPresent()) {
            return false;
        } else {
            var eksisterendeInntektFilter = new InntektFilter(eksisterende).før(skjæringstidspunkt);
            var nyeInntektFilter = new InntektFilter(nye).før(skjæringstidspunkt);
            // TODO - raffinere med tanke på Startpunkt BEREGNING. Kan sjekke på diff pensjonsgivende, beregning og Sigrun
            if (eksisterendeInntektFilter.getFiltrertInntektsposter().size() != nyeInntektFilter.getFiltrertInntektsposter().size()) {
                return true;
            }
        }
        // deep check
        DiffResult diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterende.get(), nye.get());
        return !diff.isEmpty();
    }

    public AktørYtelseEndring endringPåAktørYtelseForAktør(Saksnummer egetSaksnummer, LocalDate skjæringstidspunkt, AktørId aktørId) {
        Predicate<Ytelse> predikatEksklusiveTyper = ytelse -> EKSLUSIVE_TYPER.contains(ytelse.getRelatertYtelseType())
            && (ytelse.getSaksnummer() == null || !ytelse.getSaksnummer().equals(egetSaksnummer));
        Predicate<Ytelse> predikatAndreYtelseTyper = ytelse -> !EKSLUSIVE_TYPER.contains(ytelse.getRelatertYtelseType())
            && (ytelse.getSaksnummer() == null || !ytelse.getSaksnummer().equals(egetSaksnummer));
        // Setter fris for å få med nye "parallelle" søknader, men unngår overlapp med neste barn. Kan tunes. Annen søknad får AP når denne vedtatt
        LocalDate datoForEksklusiveTyper = FPDateUtil.iDag().isAfter(skjæringstidspunkt) ? skjæringstidspunkt.plusMonths(3L) : skjæringstidspunkt;

        List<Ytelse> førYtelserFpsak = hentYtelserForAktør(grunnlag1, datoForEksklusiveTyper, aktørId, predikatEksklusiveTyper);
        List<Ytelse> nåYtelserFpsak = hentYtelserForAktør(grunnlag2, datoForEksklusiveTyper, aktørId, predikatEksklusiveTyper);
        boolean erEksklusiveYtlserEndret = !new IAYDiffsjekker().getDiffEntity().diff(førYtelserFpsak, nåYtelserFpsak).isEmpty();

        List<Ytelse> førYtelserEkstern = hentYtelserForAktør(grunnlag1, skjæringstidspunkt, aktørId, predikatAndreYtelseTyper);
        List<Ytelse> nåYtelserEkstern = hentYtelserForAktør(grunnlag2, skjæringstidspunkt, aktørId, predikatAndreYtelseTyper);
        boolean erAndreYtelserEndret = !new IAYDiffsjekker().getDiffEntity().diff(førYtelserEkstern, nåYtelserEkstern).isEmpty();

        return new AktørYtelseEndring(erEksklusiveYtlserEndret, erAndreYtelserEndret);
    }

    private List<Ytelse> hentYtelserForAktør(InntektArbeidYtelseGrunnlag grunnlag, LocalDate skjæringstidspunkt, AktørId aktørId,
                                             Predicate<Ytelse> predikatYtelseskilde) {
        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt);
        return filter.getFiltrertYtelser().stream()
            .filter(predikatYtelseskilde)
            .collect(Collectors.toList());
    }
}
