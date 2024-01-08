package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.ForeldrepengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.kodeverk.Dekningsgrad;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.EndreDekningsgradVedDødTjeneste;
import no.nav.foreldrepenger.behandling.VurderDekningsgradVedDødsfallAksjonspunktUtleder;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG)
@BehandlingTypeRef
@ApplicationScoped
public class ForeslåBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private EndreDekningsgradVedDødTjeneste endreDekningsgradVedDødTjeneste;

    protected ForeslåBeregningsgrunnlagSteg() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                         FamilieHendelseRepository familieHendelseRepository,
                                         BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                         BeregningsgrunnlagInputProvider inputTjenesteProvider,
                                         EndreDekningsgradVedDødTjeneste endreDekningsgradVedDødTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.endreDekningsgradVedDødTjeneste = endreDekningsgradVedDødTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        var input = getInputTjeneste(ref.fagsakYtelseType()).lagInput(ref.behandlingId());
        var resultat = beregningsgrunnlagKopierOgLagreTjeneste.foreslåBeregningsgrunnlag(input);

        var aksjonspunkter = resultat.getAksjonspunkter().stream().map(BeregningAksjonspunktResultatMapper::map)
                .toList();

        if (behandling.getFagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER) && input.getYtelsespesifiktGrunnlag() instanceof ForeldrepengerGrunnlag fg) {
            var måDekningsgradJusteres = VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(mapTilDekningsgradFpsak(fg.getDekningsgrad()), getBarn(ref.behandlingId()));
            if (måDekningsgradJusteres) {
                endreDekningsgradVedDødTjeneste.endreDekningsgradTil100(ref);
            }
        }

        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    private no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad mapTilDekningsgradFpsak(Dekningsgrad dekningsgrad) {
        return switch (dekningsgrad) {
            case DEKNINGSGRAD_80 -> no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad._80;
            case DEKNINGSGRAD_100 -> no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad._100;
            case DEKNINGSGRAD_60, DEKNINGSGRAD_65, DEKNINGSGRAD_70 -> throw new IllegalStateException("Ugyldig dekningsgrad for foreldrepenger: " + dekningsgrad);
        };
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (tilSteg.equals(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG)) {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddForeslåBeregningsgrunnlagVedTilbakeføring();
        }
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
    }

    private List<UidentifisertBarn> getBarn(Long behandlingId) {
        Objects.requireNonNull(familieHendelseRepository, "familieHendelseRepository");
        var familiehendelse = familieHendelseRepository
                .hentAggregatHvisEksisterer(behandlingId)
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon);
        return familiehendelse.orElseThrow().getBarna();
    }
}
