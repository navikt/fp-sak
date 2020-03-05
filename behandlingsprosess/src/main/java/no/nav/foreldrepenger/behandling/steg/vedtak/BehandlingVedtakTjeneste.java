package no.nav.foreldrepenger.behandling.steg.vedtak;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
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
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.impl.BehandlingVedtakEventPubliserer;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
public class BehandlingVedtakTjeneste {

    private BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private OpphørUttakTjeneste opphørUttakTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    BehandlingVedtakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingVedtakTjeneste(BehandlingVedtakEventPubliserer behandlingVedtakEventPubliserer,
                                    BehandlingRepositoryProvider repositoryProvider, OpphørUttakTjeneste opphørUttakTjeneste, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingVedtakEventPubliserer = behandlingVedtakEventPubliserer;
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.opphørUttakTjeneste = opphørUttakTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public void opprettBehandlingVedtak(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, behandling.getFagsak().getYtelseType()).orElseThrow();
        VedtakResultatType vedtakResultatType;
        if (behandling.getFagsakYtelseType().gjelderEngangsstønad()) {
            vedtakResultatType = UtledVedtakResultatTypeES.utled(behandling);
        } else {
            Optional<LocalDate> opphørsdato = Optional.empty();
            Optional<LocalDate> skjæringstidspunkt = Optional.empty();
            if (behandling.erRevurdering()) {
                Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
                var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
                opphørsdato = opphørUttakTjeneste.getOpphørsdato(ref, behandling.getBehandlingsresultat());

                skjæringstidspunkt = skjæringstidspunkter.getSkjæringstidspunktHvisUtledet();
            }
            vedtakResultatType = UtledVedtakResultatType.utled(behandling, opphørsdato, skjæringstidspunkt);
        }
        String ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandling);
        LocalDateTime vedtakstidspunkt = FPDateUtil.nå();

        boolean erRevurderingMedUendretUtfall = revurderingTjeneste.erRevurderingMedUendretUtfall(behandling);
        BehandlingVedtak behandlingVedtak = BehandlingVedtak.builder()
            .medVedtakResultatType(vedtakResultatType)
            .medAnsvarligSaksbehandler(ansvarligSaksbehandler)
            .medVedtakstidspunkt(vedtakstidspunkt)
            .medBehandlingsresultat(behandling.getBehandlingsresultat())
            .medBeslutning(erRevurderingMedUendretUtfall)
            .build();
        behandlingVedtakRepository.lagre(behandlingVedtak, kontekst.getSkriveLås());
        behandlingVedtakEventPubliserer.fireEvent(behandlingVedtak, behandling);
    }
}
