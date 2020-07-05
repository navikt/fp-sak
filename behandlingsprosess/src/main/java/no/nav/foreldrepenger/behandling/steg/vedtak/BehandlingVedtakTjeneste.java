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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.IverksettingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;

@ApplicationScoped
public class BehandlingVedtakTjeneste {

    private BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    BehandlingVedtakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingVedtakTjeneste(BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer,
                                    BehandlingRepositoryProvider repositoryProvider) {
        this.behandlingVedtakEventPubliserer = behandlingVedtakEventPubliserer;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
    }

    public void opprettBehandlingVedtak(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsak().getYtelseType()).orElseThrow();
        VedtakResultatType vedtakResultatType;
        if (behandling.getFagsakYtelseType().gjelderEngangsstønad()) {
            vedtakResultatType = UtledVedtakResultatTypeES.utled(behandling);
        } else {
            vedtakResultatType = UtledVedtakResultatType.utled(behandling);
        }
        String ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandling);
        LocalDateTime vedtakstidspunkt = LocalDateTime.now();

        boolean erRevurderingMedUendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(behandling);
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder()
            .medVedtakResultatType(vedtakResultatType)
            .medAnsvarligSaksbehandler(ansvarligSaksbehandler)
            .medVedtakstidspunkt(vedtakstidspunkt)
            .medBehandlingsresultat(behandling.getBehandlingsresultat())
            .medBeslutning(erRevurderingMedUendretUtfall)
            .medIverksettingStatus(IverksettingStatus.IKKE_IVERKSATT)
            .build();
        behandlingVedtakRepository.lagre(behandlingVedtak, kontekst.getSkriveLås());
        behandlingVedtakEventPubliserer.fireEvent(behandlingVedtak, behandling);
    }
}
