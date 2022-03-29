package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegKoder;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.domene.vedtak.OpprettProsessTaskIverksett;
import no.nav.foreldrepenger.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;

@BehandlingStegRef(kode = BehandlingStegKoder.IVERKSETT_VEDTAK_KODE)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class IverksetteVedtakStegFelles implements IverksetteVedtakSteg {

    private static final Logger LOG = LoggerFactory.getLogger(IverksetteVedtakStegFelles.class);

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private HistorikkRepository historikkRepository;
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
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
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
        var delBuilder = new HistorikkInnslagTekstBuilder();
        delBuilder.medHendelse(HistorikkinnslagType.IVERKSETTELSE_VENT);
        delBuilder.medÅrsak(Venteårsak.VENT_TIDLIGERE_BEHANDLING);

        var historikkinnslag = new Historikkinnslag();
        historikkinnslag.setType(HistorikkinnslagType.IVERKSETTELSE_VENT);
        historikkinnslag.setBehandlingId(behandling.getId());
        historikkinnslag.setFagsakId(behandling.getFagsakId());
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        delBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

}
