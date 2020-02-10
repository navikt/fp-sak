package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

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
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;

@BehandlingStegRef(kode = "FORVEDSTEG")
@BehandlingTypeRef("BT-004") //Revurdering
@FagsakYtelseTypeRef("FP")
@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class ForeslåVedtakRevurderingStegImpl implements ForeslåVedtakSteg {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BehandlingRepository behandlingRepository;
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    ForeslåVedtakRevurderingStegImpl() {
    }

    @Inject
    ForeslåVedtakRevurderingStegImpl(ForeslåVedtakTjeneste foreslåVedtakTjeneste,
                                     HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                     BehandlingRepositoryProvider repositoryProvider) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.foreslåVedtakTjeneste = foreslåVedtakTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling revurdering = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Behandling orginalBehandling = getOriginalBehandling(revurdering);

        BehandleStegResultat behandleStegResultat = foreslåVedtakTjeneste.foreslåVedtak(revurdering, kontekst);

        Optional<BeregningsgrunnlagEntitet> revurderingBG = hentBeregningsgrunnlag(revurdering.getId());
        if (revurderingBG.isEmpty() || isBehandlingsresultatAvslåttEllerOpphørt(orginalBehandling)) {
            return behandleStegResultat;
        }

        //Oppretter aksjonspunkt dersom revurdering har mindre beregningsgrunnlag enn orginal
        if (ErEndringIBeregning.vurderUgunst(revurderingBG, hentBeregningsgrunnlag(orginalBehandling.getId()))) {
            List<AksjonspunktDefinisjon> aksjonspunkter = behandleStegResultat.getAksjonspunktResultater().stream()
                .map(AksjonspunktResultat::getAksjonspunktDefinisjon).collect(Collectors.toList());
            aksjonspunkter.add(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
            return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
        }
        return behandleStegResultat;
    }

    private boolean isBehandlingsresultatAvslåttEllerOpphørt(Behandling orginalBehandling) {
        Behandlingsresultat sistBehandlingsresultatUtenIngenEndring = getSistBehandlingsresultatUtenIngenEndring(orginalBehandling);
        return sistBehandlingsresultatUtenIngenEndring.isBehandlingsresultatAvslått()
            || sistBehandlingsresultatUtenIngenEndring.isBehandlingsresultatOpphørt();
    }

    private Behandlingsresultat getSistBehandlingsresultatUtenIngenEndring(Behandling orginalBehandling) {
        Behandlingsresultat sisteBehandlingResultat = getBehandlingsresultat(orginalBehandling);

        while (sisteBehandlingResultat.isBehandlingsresultatIkkeEndret()) {
            sisteBehandlingResultat = getBehandlingsresultat(getOriginalBehandling(sisteBehandlingResultat.getBehandling()));
        }

        return sisteBehandlingResultat;
    }

    private Behandling getOriginalBehandling(Behandling behandling) {
        return behandling.getOriginalBehandling()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Revurdering skal alltid ha orginal behandling"));
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling orginalBehandling) {
        return behandlingsresultatRepository.hent(orginalBehandling.getId());
    }

    private Optional<BeregningsgrunnlagEntitet> hentBeregningsgrunnlag(Long behandlingId) {
        return beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(behandlingId);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat)
            .fjernKonsekvenserForYtelsen()
            .buildFor(behandling);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }
}
