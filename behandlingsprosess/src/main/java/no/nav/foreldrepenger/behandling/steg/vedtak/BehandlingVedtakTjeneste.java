package no.nav.foreldrepenger.behandling.steg.vedtak;

import java.time.LocalDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.es.UtledVedtakResultatTypeES;
import no.nav.foreldrepenger.behandling.fp.UtledVedtakResultatType;
import no.nav.foreldrepenger.behandling.impl.FinnAnsvarligSaksbehandler;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;

@ApplicationScoped
public class BehandlingVedtakTjeneste {

    private BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingRepository behandlingRepository;

    BehandlingVedtakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingVedtakTjeneste(BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer,
            BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingVedtakEventPubliserer = behandlingVedtakEventPubliserer;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    public void opprettBehandlingVedtak(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsak().getYtelseType())
                .orElseThrow();
        var vedtakResultatType = utledVedtakResultatType(behandling);
        var ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandling);
        var vedtakstidspunkt = LocalDateTime.now();

        var erRevurderingMedUendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(behandling);
        var behandlingsresultat = behandlingsresultatRepository.hent(behandling.getId());
        var behandlingVedtak = BehandlingVedtak.builder()
                .medVedtakResultatType(vedtakResultatType)
                .medAnsvarligSaksbehandler(ansvarligSaksbehandler)
                .medVedtakstidspunkt(vedtakstidspunkt)
                .medBehandlingsresultat(behandlingsresultat)
                .medBeslutning(erRevurderingMedUendretUtfall)
                .medIverksettingStatus(IverksettingStatus.IKKE_IVERKSATT)
                .build();
        behandlingVedtakRepository.lagre(behandlingVedtak, kontekst.getSkriveLås());
        behandlingVedtakEventPubliserer.fireEvent(behandlingVedtak, behandling);
    }

    public Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

    private VedtakResultatType utledVedtakResultatType(Behandling behandling) {
        var behandlingResultatType = getBehandlingsresultat(behandling.getId()).getBehandlingResultatType();
        if (FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            return UtledVedtakResultatTypeES.utled(behandling.getType(), behandlingResultatType);
        }
        if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingResultatType)) {
            var original = behandling.getOriginalBehandlingId().map(behandlingRepository::hentBehandling)
                    .orElseThrow(() -> new IllegalStateException("INGEN ENDRING uten original behandling"));
            return utledVedtakResultatType(original);
        }
        return UtledVedtakResultatType.utled(behandling.getType(), behandlingResultatType);
    }
}
