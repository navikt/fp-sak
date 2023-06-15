package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.felles.ErEndringIBeregning;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;

@BehandlingStegRef(BehandlingStegType.FORESLÅ_VEDTAK)
@BehandlingTypeRef(BehandlingType.REVURDERING) // Revurdering
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class ForeslåVedtakRevurderingStegImpl implements ForeslåVedtakSteg {

    private BeregningTjeneste beregningTjeneste;
    private BehandlingRepository behandlingRepository;
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    ForeslåVedtakRevurderingStegImpl() {
    }

    @Inject
    ForeslåVedtakRevurderingStegImpl(ForeslåVedtakTjeneste foreslåVedtakTjeneste,
            BeregningTjeneste beregningTjeneste,
            BehandlingRepositoryProvider repositoryProvider) {
        this.beregningTjeneste = beregningTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.foreslåVedtakTjeneste = foreslåVedtakTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var revurdering = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var orginalBehandling = getOriginalBehandling(revurdering);

        List<AksjonspunktDefinisjon> aksjonspunkter = new ArrayList<>();
        // Oppretter aksjonspunkt dersom revurdering har mindre beregningsgrunnlag enn orginal
        var revurderingBG = hentBeregningsgrunnlag(revurdering.getId());
        if (revurderingBG.isPresent() && !isBehandlingsresultatAvslåttEllerOpphørt(orginalBehandling) &&
            ErEndringIBeregning.vurderUgunst(revurderingBG, hentBeregningsgrunnlag(orginalBehandling.getId()))) {
            aksjonspunkter.add(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
        }

        var behandleStegResultat = foreslåVedtakTjeneste.foreslåVedtak(revurdering, aksjonspunkter);
        return behandleStegResultat;
    }

    private boolean isBehandlingsresultatAvslåttEllerOpphørt(Behandling orginalBehandling) {
        var sistBehandlingsresultatUtenIngenEndring = getSistBehandlingsresultatUtenIngenEndring(orginalBehandling);
        return sistBehandlingsresultatUtenIngenEndring.isBehandlingsresultatAvslått()
                || sistBehandlingsresultatUtenIngenEndring.isBehandlingsresultatOpphørt();
    }

    private Behandlingsresultat getSistBehandlingsresultatUtenIngenEndring(Behandling orginalBehandling) {
        var sisteBehandling = orginalBehandling;
        var sisteBehandlingResultat = getBehandlingsresultat(orginalBehandling);

        while (sisteBehandlingResultat.isBehandlingsresultatIkkeEndret()) {
            sisteBehandling = getOriginalBehandling(sisteBehandling);
            sisteBehandlingResultat = getBehandlingsresultat(sisteBehandling);
        }

        return sisteBehandlingResultat;
    }

    private Behandling getOriginalBehandling(Behandling behandling) {
        return behandling.getOriginalBehandlingId().map(behandlingRepository::hentBehandling)
                .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Revurdering skal alltid ha orginal behandling"));
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling orginalBehandling) {
        return behandlingsresultatRepository.hent(orginalBehandling.getId());
    }

    private Optional<Beregningsgrunnlag> hentBeregningsgrunnlag(Long behandlingId) {
        return beregningTjeneste.hent(behandlingId).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var behandlingsresultat = getBehandlingsresultat(behandling);
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
                .fjernKonsekvenserForYtelsen()
                .buildFor(behandling);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }
}
