package no.nav.foreldrepenger.jsonfeed;

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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public abstract class AbstractHendelsePublisererTjeneste implements HendelsePublisererTjeneste {
    private static final Logger log = LoggerFactory.getLogger(AbstractHendelsePublisererTjeneste.class);
    protected static final String FAGSAK_PREFIX = "FS";
    protected static final String VEDTAK_PREFIX = "VT";

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private EtterkontrollRepository etterkontrollRepository;
    private BehandlingRepository behandlingRepository;

    public AbstractHendelsePublisererTjeneste() {
        //Creatively Diversified Investments
    }

    public AbstractHendelsePublisererTjeneste(BehandlingsresultatRepository behandlingsresultatRepository,
                                              EtterkontrollRepository etterkontrollRepository,
                                              BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.etterkontrollRepository = etterkontrollRepository;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
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

        if (!(erInnvilgetFørstegangssøknad(vedtak, behandlingType) || erEndring(behandlingType))
            || erBeslutningsvedtak(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())
            || FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsak().getYtelseType())
            || erEndringUtenEndretPeriode(behandling) || erAvslagPåAvslag(behandling)) {
            return; //dette vedtaket trigger ingen hendelse
        }

        doLagreVedtak(vedtak, behandling);
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
