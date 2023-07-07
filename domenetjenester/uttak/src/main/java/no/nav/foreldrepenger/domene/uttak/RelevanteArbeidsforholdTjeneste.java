package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.UttakYrkesaktiviteter;

@ApplicationScoped
public class RelevanteArbeidsforholdTjeneste {

    private FpUttakRepository fpUttakRepository;

    @Inject
    public RelevanteArbeidsforholdTjeneste(FpUttakRepository fpUttakRepository) {
        this.fpUttakRepository = fpUttakRepository;
    }

    RelevanteArbeidsforholdTjeneste() {
        //CDI
    }

    public boolean arbeidsforholdRelevantForUttakErEndretSidenForrigeBehandling(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var forrigeBehandling = finnForrigeBehandling(ref);
        var uttakForrigeBehandling = fpUttakRepository.hentUttakResultatHvisEksisterer(forrigeBehandling);
        if (uttakForrigeBehandling.isEmpty()) {
            return false;
        }
        var bgStatuser = input.getBeregningsgrunnlagStatuser();
        var uttakAktiviteter = aktiviteterIUttak(uttakForrigeBehandling.get());
        for (var status : bgStatuser) {
            if (!finnesAktivitetIUttakLikBgStatus(uttakAktiviteter, status)) {
                return true;
            }
        }
        for (var uttakAktivitet : uttakAktiviteter) {
            if (!finnesBgStatusLikUttakAktivitet(bgStatuser, uttakAktivitet)) {
                return true;
            }
        }
        if (bgStatuser.size() > 1 || uttakAktiviteter.size() > 1) {
            return aktivitetIFørsteUttaksperiodeHarStartdatoEtterPerioden(input, uttakForrigeBehandling.get());
        }
        return false;
    }

    private boolean finnesBgStatusLikUttakAktivitet(Collection<BeregningsgrunnlagStatus> statuser,
                                                    UttakAktivitetEntitet uttakAktivitet) {
        return statuser.stream().anyMatch(status -> aktivitetLikBgStatus(status, uttakAktivitet));
    }

    private Set<UttakAktivitetEntitet> aktiviteterIUttak(UttakResultatEntitet uttak) {
        return uttak.getGjeldendePerioder()
            .getPerioder()
            .stream()
            .flatMap(p -> p.getAktiviteter().stream())
            .map(UttakResultatPeriodeAktivitetEntitet::getUttakAktivitet)
            .collect(Collectors.toSet());
    }

    private boolean finnesAktivitetIUttakLikBgStatus(Set<UttakAktivitetEntitet> uttakAktiviteter,
                                                     BeregningsgrunnlagStatus bgStatus) {
        return uttakAktiviteter.stream().anyMatch(aktivitet -> aktivitetLikBgStatus(bgStatus, aktivitet));
    }

    private boolean aktivitetLikBgStatus(BeregningsgrunnlagStatus bgStatus, UttakAktivitetEntitet aktivitet) {
        if (aktivitet.getUttakArbeidType().equals(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)) {
            return bgStatus.erSelvstendigNæringsdrivende();
        }
        if (aktivitet.getUttakArbeidType().equals(UttakArbeidType.FRILANS)) {
            return bgStatus.erFrilanser();
        }
        if (aktivitet.getUttakArbeidType().equals(UttakArbeidType.ORDINÆRT_ARBEID)) {
            return ordinærtArbeidAktivitetLikStatus(bgStatus, aktivitet);
        }
        if (aktivitet.getUttakArbeidType().equals(UttakArbeidType.ANNET)) {
            return !bgStatus.erArbeidstaker() && !bgStatus.erFrilanser() && !bgStatus.erSelvstendigNæringsdrivende();
        }
        throw new IllegalStateException("Ukjent uttakArbeidType " + aktivitet.getUttakArbeidType());
    }

    private boolean ordinærtArbeidAktivitetLikStatus(BeregningsgrunnlagStatus status, UttakAktivitetEntitet a) {
        if (!status.erArbeidstaker()) {
            return false;
        }
        var arbeidsgiver = status.getArbeidsgiver();
        if (arbeidsgiver.isEmpty()) {
            return a.getArbeidsgiver().isEmpty();
        }
        if (a.getArbeidsgiver().isEmpty()) {
            return false;
        }
        var arbeidsforholdRef = status.getArbeidsforholdRef();
        return Objects.equals(a.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null), arbeidsgiver.get().getIdentifikator())
            && Objects.equals(a.getArbeidsforholdRef(), arbeidsforholdRef.orElse(InternArbeidsforholdRef.nullRef()));
    }

    private Long finnForrigeBehandling(BehandlingReferanse behandling) {
        return behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException(
                "Utviklerfeil: Original behandling mangler på revurdering - skal ikke skje"));
    }

    private boolean aktivitetIFørsteUttaksperiodeHarStartdatoEtterPerioden(UttakInput input,
                                                                           UttakResultatEntitet uttakForrigeBehandling) {
        var gjeldendePerioder = uttakForrigeBehandling.getGjeldendePerioder().getPerioder();
        if (gjeldendePerioder.isEmpty()) {
            return false;
        }
        var førstePeriodeForrigeUttak = gjeldendePerioder.get(0);
        return førstePeriodeForrigeUttak.getAktiviteter()
            .stream()
            .anyMatch(a -> startdatoEtterDato(a, førstePeriodeForrigeUttak.getFom(), new UttakYrkesaktiviteter(input)));
    }

    private boolean startdatoEtterDato(UttakResultatPeriodeAktivitetEntitet aktivitet,
                                       LocalDate dato,
                                       UttakYrkesaktiviteter yrkesaktiviteter) {
        var arbeidsgiver = aktivitet.getUttakAktivitet().getArbeidsgiver();
        if (arbeidsgiver.isEmpty()) {
            return false;
        }
        var startdato = yrkesaktiviteter.finnStartdato(arbeidsgiver.get(),
            aktivitet.getUttakAktivitet().getArbeidsforholdRef()).orElse(LocalDate.MIN);
        return startdato.isAfter(dato);
    }
}
