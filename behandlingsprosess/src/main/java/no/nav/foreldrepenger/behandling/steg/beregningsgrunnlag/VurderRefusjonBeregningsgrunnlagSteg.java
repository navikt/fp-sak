package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.iay.AktørArbeidDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.permisjon.PermisjonDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

@BehandlingStegRef(BehandlingStegType.VURDER_REF_BERGRUNN)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderRefusjonBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {
    private static final Logger LOG = LoggerFactory.getLogger(VurderRefusjonBeregningsgrunnlagSteg.class);
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    protected VurderRefusjonBeregningsgrunnlagSteg() {
        // CDI Proxy
    }

    @Inject
    public VurderRefusjonBeregningsgrunnlagSteg(BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                                BehandlingRepository behandlingRepository,
                                                BeregningsgrunnlagInputProvider inputTjenesteProvider,
                                                BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var input = getInputTjeneste(behandling.getFagsakYtelseType()).lagInput(behandlingId);

        // Logging for å utrede hvor vanlig det er med tilkommet permisjon med og uten refusjon
        loggPermisjonssaker(input, behandling.getFagsak().getSaksnummer());

        var beregningsgrunnlagResultat = beregningsgrunnlagKopierOgLagreTjeneste.vurderRefusjonBeregningsgrunnlag(input);
        var aksjonspunkter = beregningsgrunnlagResultat.getAksjonspunkter();
        return BehandleStegResultat
                .utførtMedAksjonspunktResultater(aksjonspunkter.stream().map(BeregningAksjonspunktResultatMapper::map).collect(Collectors.toList()));
    }

    private void loggPermisjonssaker(BeregningsgrunnlagInput input, Saksnummer saksnummer) {
        var stpBGOpt = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(input.getKoblingId()).map(BeregningsgrunnlagEntitet::getSkjæringstidspunkt);
        if (stpBGOpt.isEmpty()) {
            return;
        }
        var stpBG = stpBGOpt.get();
        var yrkesaktiviteter = input.getIayGrunnlag()
            .getAktørArbeidFraRegister()
            .map(AktørArbeidDto::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList());
        yrkesaktiviteter.forEach(ya -> {
            Set<PermisjonDto> permisjoner = ya.getPermisjoner() == null ? Collections.emptySet() : ya.getPermisjoner();
            var finnesTilkommetPermisjon = permisjoner.stream()
                .filter(p -> !p.getPermisjonsbeskrivelseType().equals(PermisjonsbeskrivelseType.PERMISJON_MED_FORELDREPENGER)
                    && !p.getPermisjonsbeskrivelseType().equals(PermisjonsbeskrivelseType.UTDANNINGSPERMISJON))
                .filter(p -> p.getPeriode().getFomDato().isAfter(stpBG))
                .anyMatch(p -> p.getProsentsats().compareTo(BigDecimal.valueOf(100)) >= 0);
            var kreverefusjonFraStart = input.getInntektsmeldinger()
                .stream()
                .filter(ya::gjelderFor)
                .anyMatch(im -> im.getRefusjonBeløpPerMnd() != null && im.getRefusjonBeløpPerMnd().getVerdi().compareTo(BigDecimal.ZERO) > 0);
            if (finnesTilkommetPermisjon && kreverefusjonFraStart) {
                var msg = String.format("FP-564876: Saksnummer %s har tilkommet permisjon og krever refusjon fra start", saksnummer.getVerdi());
                LOG.info(msg);
            }
            else if (finnesTilkommetPermisjon) {
                var msg = String.format("FP-564877: Saksnummer %s har tilkommet permisjon", saksnummer.getVerdi());
                LOG.info(msg);
            }
        });
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (tilSteg.equals(BehandlingStegType.VURDER_REF_BERGRUNN)) {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddVurderRefusjonBeregningsgrunnlagVedTilbakeføring();
        }
    }

}
