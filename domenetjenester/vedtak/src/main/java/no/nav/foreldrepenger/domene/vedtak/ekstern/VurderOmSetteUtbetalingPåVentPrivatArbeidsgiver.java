package no.nav.foreldrepenger.domene.vedtak.ekstern;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ØkonomioppdragRepository;

@ApplicationScoped
class VurderOmSetteUtbetalingPåVentPrivatArbeidsgiver {

    private BeregningsresultatRepository beregningsresultatRepository;
    private ØkonomioppdragRepository økonomioppdragRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    public VurderOmSetteUtbetalingPåVentPrivatArbeidsgiver() {
        // CDI
    }

    @Inject
    public VurderOmSetteUtbetalingPåVentPrivatArbeidsgiver(BeregningsresultatRepository beregningsresultatRepository,
                                                           ØkonomioppdragRepository økonomioppdragRepository,
                                                           OppgaveTjeneste oppgaveTjeneste,
                                                           BehandlingVedtakRepository behandlingVedtakRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
    }

    void opprettOppgave(Behandling behandling) {
        var refusjoner = vurder(behandling);
        if (!refusjoner.isEmpty()) {
            // send en oppdrag til NØS hver privat arbeidsgiver, det kan være flere
            List<BeregningsresultatPeriode> beregningsperioder = hentBeregningsperioder(behandling.getId());
            for (AktørId arbeidsgiverAktørId : refusjoner) {
                LocalDate førsteUttaksdato = hentFørsteUttaksdato(arbeidsgiverAktørId, beregningsperioder);
                LocalDate vedtaksdato = hentVedtaksdato(behandling);
                oppgaveTjeneste.opprettOppgaveSettUtbetalingPåVentPrivatArbeidsgiver(behandling.getId(),
                    førsteUttaksdato,
                    vedtaksdato,
                    arbeidsgiverAktørId);
            }
        }
    }

    /**
     * Vurder hvis trenges å sende en oppgave til NØS når bruker har privatperson som arbeidsgiver som krever
     * refusjon etter endringsdato.
     *
     * @param behandling aktuell behandling
     * @return en mappe av dato interval/refusjoner til privatarbeidsgiver ellers en tom liste
     */
    private Set<AktørId> vurder(Behandling behandling) {
        Set<AktørId> result = new HashSet<>();
        List<Oppdragslinje150> oppdragslinje150List = hentAlleOppdragslinje150IkkeOPPH(behandling);

        Long behandlingId = behandling.getId();
        List<BeregningsresultatPeriode> beregningsperioder = hentBeregningsperioder(behandlingId);
        for (Oppdragslinje150 oppdragslinje150 : oppdragslinje150List) {
            LocalDate datoVedtakFom = oppdragslinje150.getDatoVedtakFom();
            LocalDate datoVedtakTom = oppdragslinje150.getDatoVedtakTom();
            beregningsperioder.stream()
                .filter(brPeriode -> sjekkVedtakPeriode(datoVedtakFom, datoVedtakTom, brPeriode))
                .flatMap(brPeriode -> brPeriode.getBeregningsresultatAndelList().stream())
                .filter(this::erPrivatarbeidsgiverMedRefusjon)
                .forEach(brAndel -> result.add(hentArbeidsgiverPersonAktørId(brAndel)));
        }
        return result;
    }

    private List<Oppdragslinje150> hentAlleOppdragslinje150IkkeOPPH(Behandling behandling) {
        Optional<Oppdragskontroll> oppdragskontroll = økonomioppdragRepository.finnOppdragForBehandling(behandling.getId());
        return oppdragskontroll
            .map(Oppdragskontroll::getOppdrag110Liste)
            .orElse(Collections.emptyList())
            .stream()
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .filter(oppdragslinje150 -> !oppdragslinje150.gjelderOpphør())
            .collect(Collectors.toList());
    }

    private List<BeregningsresultatPeriode> hentBeregningsperioder(Long behandlingId) {
        return beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .orElse(Collections.emptyList());
    }

    private boolean erPrivatarbeidsgiverMedRefusjon(BeregningsresultatAndel brAndel) {
        return !brAndel.erBrukerMottaker() && brAndel.erArbeidsgiverPrivatperson() && brAndel.getDagsats() > 0;
    }

    private boolean sjekkVedtakPeriode(LocalDate vedtakPeriodeFom,
                                       LocalDate vedtakPeriodeTom,
                                       BeregningsresultatPeriode brPeriode) {
        return vedtakPeriodeFom.compareTo(brPeriode.getBeregningsresultatPeriodeFom()) >= 0 &&
            vedtakPeriodeTom.compareTo(brPeriode.getBeregningsresultatPeriodeTom()) == 0;

    }

    private AktørId hentArbeidsgiverPersonAktørId(BeregningsresultatAndel brAndel) {
        return brAndel.getArbeidsgiver()
            .map(Arbeidsgiver::getAktørId)
            .orElseThrow(() -> new IllegalStateException("Mangler AktørId til privat arbeidsgiver"));
    }

    private LocalDate hentFørsteUttaksdato(AktørId aktørId, List<BeregningsresultatPeriode> beregningsperioder) {
        return beregningsperioder.stream()
            .sorted(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
            // først uttaksdato til hver arbeidsgiver
            .filter(brp -> brp.getBeregningsresultatAndelList().stream()
                .filter(this::erPrivatarbeidsgiverMedRefusjon)
                .anyMatch(a -> aktørId.equals(hentArbeidsgiverPersonAktørId(a)))
            )
            .findFirst()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom).orElse(null);
    }

    private LocalDate hentVedtaksdato(Behandling behandling) {
        return behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandling.getId())
            .map(BehandlingVedtak::getVedtaksdato)
            .orElseThrow(() -> new IllegalStateException(String.format("Finner ikke vedtaksdato for behandling %d", behandling.getId())));
    }
}
