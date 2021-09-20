package no.nav.foreldrepenger.domene.vedtak.intern;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
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
        var barna = familieHendelseGrunnlag
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getBarna);

        if (barna.isEmpty() || barna.get().isEmpty() || barna.get().stream().anyMatch(b -> b.getDødsdato().isEmpty()))
            return Optional.empty();
        return barna.get().stream()
            .map(UidentifisertBarn::getDødsdato)
            .flatMap(Optional::stream)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder());
    }

    protected LocalDate avsluttningsdatoHvisBehandlingAvslåttEllerOpphørt(Behandling behandling, LocalDate avsluttningsdato) {
        var familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        if(familieHendelseGrunnlag.isPresent()){

            var sisteDødsdato = sisteDødsdato(familieHendelseGrunnlag);
            if(sisteDødsdato.isPresent()) return sisteDødsdato.get().plusWeeks(StandardKonfigurasjon.KONFIGURASJON.getParameter(Parametertype.UTTAK_ETTER_BARN_DØDT_UKER, LocalDate.now())).plusWeeks(KLAGEFRIST_I_UKER_VED_DØD);

        }

        var behandlingsresultatAvslåttOrOpphørt = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .filter(Behandlingsresultat::isBehandlingsresultatAvslåttOrOpphørt).isPresent();
        return behandlingsresultatAvslåttOrOpphørt
            && erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, LocalDate.now()) ? LocalDate.now().plusDays(1) : avsluttningsdato;
    }

    protected LocalDate finnAvsluttningsdatoForRelaterteBehandlinger(Behandling behandling, LocalDate avsluttningsdato) {
        var uttakInput = uttakInputTjeneste.lagInput(behandling);
        var maksDatoUttak = maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput);

        if(maksDatoUttak.isEmpty()) return avsluttningsdatoHvisDetIkkeErGjortUttak(behandling, avsluttningsdato);
        var avslutningsdatoFraMaksDatoUttak = maksDatoUttak.get().plusDays(1);

        var stønadRest = stønadskontoSaldoTjeneste.finnStønadRest(uttakInput);

        if( stønadRest > 0 ) {
            //rest er allerede lagt til i maksDatoUttak, men der mangler 'buffer'
            avslutningsdatoFraMaksDatoUttak =  avslutningsdatoFraMaksDatoUttak.plusMonths(JUSTERING_I_HELE_MÅNEDER_VED_REST_I_STØNADSDAGER).with(TemporalAdjusters.lastDayOfMonth());
            var gjeldendeFødselsdato = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
                .map(FamilieHendelseGrunnlagEntitet::finnGjeldendeFødselsdato);
            if(gjeldendeFødselsdato.isPresent()){
                avslutningsdatoFraMaksDatoUttak = beskjærMotAbsoluttMaksDato(gjeldendeFødselsdato.get(), avslutningsdatoFraMaksDatoUttak);
            }
        }
        // TODO: diskuter og slett
        //return avslutningsdatoFraMaksDatoUttak.isAfter(LocalDate.now()) && erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, avslutningsdatoFraMaksDatoUttak)?
        //    avslutningsdatoFraMaksDatoUttak : avsluttningsdato;
        return avslutningsdatoFraMaksDatoUttak;
    }

    private LocalDate beskjærMotAbsoluttMaksDato(LocalDate fødselsdato, LocalDate beregnetMaksDato) {
        var absoluttMaksDato = fødselsdato
            .plus(StandardKonfigurasjon.KONFIGURASJON.getParameter(Parametertype.GRENSE_ETTER_FØDSELSDATO, Period.class, LocalDate.now()));
        return absoluttMaksDato.isBefore(beregnetMaksDato)? absoluttMaksDato : beregnetMaksDato;
    }

    protected LocalDate avsluttningsdatoHvisDetIkkeErGjortUttak(Behandling behandling, LocalDate avsluttningsdato) {
        var familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        var fødselsdato = familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .flatMap(FamilieHendelseEntitet::getFødselsdato);
        var omsorgsovertalsesdato = familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .flatMap(FamilieHendelseEntitet::getAdopsjon)
            .map(AdopsjonEntitet::getOmsorgsovertakelseDato);
        var termindato = familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .flatMap(FamilieHendelseEntitet::getTerminbekreftelse)
            .map(TerminbekreftelseEntitet::getTermindato);
        return omsorgsovertalsesdato.or(() -> fødselsdato).or(() -> termindato)
            .filter(hendelseDato -> erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, hendelseDato.plusYears(3)))
            .map(hendelseDato -> hendelseDato.plusYears(3)).orElse(avsluttningsdato);
    }

    protected boolean erAvsluttningsdatoIkkeSattEllerEtter(LocalDate avsluttningsdato, LocalDate nyAvsluttningsdato) {
        return avsluttningsdato == null || nyAvsluttningsdato.isBefore(avsluttningsdato);
    }
}
