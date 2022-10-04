package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.EndreDekningsgradVedDødTjeneste;
import no.nav.foreldrepenger.behandling.VurderDekningsgradVedDødsfallAksjonspunktUtleder;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.konfig.Environment;

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
                .collect(Collectors.toList());

        var kanEndreDekningsgradAutomatisk = !Environment.current().isProd();

        if (behandling.getFagsakYtelseType().equals(FagsakYtelseType.FORELDREPENGER) && !kanEndreDekningsgradAutomatisk) {
            boolean skalHaAksjonspunktForVurderDekningsgrad = VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(input.getYtelsespesifiktGrunnlag().getDekningsgrad(),
                getBarn(ref.behandlingId()));
            if (skalHaAksjonspunktForVurderDekningsgrad) {
                aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_DEKNINGSGRAD));
            }
        }

        if (kanEndreDekningsgradAutomatisk) {
            var måDekningsgradJusteres = VurderDekningsgradVedDødsfallAksjonspunktUtleder.utled(input.getYtelsespesifiktGrunnlag().getDekningsgrad(),
                getBarn(ref.behandlingId()));
            if (måDekningsgradJusteres) {
                endreDekningsgradVedDødTjeneste.endreDekningsgradTil100(ref);
            }
        }

        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
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
