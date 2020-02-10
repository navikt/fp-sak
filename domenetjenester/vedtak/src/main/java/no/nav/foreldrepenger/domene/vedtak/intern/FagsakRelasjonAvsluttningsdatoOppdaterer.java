package no.nav.foreldrepenger.domene.vedtak.intern;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;

@ApplicationScoped
public class FagsakRelasjonAvsluttningsdatoOppdaterer {

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private UttakRepository uttakRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    public FagsakRelasjonAvsluttningsdatoOppdaterer() {
        // NOSONAR
    }

    @Inject
    public FagsakRelasjonAvsluttningsdatoOppdaterer(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                    StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                                    UttakInputTjeneste uttakInputTjeneste,
                                                    FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.uttakRepository = behandlingRepositoryProvider.getUttakRepository();
        this.familieHendelseRepository = behandlingRepositoryProvider.getFamilieHendelseRepository();
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    void oppdaterFagsakRelasjonAvsluttningsdato(FagsakRelasjon relasjon,
                                                Long fagsakId,
                                                FagsakRelasjonLås lås,
                                                Optional<FagsakLås> fagsak1Lås,
                                                Optional<FagsakLås> fagsak2Lås) {
        fagsakRelasjonTjeneste.oppdaterMedAvsluttningsdato(relasjon, finnAvsluttningsdato(fagsakId, relasjon), lås, fagsak1Lås, fagsak2Lås);
    }

    private LocalDate finnAvsluttningsdato(Long fagsakId, FagsakRelasjon fagsakRelasjon) {
        LocalDate avsluttningsdato = avsluttningsdatoFraEksisterendeFagsakRelasjon(fagsakRelasjon);

        Optional<Behandling> behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        if (behandling.isPresent()) {
            avsluttningsdato = avsluttningsdatoHvisBehandlingAvslåttEllerOpphørt(behandling.get(), avsluttningsdato);
            avsluttningsdato = avsluttningsdatoHvisDetIkkeErStønadsdagerIgjen(behandling.get(), avsluttningsdato);
            avsluttningsdato = avsluttningsdatoHvisDetErStønadsdagerIgjen(behandling.get(), avsluttningsdato);
        }

        if (avsluttningsdato == null) {
            avsluttningsdato = LocalDate.now().plusDays(1);
        }
        return avsluttningsdato;
    }

    private LocalDate avsluttningsdatoFraEksisterendeFagsakRelasjon(FagsakRelasjon fagsakRelasjon) {
        if (fagsakRelasjon.getAvsluttningsdato() != null && fagsakRelasjon.getAvsluttningsdato().isAfter(LocalDate.now())) {
            return fagsakRelasjon.getAvsluttningsdato();
        }
        return null;
    }

    private LocalDate avsluttningsdatoHvisBehandlingAvslåttEllerOpphørt(Behandling behandling, LocalDate avsluttningsdato) {
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        return behandlingsresultat.isPresent() && behandlingsresultat.get().isBehandlingsresultatAvslåttOrOpphørt()
            && erAvsluttningsdatoSattEllerEtter(avsluttningsdato, LocalDate.now()) ? LocalDate.now().plusDays(1) : avsluttningsdato;
    }

    private LocalDate avsluttningsdatoHvisDetIkkeErStønadsdagerIgjen(Behandling behandling,
                                                                     LocalDate avsluttningsdato) {
        Optional<UttakResultatEntitet> uttakResultatEntitet = uttakRepository.hentUttakResultatHvisEksisterer(behandling.getId());
        if (uttakResultatEntitet.isPresent()) {
            List<UttakResultatPeriodeEntitet> uttakPerioder = uttakResultatEntitet.get().getGjeldendePerioder().getPerioder();
            Optional<LocalDate> sisteUttaksdato = uttakPerioder.stream()
                .max(Comparator.comparing(UttakResultatPeriodeEntitet::getTom))
                .map(UttakResultatPeriodeEntitet::getTom);
            var uttakInput = uttakInputTjeneste.lagInput(behandling);
            var sluttPåStønadsdager = stønadskontoSaldoTjeneste.erSluttPåStønadsdager(uttakInput);
            return sisteUttaksdato.isPresent() && sisteUttaksdato.get().isAfter(LocalDate.now()) && erAvsluttningsdatoSattEllerEtter(avsluttningsdato, sisteUttaksdato.get())
                && sluttPåStønadsdager ? sisteUttaksdato.get() : avsluttningsdato;
        }
        return avsluttningsdato;
    }

    private LocalDate avsluttningsdatoHvisDetErStønadsdagerIgjen(Behandling behandling, LocalDate avsluttningsdato) {
        Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        Optional<LocalDate> fødselsdato = familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .flatMap(FamilieHendelseEntitet::getFødselsdato);
        Optional<LocalDate> omsorgsovertalsesdato = familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .flatMap(FamilieHendelseEntitet::getAdopsjon)
            .map(AdopsjonEntitet::getOmsorgsovertakelseDato);
        Optional<LocalDate> termindato = familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .flatMap(FamilieHendelseEntitet::getTerminbekreftelse)
            .map(TerminbekreftelseEntitet::getTermindato);
        return omsorgsovertalsesdato.map(localDate -> (erAvsluttningsdatoSattEllerEtter(avsluttningsdato, localDate.plusYears(3))
            ? localDate.plusYears(3) : avsluttningsdato)).orElseGet(() -> (fødselsdato.isPresent()
            && erAvsluttningsdatoSattEllerEtter(avsluttningsdato, fødselsdato.get().plusYears(3)) ? fødselsdato.get().plusYears(3) : termindato.isPresent()
            && erAvsluttningsdatoSattEllerEtter(avsluttningsdato, termindato.get().plusYears(3)) ? termindato.get().plusYears(3) : avsluttningsdato));
    }

    private boolean erAvsluttningsdatoSattEllerEtter(LocalDate avsluttningsdato, LocalDate nyAvsluttningsdato) {
        return avsluttningsdato == null || nyAvsluttningsdato.isBefore(avsluttningsdato);
    }
}
