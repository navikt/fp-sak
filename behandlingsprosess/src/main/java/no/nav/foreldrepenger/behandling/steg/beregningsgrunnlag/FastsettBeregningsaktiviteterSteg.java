package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.folketrygdloven.kalkulator.output.BeregningAvklaringsbehovResultat;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.fp.SykemeldingVentTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING)
@BehandlingTypeRef
@ApplicationScoped
public class FastsettBeregningsaktiviteterSteg implements BeregningsgrunnlagSteg {
    private static final Logger LOG = LoggerFactory.getLogger(FastsettBeregningsaktiviteterSteg.class);

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;
    private SykemeldingVentTjeneste sykemeldingVentTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    protected FastsettBeregningsaktiviteterSteg() {
        // for CDI proxy
    }

    @Inject
    public FastsettBeregningsaktiviteterSteg(BehandlingRepository behandlingRepository,
                                             BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                             BeregningsgrunnlagInputProvider inputTjenesteProvider,
                                             SykemeldingVentTjeneste sykemeldingVentTjeneste,
                                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.beregningsgrunnlagInputTjeneste = Objects.requireNonNull(inputTjenesteProvider, "inputTjenestene");
        this.sykemeldingVentTjeneste = sykemeldingVentTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var input = getInputTjeneste(behandling.getFagsakYtelseType()).lagInput(behandling);
        var ugyldigAndel = UgyldigGraderingUtleder.finnFørsteUgyldigeAndel(input);
        if (ugyldigAndel.isPresent()) {
            var saksnummer = BehandlingReferanse.fra(behandling).saksnummer();
            var feilmelding = String.format("Mulig gradering uten arbeidsforhold på saksnummer %s . Arbeidsgiver: %s Internref: %s", saksnummer.getVerdi(),
                OrgNummer.tilMaskertNummer(ugyldigAndel.get().arbeidsgiverIdent()), ugyldigAndel.get().internRef());
            LOG.warn(feilmelding);
        }

        var aksjonspunktResultater = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);
        var ventPåSykemeldingAksjonspunkt = skalVentePåSykemelding(behandling);
        if (aksjonspunktResultater == null) {
            return BehandleStegResultat.fremoverført(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
        }
        // Hvis det ikke allerede er utledet ventepunkt og vi har ventepunkt for sykemelding
        if (aksjonspunktResultater.stream().noneMatch(BeregningAvklaringsbehovResultat::harFrist) && ventPåSykemeldingAksjonspunkt.isPresent()) {
            return BehandleStegResultat.utførtMedAksjonspunktResultat(ventPåSykemeldingAksjonspunkt.get());
        }
        // hent på nytt i tilfelle lagret og flushet
        return BehandleStegResultat
                .utførtMedAksjonspunktResultater(aksjonspunktResultater.stream().map(BeregningAksjonspunktResultatMapper::map).collect(Collectors.toList()));
    }

    private Optional<AksjonspunktResultat> skalVentePåSykemelding(Behandling behandling) {
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var referanse = BehandlingReferanse.fra(behandling, stp);
        var ventefrist = sykemeldingVentTjeneste.skalVentePåSykemelding(referanse);
        if (ventefrist.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(AksjonspunktResultat.opprettForAksjonspunktMedFrist(
            AksjonspunktDefinisjon.AUTO_VENT_PÅ_SYKEMELDING,
            Venteårsak.VENT_MANGLENDE_SYKEMELDING,
            LocalDateTime.of(ventefrist.get(), LocalDateTime.now().toLocalTime())));
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING.equals(tilSteg)) {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).gjenopprettFastsattBeregningAktivitetBeregningsgrunnlag();
        } else {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddFastsettSkjæringstidspunktVedTilbakeføring();
        }
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputTjeneste.getTjeneste(ytelseType);
    }
}
