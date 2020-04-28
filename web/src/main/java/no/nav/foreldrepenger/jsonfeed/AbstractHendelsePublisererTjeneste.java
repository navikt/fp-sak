package no.nav.foreldrepenger.jsonfeed;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.EtterkontrollRepository;
import no.nav.foreldrepenger.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.feed.FpVedtakUtgåendeHendelse;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerEndret;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerInnvilget;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.ForeldrepengerOpphoert;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Innhold;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.Meldingstype;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.xml.vedtak.v2.Vedtak;

public abstract class AbstractHendelsePublisererTjeneste implements HendelsePublisererTjeneste {
    private static final Logger log = LoggerFactory.getLogger(AbstractHendelsePublisererTjeneste.class);
    protected static final String FAGSAK_PREFIX = "FS";
    protected static final String VEDTAK_PREFIX = "VT";

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private EtterkontrollRepository etterkontrollRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    public AbstractHendelsePublisererTjeneste() {
        //Creatively Diversified Investments
    }

    public AbstractHendelsePublisererTjeneste(BehandlingsresultatRepository behandlingsresultatRepository,
                                              EtterkontrollRepository etterkontrollRepository,
                                              BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.etterkontrollRepository = etterkontrollRepository;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository()
        this.beregningsresultatRepository = behandlingRepositoryProvider.getBeregningsresultatRepository();
    }

