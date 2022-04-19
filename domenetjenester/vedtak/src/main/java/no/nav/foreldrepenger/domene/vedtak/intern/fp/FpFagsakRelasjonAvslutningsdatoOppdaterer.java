package no.nav.foreldrepenger.domene.vedtak.intern.fp;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.laas.FagsakRelasjonLås;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.saldo.StønadskontoSaldoTjeneste;
import no.nav.foreldrepenger.domene.vedtak.intern.FagsakRelasjonAvslutningsdatoOppdaterer;
import no.nav.foreldrepenger.regler.uttak.konfig.Parametertype;
import no.nav.foreldrepenger.regler.uttak.konfig.StandardKonfigurasjon;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class FpFagsakRelasjonAvslutningsdatoOppdaterer implements FagsakRelasjonAvslutningsdatoOppdaterer {

    private static final int JUSTERING_I_HELE_MÅNEDER_VED_REST_I_STØNADSDAGER = 3;
    static final int KLAGEFRIST_I_UKER_VED_DØD = 6;

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    private StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;


    @Inject
    public FpFagsakRelasjonAvslutningsdatoOppdaterer(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                     StønadskontoSaldoTjeneste stønadskontoSaldoTjeneste,
                                                     UttakInputTjeneste uttakInputTjeneste,
                                                     @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) MaksDatoUttakTjeneste fpMaksDatoUttakTjeneste,
                                                     FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = behandlingRepositoryProvider.getBehandlingsresultatRepository();
        this.familieHendelseRepository = behandlingRepositoryProvider.getFamilieHendelseRepository();
        this.stønadskontoSaldoTjeneste = stønadskontoSaldoTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.maksDatoUttakTjeneste = fpMaksDatoUttakTjeneste;
    }

    @Override
    public void oppdaterFagsakRelasjonAvsluttningsdato(FagsakRelasjon relasjon,
                                                Long fagsakId,
                                                FagsakRelasjonLås lås,
                                                Optional<FagsakLås> fagsak1Lås,
                                                Optional<FagsakLås> fagsak2Lås) {
        var avslutningsdato = finnAvslutningsdato(fagsakId, relasjon);
        fagsakRelasjonTjeneste.oppdaterMedAvsluttningsdato(relasjon, avslutningsdato, lås, fagsak1Lås, fagsak2Lås);
    }

    LocalDate finnAvslutningsdato(Long fagsakId, FagsakRelasjon fagsakRelasjon) {
        var avsluttningsdato = avsluttningsdatoFraEksisterendeFagsakRelasjon(fagsakRelasjon);
        var behandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
        if (behandling.isPresent()) {
            var datoDersomBarnDør = avslutningsDatoTilfelleBarnDør(behandling.get());
            avsluttningsdato = avsluttningsdatoHvisBehandlingAvslåttEllerOpphørt(behandling.get(), avsluttningsdato);
            avsluttningsdato = finnAvsluttningsdatoForRelaterteBehandlinger(behandling.get(), avsluttningsdato);
            if (datoDersomBarnDør.isPresent()) {
                return avsluttningsdato != null && avsluttningsdato.isAfter(datoDersomBarnDør.get()) ? avsluttningsdato : datoDersomBarnDør.get();
            }
        }

        return Optional.ofNullable(avsluttningsdato).orElseGet(() -> LocalDate.now().plusDays(1));
    }

    private static LocalDate avsluttningsdatoFraEksisterendeFagsakRelasjon(FagsakRelasjon fagsakRelasjon) {
        if (fagsakRelasjon.getAvsluttningsdato() != null && fagsakRelasjon.getAvsluttningsdato().isAfter(LocalDate.now())) {
            return fagsakRelasjon.getAvsluttningsdato();
        }
        return null;
    }

    private static Optional<LocalDate> sisteDødsdato(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        return familieHendelseGrunnlag.getGjeldendeVersjon().getBarna().stream()
            .filter(b -> b.getDødsdato().isPresent())
            .flatMap(b -> b.getDødsdato().stream())
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder());
    }

    private Optional<LocalDate> avslutningsDatoTilfelleBarnDør(Behandling behandling) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .flatMap(FpFagsakRelasjonAvslutningsdatoOppdaterer::sisteDødsdato)
            .map(d -> d.plusWeeks(StandardKonfigurasjon.KONFIGURASJON.getParameter(Parametertype.UTTAK_ETTER_BARN_DØDT_UKER, LocalDate.now())))
            .map(d -> d.plusWeeks(KLAGEFRIST_I_UKER_VED_DØD));
    }

    private LocalDate avsluttningsdatoHvisBehandlingAvslåttEllerOpphørt(Behandling behandling, LocalDate avsluttningsdato) {

        var behandlingsresultatAvslåttOrOpphørt = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .filter(Behandlingsresultat::isBehandlingsresultatAvslåttOrOpphørt).isPresent();
        return behandlingsresultatAvslåttOrOpphørt
            && erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, LocalDate.now()) ? LocalDate.now().plusDays(1) : avsluttningsdato;
    }

    private LocalDate finnAvsluttningsdatoForRelaterteBehandlinger(Behandling behandling, LocalDate avsluttningsdato) {
        var uttakInput = uttakInputTjeneste.lagInput(behandling);
        var sammenhengendeUttak = uttakInput.getBehandlingReferanse().getSkjæringstidspunkt().kreverSammenhengendeUttak();
        var stønadRest = stønadskontoSaldoTjeneste.finnStønadRest(uttakInput);
        var maksDatoUttak = maksDatoUttakTjeneste.beregnMaksDatoUttakSakskompleks(uttakInput, stønadRest);

        if (maksDatoUttak.isEmpty() || (!sammenhengendeUttak && stønadRest > 0)) {
            return avsluttningsdatoHvisDetIkkeErGjortUttak(behandling, avsluttningsdato);
        }
        var avslutningsdatoFraMaksDatoUttak = maksDatoUttak.get().plusDays(1);

        if ( stønadRest > 0 ) { // For sammenhengende uttak - fritt uttak venter 3 år - se ovenfor
            //rest er allerede lagt til i maksDatoUttak, men der mangler 'buffer'
            avslutningsdatoFraMaksDatoUttak =  avslutningsdatoFraMaksDatoUttak.plusMonths(JUSTERING_I_HELE_MÅNEDER_VED_REST_I_STØNADSDAGER).with(TemporalAdjusters.lastDayOfMonth());
            var gjeldendeFødselsdato = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseEntitet::getSkjæringstidspunkt);
            if (gjeldendeFødselsdato.isPresent()) {
                avslutningsdatoFraMaksDatoUttak = beskjærMotAbsoluttMaksDato(gjeldendeFødselsdato.get(), avslutningsdatoFraMaksDatoUttak);
            }
        }
        return avslutningsdatoFraMaksDatoUttak;
    }

    private static LocalDate beskjærMotAbsoluttMaksDato(LocalDate fødselsdato, LocalDate beregnetMaksDato) {
        var absoluttMaksDato = fødselsdato
            .plusYears(StandardKonfigurasjon.KONFIGURASJON.getParameter(Parametertype.GRENSE_ETTER_FØDSELSDATO_ÅR, LocalDate.now()));
        return absoluttMaksDato.isBefore(beregnetMaksDato)? absoluttMaksDato : beregnetMaksDato;
    }

    private LocalDate avsluttningsdatoHvisDetIkkeErGjortUttak(Behandling behandling, LocalDate avsluttningsdato) {
        var familieHendelseDato = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt);
        return familieHendelseDato
            .filter(hendelseDato -> erAvsluttningsdatoIkkeSattEllerEtter(avsluttningsdato, hendelseDato.plusYears(3)))
            .map(hendelseDato -> hendelseDato.plusYears(3)).orElse(avsluttningsdato);
    }

    private static boolean erAvsluttningsdatoIkkeSattEllerEtter(LocalDate avsluttningsdato, LocalDate nyAvsluttningsdato) {
        return avsluttningsdato == null || nyAvsluttningsdato.isBefore(avsluttningsdato);
    }

}
