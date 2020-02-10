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
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public abstract class AbstractHendelsePublisererTjeneste implements HendelsePublisererTjeneste {
    private static final Logger log = LoggerFactory.getLogger(AbstractHendelsePublisererTjeneste.class);
    protected static final String FAGSAK_PREFIX = "FS";
    protected static final String VEDTAK_PREFIX = "VT";

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private EtterkontrollRepository etterkontrollRepository;

    public AbstractHendelsePublisererTjeneste() {
        //Creatively Diversified Investments
    }

    public AbstractHendelsePublisererTjeneste(BehandlingsresultatRepository behandlingsresultatRepository, 
                                              EtterkontrollRepository etterkontrollRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.etterkontrollRepository = etterkontrollRepository;
    }

    @Override
    public void lagreVedtak(BehandlingVedtak vedtak) {
        log.info("lagrer utgående hendelse for vedtak {}", vedtak.getId());

        if (hendelseEksistererAllerede(vedtak)) {
            log.debug("Skipper lagring av hendelse av vedtakId {} fordi den allerede eksisterer", vedtak.getId());
            return;
        }

        if (vedtak.getVedtakResultatType().equals(VedtakResultatType.AVSLAG) || vedtak.getVedtakResultatType().equals(VedtakResultatType.OPPHØR)){
            etterkontrollRepository.avflaggDersomEksisterer(vedtak.getBehandlingsresultat().getBehandling().getFagsakId(), KontrollType.MANGLENDE_FØDSEL);
        }

        BehandlingType behandlingType = vedtak.getBehandlingsresultat().getBehandling().getType();

        if (!(erInnvilgetFørstegangssøknad(vedtak, behandlingType) || erEndring(behandlingType))
            || erBeslutningsvedtak(behandlingType, vedtak.getBehandlingsresultat().getBehandlingResultatType())
            || FagsakYtelseType.ENGANGSTØNAD.equals(vedtak.getBehandlingsresultat().getBehandling().getFagsak().getYtelseType())
            || erEndringUtenEndretPeriode(vedtak) || erAvslagPåAvslag(vedtak)) {
            return; //dette vedtaket trigger ingen hendelse
        }

        doLagreVedtak(vedtak, behandlingType);
    }

    protected abstract boolean hendelseEksistererAllerede(BehandlingVedtak vedtak);

    protected abstract void doLagreVedtak(BehandlingVedtak vedtak, BehandlingType behandlingType);

    private boolean erInnvilgetFørstegangssøknad(BehandlingVedtak vedtak, BehandlingType behandlingType) {
        return VedtakResultatType.INNVILGET.equals(vedtak.getVedtakResultatType())
            && (erFørstegangsSøknad(behandlingType));
    }

    private boolean erEndringUtenEndretPeriode(BehandlingVedtak vedtak) {
        if (!erEndring(vedtak.getBehandlingsresultat().getBehandling().getType())) {
            return false;
        }
        Optional<Behandling> originalBehandling = vedtak.getBehandlingsresultat().getBehandling().getOriginalBehandling();
        if (!originalBehandling.isPresent()) {
            throw HendelsePublisererFeil.FACTORY.manglerOriginialBehandlingPåEndringsVedtak().toException();
        }
        return !uttakFomEllerTomErEndret(hentBehandlingsresultat(originalBehandling.get()), vedtak.getBehandlingsresultat());
    }

    protected abstract boolean uttakFomEllerTomErEndret(Optional<Behandlingsresultat> gammeltResultat, Behandlingsresultat nyttResultat);

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

    private boolean erAvslagPåAvslag(BehandlingVedtak vedtak) {
        Behandling behandling = vedtak.getBehandlingsresultat().getBehandling();
        if(behandling.erRevurdering()) {
            Optional<Behandling> origBehandling = vedtak.getBehandlingsresultat().getBehandling().getOriginalBehandling();
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

    protected Optional<Behandlingsresultat> hentBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
    }
}