    @Override
    public void lagreVedtak(BehandlingVedtak vedtak) {
        log.info("lagrer utgående hendelse for vedtak {}", vedtak.getId());

        Behandling behandling = behandlingRepository.hentBehandling(vedtak.getBehandlingsresultat().getBehandlingId());
        BehandlingType behandlingType = behandling.getType();

        if (hendelseEksistererAllerede(vedtak)) {
            log.debug("Skipper lagring av hendelse av vedtakId {} fordi den allerede eksisterer", vedtak.getId());
            return;
        }

        if (vedtak.getVedtakResultatType().equals(VedtakResultatType.AVSLAG) || vedtak.getVedtakResultatType().equals(VedtakResultatType.OPPHØR)){
            etterkontrollRepository.avflaggDersomEksisterer(behandling.getFagsakId(), KontrollType.MANGLENDE_FØDSEL);
        }

        //Disse trigger ikke hendelser
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsak().getYtelseType())
            || !behandling.getType().erYtelseBehandlingType()
            || vedtak.getBehandlingsresultat().getBehandlingResultatType().erHenlagt()) {
            return;
        }

        nyDoLagreVedtak(vedtak, behandling);
    }

    private void nyDoLagreVedtak(BehandlingVedtak vedtak, Behandling behandling) {

        Meldingstype meldingstype;
        BehandlingType behandlingType = behandling.getType();

        Optional<LocalDateInterval> innvilgetPeriode = finnPeriode(behandling);
        Optional<LocalDateInterval> orginalPeriode = Optional.empty();

        if (behandling.getOriginalBehandling().isPresent()) {
            orginalPeriode = finnPeriode(behandling.getOriginalBehandling().get());
        }

        if (innvilgetPeriode.isEmpty() && orginalPeriode.isEmpty()) {
            return;
        }

        if (!innvilgetPeriode.isEmpty() && orginalPeriode.isEmpty()) {
            meldingstype = Meldingstype.FORELDREPENGER_INNVILGET;
        } else if (innvilgetPeriode.isEmpty() && !orginalPeriode.isEmpty()) {
            meldingstype = Meldingstype.FORELDREPENGER_OPPHOERT;
        } else if (!innvilgetPeriode.get().equals(orginalPeriode.get())) {
            meldingstype = Meldingstype.FORELDREPENGER_ENDRET;
        } else {
            //revurdering, men ingen endring i utbetalingsperiode
            return;
        }

        Innhold innhold = nyMapVedtakTilInnhold();
        String payloadJason = JsonMapper.toJson(innhold);

        FpVedtakUtgåendeHendelse fpVedtakUtgåendeHendelse = FpVedtakUtgåendeHendelse.builder()
            .aktørId(behandling.getAktørId().getId())
            .payload(payloadJason)
            .type(meldingstype.getType())
            .kildeId(VEDTAK_PREFIX + vedtak.getId())
            .build();
        feedRepository.lagre(fpVedtakUtgåendeHendelse);
    }

    Optional<LocalDateInterval> finnPeriode(Behandling behandling ){
        Optional<LocalDate> førsteUtbetDato = finnMinsteUtbetDato(behandling.getId());
        Optional<LocalDate> sisteUtbetDato = finnSisteUtbetDato(behandling.getId());

        if (førsteUtbetDato.isPresent() && sisteUtbetDato.isPresent()) {
            return Optional.of(new LocalDateInterval(førsteUtbetDato.get(), sisteUtbetDato.get()));
        }
        else {
            return Optional.empty();
        }
    }

    private Optional<LocalDate> finnMinsteUtbetDato(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);

        return berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder())
            .map(this::fomMandag);
    }


    private Optional<LocalDate> finnSisteUtbetDato(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);
        return berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
            .max(Comparator.naturalOrder())
            .map(this::tomFredag);
    }

    private Innhold nyMapVedtakTilInnhold(BehandlingVedtak vedtak, Behandling behandling) {
        Innhold innhold;
        BehandlingType behandlingType = behandling.getType();
        Optional<Behandling> originalBehandling = Optional.empty();

        if (erFørstegangsSøknad(behandlingType) || erInnvilgetRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            innhold = new ForeldrepengerInnvilget();
        } else if (erOpphørtRevurdering(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())) {
            innhold = new ForeldrepengerOpphoert();
            originalBehandling = behandling.getOriginalBehandling();
        } else {
            innhold = new ForeldrepengerEndret();
            originalBehandling = behandling.getOriginalBehandling();
        }

        if (originalBehandling.isPresent() && !uttakFomEllerTomErEndret(originalBehandling.get().getId(), behandling.getId())) {// NOSONAR
            throw HendelsePublisererFeil.FACTORY.fantIngenEndringIUttakFomEllerTom().toException();
        }

        Optional<LocalDate> førsteUtbetDato = finnMinsteUtbetDato(behandling.getId());
        Optional<LocalDate> sisteUtbetDato = finnSisteUtbetDato(behandling.getId());

        if (førsteUtbetDato.isEmpty()) {
            if (originalBehandling.isPresent()){
                førsteUtbetDato = finnMinsteUtbetDato(originalBehandling.get().getId());
                sisteUtbetDato = førsteUtbetDato;
            }
            //finner ingen minste dato, noe er feil
            if (førsteUtbetDato.isEmpty()) {
                throw HendelsePublisererFeil.FACTORY.finnerIkkeRelevantUttaksplanForVedtak().toException();
            }
        }
        // Hvis det ikke finnes noen innvilgede perioder etter revurdering så vil hendelsen være at ytelsen opphører samme dag som den opprinnelig ble innvilget
        if (sisteUtbetDato.isEmpty()) {
            sisteUtbetDato = førsteUtbetDato;
        }

        førsteUtbetDato.ifPresent(innhold::setFoersteStoenadsdag);
        sisteUtbetDato.ifPresent(innhold::setSisteStoenadsdag);
        innhold.setAktoerId(behandling.getAktørId().getId());
        innhold.setGsakId(behandling.getFagsak().getSaksnummer().getVerdi());

        return innhold;
    }

    private boolean erRevurderingshendelse(Behandling behandling, BehandlingVedtak vedtak) {
       return behandling.erRevurdering() && !vedtak.getBehandlingsresultat().getBehandlingResultatType().erHenlagt();
     }

    protected abstract boolean hendelseEksistererAllerede(BehandlingVedtak vedtak);

    protected abstract void doLagreVedtak(BehandlingVedtak vedtak, Behandling behandling);

    private boolean erInnvilgetFørstegangssøknad(BehandlingVedtak vedtak, BehandlingType behandlingType) {
        return VedtakResultatType.INNVILGET.equals(vedtak.getVedtakResultatType())
            && (erFørstegangsSøknad(behandlingType));
    }

    private boolean erEndringUtenEndretPeriode(Behandling behandling) {
        BehandlingType behandlingType = behandling.getType();

        if (!erEndring(behandlingType)) {
            return false;
        }
        Optional<Behandling> originalBehandling = behandling.getOriginalBehandling();
        if (!originalBehandling.isPresent()) {
            throw HendelsePublisererFeil.FACTORY.manglerOriginialBehandlingPåEndringsVedtak().toException();
        }
        return !uttakFomEllerTomErEndret(originalBehandling.get().getId(), behandling.getId());
    }

    protected abstract boolean uttakFomEllerTomErEndret(Long orginalbehId, Long behandlingId);

    protected boolean erInnvilgetRevurdering(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType) {
        return BehandlingType.REVURDERING.equals(behandlingType) && BehandlingResultatType.INNVILGET.equals(behandlingResultatType);
    }

    protected boolean erFørstegangsSøknad(BehandlingType behandlingType) {
        return BehandlingType.FØRSTEGANGSSØKNAD.equals(behandlingType);
    }

    protected boolean erOpphørtRevurdering(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType) {
        return BehandlingType.REVURDERING.equals(behandlingType) && BehandlingResultatType.OPPHØR.equals(behandlingResultatType);
    }

    private boolean erBeslutningsvedtak(BehandlingType behandlingType, BehandlingResultatType behandlingResultatType) {
        return BehandlingType.REVURDERING.equals(behandlingType) && BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType);
    }

    private boolean erEndring(BehandlingType behandlingType) {
        return BehandlingType.REVURDERING.equals(behandlingType);
    }

    private boolean erAvslagPåAvslag(Behandling behandling) {
        if(behandling.erRevurdering()) {
            Optional<Behandling> origBehandling = behandling.getOriginalBehandling();
            if (origBehandling.isPresent()) {
                return erAvslåttBehandling(behandling) && erAvslåttBehandling(origBehandling.get());
            }
        }
        return false;
    }

    private boolean erAvslåttBehandling(Behandling behandling) {
        if (hentBehandlingsresultat(behandling).isPresent()) {
            return hentBehandlingsresultat(behandling).get().isBehandlingsresultatAvslått();
        }
        return false;
    }

    private Optional<Behandlingsresultat> hentBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
    }

}
