package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class IAYGrunnlagDiff {
    private static final Set<RelatertYtelseType> EKSLUSIVE_TYPER = Set.of(RelatertYtelseType.FORELDREPENGER, RelatertYtelseType.ENGANGSTØNAD);
    private static final Set<RelatertYtelseType> PLEIEPENGER_TYPER = Set.of(RelatertYtelseType.PLEIEPENGER_SYKT_BARN,
        RelatertYtelseType.PLEIEPENGER_NÆRSTÅENDE, RelatertYtelseType.OPPLÆRINGSPENGER);

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
        }
        if (eksisterende.isEmpty()) {
            return false;
        }
        if (eksisterende.get().getAlleInntektsmeldinger().size() != nye.map(InntektsmeldingAggregat::getAlleInntektsmeldinger).orElse(List.of()).size()) {
            return true;
        }
        var diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterende.get(), nye.orElse(null));
        return !diff.isEmpty();
    }

    public boolean erEndringPåAktørArbeidForAktør(LocalDate skjæringstidspunkt, AktørId aktørId) {
        var eksisterendeAktørArbeid = grunnlag1.getAktørArbeidFraRegister(aktørId);
        var nyAktørArbeid = grunnlag2.getAktørArbeidFraRegister(aktørId);

        // quick check
        if (eksisterendeAktørArbeid.isPresent() != nyAktørArbeid.isPresent()) {
            return true;
        }
        if (eksisterendeAktørArbeid.isEmpty()) {
            return false;
        }
        var eksisterendeFilter = new YrkesaktivitetFilter(Optional.empty(), eksisterendeAktørArbeid).før(skjæringstidspunkt);
        var nyFilter = new YrkesaktivitetFilter(Optional.empty(), nyAktørArbeid).før(skjæringstidspunkt);
        if (eksisterendeFilter.getYrkesaktiviteter().size() != nyFilter.getYrkesaktiviteter().size()
            || eksisterendeFilter.getAnsettelsesPerioder().size() != nyFilter.getAnsettelsesPerioder().size()) {
            return true;
        }

        // deep check
        var diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterendeAktørArbeid.get(), nyAktørArbeid.orElse(null));
        return !diff.isEmpty();
    }

    public boolean erEndringPåAktørInntektForAktør(LocalDate skjæringstidspunkt, AktørId aktørId) {

        var eksisterende = grunnlag1.getAktørInntektFraRegister(aktørId);
        var nye = grunnlag2.getAktørInntektFraRegister(aktørId);

        // quick check
        if (eksisterende.isPresent() != nye.isPresent()) {
            return true;
        }
        if (eksisterende.isEmpty()) {
            return false;
        }
        var eksisterendeInntektFilter = new InntektFilter(eksisterende).før(skjæringstidspunkt);
        var nyeInntektFilter = new InntektFilter(nye).før(skjæringstidspunkt);
        // TODO - raffinere med tanke på Startpunkt BEREGNING. Kan sjekke på diff pensjonsgivende, beregning og Sigrun
        if (eksisterendeInntektFilter.getFiltrertInntektsposter().size() != nyeInntektFilter.getFiltrertInntektsposter().size()) {
            return true;
        }
        // deep check
        var diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterende.get(), nye.orElse(null));
        return !diff.isEmpty();
    }

    public AktørYtelseEndring endringPåAktørYtelseForAktør(Saksnummer egetSaksnummer, LocalDate skjæringstidspunkt, AktørId aktørId) {
        Predicate<Ytelse> predikatEksklusiveTyper = ytelse -> EKSLUSIVE_TYPER.contains(ytelse.getRelatertYtelseType()) && (
            ytelse.getSaksnummer() == null || !ytelse.getSaksnummer().equals(egetSaksnummer));
        Predicate<Ytelse> predikatAndreYtelseTyper = ytelse -> !EKSLUSIVE_TYPER.contains(ytelse.getRelatertYtelseType()) && (
            ytelse.getSaksnummer() == null || !ytelse.getSaksnummer().equals(egetSaksnummer));
        // Setter fris for å få med nye "parallelle" søknader, men unngår overlapp med
        // neste barn. Kan tunes. Annen søknad får AP når denne vedtatt
        var datoForEksklusiveTyper = LocalDate.now().isAfter(skjæringstidspunkt) ? skjæringstidspunkt.plusMonths(3L) : skjæringstidspunkt;

        var førYtelserFpsak = hentYtelserForAktør(grunnlag1, datoForEksklusiveTyper, aktørId, predikatEksklusiveTyper);
        var nåYtelserFpsak = hentYtelserForAktør(grunnlag2, datoForEksklusiveTyper, aktørId, predikatEksklusiveTyper);
        var erEksklusiveYtlserEndret = !new IAYDiffsjekker().getDiffEntity().diff(førYtelserFpsak, nåYtelserFpsak).isEmpty();

        var førYtelserEkstern = hentYtelserForAktør(grunnlag1, skjæringstidspunkt, aktørId, predikatAndreYtelseTyper);
        var nåYtelserEkstern = hentYtelserForAktør(grunnlag2, skjæringstidspunkt, aktørId, predikatAndreYtelseTyper);
        var erAndreYtelserEndret = !new IAYDiffsjekker().getDiffEntity().diff(førYtelserEkstern, nåYtelserEkstern).isEmpty();

        return new AktørYtelseEndring(erEksklusiveYtlserEndret, erAndreYtelserEndret);
    }

    private List<Ytelse> hentYtelserForAktør(InntektArbeidYtelseGrunnlag grunnlag, LocalDate skjæringstidspunkt, AktørId aktørId,
            Predicate<Ytelse> predikatYtelseskilde) {
        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt);
        return filter.getFiltrertYtelser().stream()
                .filter(predikatYtelseskilde)
                .toList();
    }

    public boolean erEndringPleiepengerEtterStp(LocalDate skjæringstidspunkt, AktørId aktørId) {
        Predicate<Ytelse> predikatPleiepenger = ytelse -> PLEIEPENGER_TYPER.contains(ytelse.getRelatertYtelseType());

        var førYtelserEkstern = hentYtelserForAktørEtterStp(grunnlag1, skjæringstidspunkt, aktørId, predikatPleiepenger);
        var nåYtelserEkstern = hentYtelserForAktørEtterStp(grunnlag2, skjæringstidspunkt, aktørId, predikatPleiepenger);

        return !new IAYDiffsjekker().getDiffEntity().diff(førYtelserEkstern, nåYtelserEkstern).isEmpty();
    }

    private List<Ytelse> hentYtelserForAktørEtterStp(InntektArbeidYtelseGrunnlag grunnlag, LocalDate skjæringstidspunkt, AktørId aktørId,
                                             Predicate<Ytelse> predikatYtelseskilde) {
        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)).etter(skjæringstidspunkt);
        return filter.getFiltrertYtelser().stream()
            .filter(predikatYtelseskilde)
            .toList();
    }

    public boolean erEndringPåOppgittOpptjening() {
        var eksisterende = grunnlag1.getGjeldendeOppgittOpptjening();
        var nye = grunnlag2.getGjeldendeOppgittOpptjening();
        return erEndringPåOppgittOpptjening(eksisterende, nye);
    }

    public static boolean erEndringPåOppgittOpptjening(Optional<OppgittOpptjening> eksisterende, Optional<OppgittOpptjening> nye) {
        // quick check
        if (eksisterende.isPresent() != nye.isPresent()) {
            return true;
        }
        if (eksisterende.isEmpty()) {
            return false;
        }
        // deep check
        var diff = new IAYDiffsjekker().getDiffEntity().diff(eksisterende.get(), nye.orElse(null));
        return !diff.isEmpty();
    }
}
