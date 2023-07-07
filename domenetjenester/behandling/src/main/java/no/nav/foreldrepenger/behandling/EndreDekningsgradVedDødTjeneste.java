package no.nav.foreldrepenger.behandling;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@ApplicationScoped
public class EndreDekningsgradVedDødTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(EndreDekningsgradVedDødTjeneste.class);

    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingRepository behandlingRepository;
    private HistorikkRepository historikkRepository;
    private BehandlingLåsRepository behandlingLåsRepository;

    EndreDekningsgradVedDødTjeneste() {
        // CDI
    }

    @Inject
    public EndreDekningsgradVedDødTjeneste(FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                           BehandlingsresultatRepository behandlingsresultatRepository,
                                           BehandlingRepository behandlingRepository,
                                           HistorikkRepository historikkRepository, BehandlingLåsRepository behandlingLåsRepository) {
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.historikkRepository = historikkRepository;
        this.behandlingLåsRepository = behandlingLåsRepository;
    }

    public void endreDekningsgradTil100(BehandlingReferanse ref) {
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        var nåværendeDekningsgrad = finnDekningsgrad(ref.saksnummer());
        if (nåværendeDekningsgrad.isEmpty()) {
            throw new IllegalStateException("Prøver å endre dekningsgrad uten at denne er satt.");
        }
        if (nåværendeDekningsgrad.get().equals(Dekningsgrad._100)) {
            // Eneste kjente case for å endre dekningsgrad er ved barnets død, som alltid skal ha dekningsgrad på 100.
            // Om denne allerede er satt trenger vi ikke endre
            return;
        }
        oppdaterFagsakRelasjon(behandling, Dekningsgrad._100);
        oppdaterBehandlingsresultat(behandling);
        lagHistorikkinnslag(behandling);
        LOG.info("Endrer dekningsgrad for saksnummer " + ref.saksnummer() + " automatisk på grunn av død");
    }

    private void lagHistorikkinnslag(Behandling behandling) {
        var nyeRegisteropplysningerInnslag = new Historikkinnslag();
        nyeRegisteropplysningerInnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        nyeRegisteropplysningerInnslag.setType(HistorikkinnslagType.ENDRET_DEKNINGSGRAD);
        nyeRegisteropplysningerInnslag.setBehandlingId(behandling.getId());
        var historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.ENDRET_DEKNINGSGRAD)
            .medBegrunnelse("Dekningsgraden er endret fra 80% til 100% grunnet opplysninger om død");
        historieBuilder.build(nyeRegisteropplysningerInnslag);
        historikkRepository.lagre(nyeRegisteropplysningerInnslag);
    }

    private Optional<Dekningsgrad> finnDekningsgrad(Saksnummer saksnummer) {
        return fagsakRelasjonTjeneste.finnRelasjonHvisEksisterer(saksnummer).map(FagsakRelasjon::getGjeldendeDekningsgrad);
    }

    private void oppdaterFagsakRelasjon(Behandling behandling, Dekningsgrad nyDekningsgrad) {
        fagsakRelasjonTjeneste.overstyrDekningsgrad(behandling.getFagsak(), nyDekningsgrad);
    }

    private void oppdaterBehandlingsresultat(Behandling behandling) {
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultatRepository.hent(behandling.getId()))
            .medEndretDekningsgrad(true)
            .buildFor(behandling);
        behandlingRepository.lagre(behandling, behandlingLåsRepository.taLås(behandling.getId()));
    }
}
