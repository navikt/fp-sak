package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;

@BehandlingStegRef(BehandlingStegType.IVERKSETT_VEDTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class IverksetteVedtakStegFelles implements IverksetteVedtakSteg {

    private static final Logger LOG = LoggerFactory.getLogger(IverksetteVedtakStegFelles.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private Historikkinnslag2Repository historikkinnslag2Repository;
    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;
    private VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse;

    public IverksetteVedtakStegFelles() {
        // for CDI proxy
    }

    @Inject
    public IverksetteVedtakStegFelles(BehandlingRepositoryProvider repositoryProvider,
            OpprettProsessTaskIverksett opprettProsessTaskIverksett,
            VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.historikkinnslag2Repository = repositoryProvider.getHistorikkinnslag2Repository();
        this.opprettProsessTaskIverksett = opprettProsessTaskIverksett;
        this.tidligereBehandlingUnderIverksettelse = tidligereBehandlingUnderIverksettelse;
    }

    @Override
    public final BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        var fantVedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId);
        if (behandlingsresultat.isBehandlingHenlagt() && fantVedtak.isEmpty()) {
            // Gå til avslutning. Dersom alle henlagte skal innom her så bør man sjekke
            // behov for Berørt og andre IVED.VENTER
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        if (fantVedtak.isEmpty()) {
            throw new IllegalStateException(String.format("Utviklerfeil: Kan ikke iverksette, behandling mangler vedtak %s", behandlingId));
        }
        var vedtak = fantVedtak.get();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        if (IverksettingStatus.IVERKSATT.equals(vedtak.getIverksettingStatus())) {
            LOG.info("Behandling {}: Iverksetting allerede fullført", kontekst.getBehandlingId());
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        var venteårsakOpt = kanBegynneIverksetting(behandling);
        if (venteårsakOpt.filter(Venteårsak.VENT_TIDLIGERE_BEHANDLING::equals).isPresent()) {
            LOG.info("Behandling {}: Iverksetting venter på annen behandling", behandlingId);
            // Bruker transisjon startet for "prøv utførSteg senere". Stegstatus VENTER
            // betyr "under arbeid" (suspendert).
            // Behandlingsprosessen stopper og denne behandlingen blir plukket opp av
            // avsluttBehandling.
            return BehandleStegResultat.startet();
        }
        LOG.info("Behandling {}: Iverksetter vedtak", behandlingId);
        opprettProsessTaskIverksett.opprettIverksettingTasks(behandling);
        return BehandleStegResultat.settPåVent();
    }

    @Override
    public final BehandleStegResultat gjenopptaSteg(BehandlingskontrollKontekst kontekst) {
        LOG.info("Behandling {}: Iverksetting fullført", kontekst.getBehandlingId());
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    public Optional<Venteårsak> kanBegynneIverksetting(Behandling behandling) {
        if (behandling.erYtelseBehandling() && tidligereBehandlingUnderIverksettelse.vurder(behandling)) {
            opprettHistorikkinnslagNårIverksettelsePåVent(behandling);
            return Optional.of(Venteårsak.VENT_TIDLIGERE_BEHANDLING);
        }
        return Optional.empty();
    }

    private void opprettHistorikkinnslagNårIverksettelsePåVent(Behandling behandling) {
        var historikkinnslag = new Historikkinnslag2.Builder().medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medTittel("Behandlingen venter på iverksettelse")
            .medAktør(HistorikkAktør.VEDTAKSLØSNINGEN)
            .addLinje("Venter på iverksettelse av en tidligere behandling i denne saken")
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }

}
