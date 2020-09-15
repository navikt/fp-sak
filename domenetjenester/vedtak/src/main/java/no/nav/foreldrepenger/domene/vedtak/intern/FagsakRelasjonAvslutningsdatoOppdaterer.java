package no.nav.foreldrepenger.domene.vedtak.intern;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.regler.uttak.konfig.Parametertype;
import no.nav.foreldrepenger.regler.uttak.konfig.StandardKonfigurasjon;

public abstract class FagsakRelasjonAvslutningsdatoOppdaterer {

    private static final int JUSTERING_I_HELE_MÅNEDER_VED_REST_I_STØNADSDAGER = 3;
    protected static final int KLAGEFRIST_I_UKER_VED_DØD = 6;

    protected FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    protected BehandlingRepository behandlingRepository;
    protected BehandlingsresultatRepository behandlingsresultatRepository;
    protected ForeldrepengerUttakTjeneste fpUttakTjeneste;
    protected FamilieHendelseRepository familieHendelseRepository;

    protected StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    protected UttakInputTjeneste uttakInputTjeneste;
    protected MaksDatoUttakTjeneste maksDatoUttakTjeneste;

    void oppdaterFagsakRelasjonAvsluttningsdato(FagsakRelasjon relasjon,
                                                Long fagsakId,
                                                FagsakRelasjonLås lås,
                                                Optional<FagsakLås> fagsak1Lås,
                                                Optional<FagsakLås> fagsak2Lås) {
        fagsakRelasjonTjeneste.oppdaterMedAvsluttningsdato(relasjon, finnAvslutningsdato(fagsakId, relasjon), lås, fagsak1Lås, fagsak2Lås);
    }

    protected abstract LocalDate finnAvslutningsdato(Long fagsakId, FagsakRelasjon fagsakRelasjon);

    protected LocalDate avsluttningsdatoFraEksisterendeFagsakRelasjon(FagsakRelasjon fagsakRelasjon) {
        if (fagsakRelasjon.getAvsluttningsdato() != null && fagsakRelasjon.getAvsluttningsdato().isAfter(LocalDate.now())) {
            return fagsakRelasjon.getAvsluttningsdato();
        }
        return null;
    }

    private Optional<LocalDate> sisteDødsdato(Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag) {
        Optional<List<UidentifisertBarn>> barna = familieHendelseGrunnlag
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getBarna);

        if (barna.isEmpty() || barna.get().isEmpty() || barna.get().stream().anyMatch(b -> b.getDødsdato().isEmpty()))
            return Optional.empty();
        else return barna.get().stream()
            .map(UidentifisertBarn::getDødsdato)
            .flatMap(Optional::stream)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder());
    }

    protected LocalDate avsluttningsdatoHvisBehandlingAvslåttEllerOpphørt(Behandling behandling, LocalDate avsluttningsdato) {
        var familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        if(familieHendelseGrunnlag.isPresent()){

            Optional<LocalDate> sisteDødsdato = sisteDødsdato(familieHendelseGrunnlag);
            if(sisteDødsdato.isPresent()) return sisteDødsdato.get().plusWeeks(StandardKonfigurasjon.KONFIGURASJON.getParameter(Parametertype.UTTAK_ETTER_BARN_DØDT_UKER, LocalDate.now())).plusWeeks(KLAGEFRIST_I_UKER_VED_DØD);

        }

        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        return behandlingsresultat.isPresent() && behandlingsresultat.get().isBehandlingsresultatAvslåttOrOpphørt()
            && erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, LocalDate.now()) ? LocalDate.now().plusDays(1) : avsluttningsdato;
    }

    protected LocalDate finnAvsluttningsdatoForRelaterteBehandlinger(Behandling behandling, LocalDate avsluttningsdato) {
        var uttakInput = uttakInputTjeneste.lagInput(behandling);
        var maksDatoUttak = maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput);

        if( !maksDatoUttak.isPresent() ) return avsluttningsdatoHvisDetIkkeErGjortUttak(behandling, avsluttningsdato);
        LocalDate avslutningsdatoFraMaksDatoUttak = maksDatoUttak.get().plusDays(1);

        var stønadRest = stønadskontoSaldoTjeneste.finnStønadRest(uttakInput);

        if( stønadRest > 0 ) {
            //rest er allerede lagt til i maksDatoUttak, men der mangler 'buffer'
            avslutningsdatoFraMaksDatoUttak =  avslutningsdatoFraMaksDatoUttak.plusMonths(JUSTERING_I_HELE_MÅNEDER_VED_REST_I_STØNADSDAGER).with(TemporalAdjusters.lastDayOfMonth());
            if(uttakInput != null && uttakInput.getYtelsespesifiktGrunnlag() != null && uttakInput.getYtelsespesifiktGrunnlag() instanceof  ForeldrepengerGrunnlag && ((ForeldrepengerGrunnlag)uttakInput.getYtelsespesifiktGrunnlag()).getFamilieHendelser() != null) {
                avslutningsdatoFraMaksDatoUttak = beskjærMotAbsoluttMaksDato(uttakInput, avslutningsdatoFraMaksDatoUttak);
            }
        }

        return avslutningsdatoFraMaksDatoUttak.isAfter(LocalDate.now()) && erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, avslutningsdatoFraMaksDatoUttak)?
            avslutningsdatoFraMaksDatoUttak : avsluttningsdato;
    }

    private LocalDate beskjærMotAbsoluttMaksDato(UttakInput uttakInput, LocalDate beregnetMaksDato) {
        Optional<LocalDate> fødselsdato = ((ForeldrepengerGrunnlag)uttakInput.getYtelsespesifiktGrunnlag()).getFamilieHendelser().getSøknadFamilieHendelse().getFødselsdato();
        if(fødselsdato.isPresent()){
            LocalDate absoluttMaksDato = fødselsdato.get().plusYears(3);
            return absoluttMaksDato.isBefore(beregnetMaksDato)? absoluttMaksDato : beregnetMaksDato;
        }
        return beregnetMaksDato;
    }

    protected LocalDate avsluttningsdatoHvisDetIkkeErGjortUttak(Behandling behandling, LocalDate avsluttningsdato) {
        Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        Optional<LocalDate> fødselsdato = familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .flatMap(FamilieHendelseEntitet::getFødselsdato);
        Optional<LocalDate> omsorgsovertalsesdato = familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .flatMap(FamilieHendelseEntitet::getAdopsjon)
            .map(AdopsjonEntitet::getOmsorgsovertakelseDato);
        Optional<LocalDate> termindato = familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .flatMap(FamilieHendelseEntitet::getTerminbekreftelse)
            .map(TerminbekreftelseEntitet::getTermindato);
        return omsorgsovertalsesdato.map(localDate -> (erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, localDate.plusYears(3))
            ? localDate.plusYears(3) : avsluttningsdato)).orElseGet(() -> (fødselsdato.isPresent()
            && erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, fødselsdato.get().plusYears(3)) ? fødselsdato.get().plusYears(3) : termindato.isPresent()
            && erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, termindato.get().plusYears(3)) ? termindato.get().plusYears(3) : avsluttningsdato));
    }

    protected boolean erAvsluttningsdatoIkkeSattEllerEtter(LocalDate avsluttningsdato, LocalDate nyAvsluttningsdato) {
        return avsluttningsdato == null || nyAvsluttningsdato.isBefore(avsluttningsdato);
    }
}
