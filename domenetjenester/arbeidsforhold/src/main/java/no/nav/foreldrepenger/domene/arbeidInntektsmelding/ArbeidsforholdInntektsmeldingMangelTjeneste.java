package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValgRepository;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk.ArbeidInntektHistorikkinnslagTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class ArbeidsforholdInntektsmeldingMangelTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsforholdInntektsmeldingMangelTjeneste.class);

    private ArbeidsforholdValgRepository arbeidsforholdValgRepository;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsforholdInntektsmeldingsMangelUtleder arbeidsforholdInntektsmeldingsMangelUtleder;
    private ArbeidInntektHistorikkinnslagTjeneste arbeidInntektHistorikkinnslagTjeneste;
    private InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;


    public ArbeidsforholdInntektsmeldingMangelTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdInntektsmeldingMangelTjeneste(ArbeidsforholdValgRepository arbeidsforholdValgRepository,
                                                       ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste,
                                                       InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                       ArbeidsforholdInntektsmeldingsMangelUtleder arbeidsforholdInntektsmeldingsMangelUtleder,
                                                       ArbeidInntektHistorikkinnslagTjeneste arbeidInntektHistorikkinnslagTjeneste,
                                                       InntektsmeldingRegisterTjeneste inntektsmeldingRegisterTjeneste,
                                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.arbeidsforholdValgRepository = arbeidsforholdValgRepository;
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.arbeidsforholdInntektsmeldingsMangelUtleder = arbeidsforholdInntektsmeldingsMangelUtleder;
        this.arbeidInntektHistorikkinnslagTjeneste = arbeidInntektHistorikkinnslagTjeneste;
        this.inntektsmeldingRegisterTjeneste = inntektsmeldingRegisterTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public void lagreManglendeOpplysningerVurdering(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt, ManglendeOpplysningerVurderingDto dto) {

        var arbeidsforholdMedMangler = arbeidsforholdInntektsmeldingsMangelUtleder.finnAlleManglerIArbeidsforholdInntektsmeldinger(behandlingReferanse, skjæringstidspunkt);
        var entiteter = ArbeidsforholdInntektsmeldingMangelMapper.mapManglendeOpplysningerVurdering(dto, arbeidsforholdMedMangler);
        sjekkUnikeReferanser(entiteter); // Skal kun være en avklaring pr referanse
        entiteter.forEach(ent -> arbeidsforholdValgRepository.lagre(ent, behandlingReferanse.behandlingId()));
        var iaygrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.behandlingId());
        arbeidInntektHistorikkinnslagTjeneste.opprettHistorikkinnslag(behandlingReferanse, dto, iaygrunnlag);

        // Kall til abakus, gjøres til slutt
        ryddBortManuelleArbeidsforholdVedBehov(behandlingReferanse, dto);
    }

    private void sjekkUnikeReferanser(List<ArbeidsforholdValg> entiteter) {
        var ag = entiteter.get(0).getArbeidsgiver();
        Map<InternArbeidsforholdRef, List<ArbeidsforholdValg>> map = entiteter.stream()
            .collect(Collectors.groupingBy(ArbeidsforholdValg::getArbeidsforholdRef));
        map.forEach((key, value) -> {
            if (value.size() != 1) {
                var msg = String.format("Mer enn 1 avklaring for arbeidsforhold hos arbeidsgiver %s med arbeidsforholdref %s", ag, key);
                throw new IllegalStateException(msg);
            }
        });
    }

    private void ryddBortManuelleArbeidsforholdVedBehov(BehandlingReferanse behandlingReferanse, ManglendeOpplysningerVurderingDto dto) {
        var eksisterendeInfo = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.behandlingUuid()).getArbeidsforholdInformasjon();
        if (eksisterendeInfo.map(info -> ArbeidsforholdInntektsmeldingRyddeTjeneste.arbeidsforholdSomMåRyddesBortVedNyttValg(dto, info)).orElse(false)) {
            var informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandlingReferanse.behandlingId());
            informasjonBuilder.fjernOverstyringerSomGjelder(ArbeidsforholdInntektsmeldingMangelMapper.lagArbeidsgiver(dto.getArbeidsgiverIdent()));
            arbeidsforholdTjeneste.lagreOverstyring(behandlingReferanse.behandlingId(), informasjonBuilder);
        }
    }

    public void lagreManuelleArbeidsforhold(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt stp, ManueltArbeidsforholdDto dto) {
        var arbeidsforholdMedMangler = arbeidsforholdInntektsmeldingsMangelUtleder.finnAlleManglerIArbeidsforholdInntektsmeldinger(behandlingReferanse, stp);
        var eksisterendeValg = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandlingReferanse.behandlingId());
        var valgSomMåRyddesBort = ArbeidsforholdInntektsmeldingRyddeTjeneste.valgSomMåRyddesBortVedOpprettelseAvArbeidsforhold(dto, eksisterendeValg);

        valgSomMåRyddesBort.ifPresent(arbeidsforholdValg -> arbeidsforholdValgRepository.fjernValg(arbeidsforholdValg));

        var informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandlingReferanse.behandlingId());
        var oppdatertBuilder = ArbeidsforholdInntektsmeldingMangelMapper.mapManueltArbeidsforhold(dto, arbeidsforholdMedMangler, informasjonBuilder);

        var iaygrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.behandlingId());
        arbeidInntektHistorikkinnslagTjeneste.opprettHistorikkinnslag(behandlingReferanse, dto, iaygrunnlag);

        // Kall til abakus, gjøres til slutt
        arbeidsforholdTjeneste.lagreOverstyring(behandlingReferanse.behandlingId(), oppdatertBuilder);
    }

    public List<ArbeidsforholdMangel> utledAlleManglerPåArbeidsforholdInntektsmelding(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt) {
        return arbeidsforholdInntektsmeldingsMangelUtleder.finnAlleManglerIArbeidsforholdInntektsmeldinger(behandlingReferanse, skjæringstidspunkt);
    }

    public List<ArbeidsforholdMangel> utledUavklarteManglerPåArbeidsforholdInntektsmelding(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt skjæringstidspunkt) {
        var alleMangler = arbeidsforholdInntektsmeldingsMangelUtleder.finnAlleManglerIArbeidsforholdInntektsmeldinger(behandlingReferanse, skjæringstidspunkt);
        var alleAvklaringer = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandlingReferanse.behandlingId());
        var uavklarteMangler = alleMangler.stream().filter(mangel -> !finnesAvklaringSomGjelderMangel(mangel, alleAvklaringer)).toList();
        return uavklarteMangler;
    }

    private boolean finnesAvklaringSomGjelderMangel(ArbeidsforholdMangel mangel, List<ArbeidsforholdValg> alleAvklaringer) {
        return alleAvklaringer.stream()
            .filter(ak -> ArbeidsforholdKomplettVurderingType.FORTSETT_UTEN_INNTEKTSMELDING.equals(ak.getVurdering()))
            .anyMatch(ak -> ak.getArbeidsgiver().equals(mangel.arbeidsgiver()) && ak.getArbeidsforholdRef().gjelderFor(mangel.ref()));
    }

    public List<ArbeidsforholdValg> hentArbeidsforholdValgForSak(BehandlingReferanse behandlingReferanse) {
        return arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandlingReferanse.behandlingId());
    }

    /**
     * Tjeneste for å rydde vekk valg som ikke er relevant i behandlingen etter kopiering fra forrige behandling.
     * @param ref
     */
    public void ryddVekkUgyldigeValg(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var valgPåBehandlingen = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(ref.behandlingId());
        var manglerPåBehandlingen = utledAlleManglerPåArbeidsforholdInntektsmelding(ref, stp);
        var valgSomMåDeaktiveres = ArbeidsforholdInntektsmeldingRyddeTjeneste.finnUgyldigeValgSomErGjort(valgPåBehandlingen, manglerPåBehandlingen);
        valgSomMåDeaktiveres.forEach(valg -> {
            LOG.info("Deaktiverer valg som ikke lenger er gyldig: {}", valg);
            arbeidsforholdValgRepository.fjernValg(valg);
        });
    }

    /**
     * Tjeneste for å rydde vekk alle valg som er gjort i behandlingen. Skal kun brukes av forvaltningstjenester.
     * @param ref
     */
    public void ryddVekkAlleValgPåBehandling(BehandlingReferanse ref) {
        var valgPåBehandlingen = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(ref.behandlingId());
        valgPåBehandlingen.forEach(valg -> {
            LOG.info("Deaktiverer valg: {}", valg);
            arbeidsforholdValgRepository.fjernValg(valg);
        });
    }

    /**
     * Tjeneste for å rydde vekk overstyringer som ikke lenger er mulig å gjøre (overgang fra gammelt aksjonspunkt 5080)
     * @param ref
     */
    public void ryddVekkUgyldigeArbeidsforholdoverstyringer(BehandlingReferanse ref) {
        var overstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.behandlingId()).getArbeidsforholdOverstyringer();
        var overstyringerSomMåFjernes = ArbeidsforholdInntektsmeldingRyddeTjeneste.finnUgyldigeOverstyringer(overstyringer);
        if (!overstyringerSomMåFjernes.isEmpty()) {
            var informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(ref.behandlingId());
            overstyringerSomMåFjernes.forEach(os -> {
                LOG.info("Fjerner overstyring som ikke lenger er gyldig: {}", os);
                informasjonBuilder.fjernOverstyringerSomGjelder(os.getArbeidsgiver());
            });
            arbeidsforholdTjeneste.lagreOverstyring(ref.behandlingId(), informasjonBuilder);
        }
    }

    /**
     * finnManglendeInntektsmeldingerHensyntattVurdering
     * For å finne alle arbeidsforhold saksbehandler krever inntektsmelding for, og om det er mottatt inntektsmelding eller ikke.
     * Filtrerer vekk arbeidsforhold der saksbehandler har avklart at inntektsmelding ikke trengs.
     * @param referanse
     * @return
     */
    public List<ArbeidsforholdInntektsmeldingStatus> finnStatusForInntektsmeldingArbeidsforhold(BehandlingReferanse referanse) {
        try {
            var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(referanse.behandlingId());
            return finnStatusForInntektsmeldingArbeidsforhold(referanse, skjæringstidspunkt);
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<ArbeidsforholdInntektsmeldingStatus> finnStatusForInntektsmeldingArbeidsforhold(BehandlingReferanse referanse, Skjæringstidspunkt skjæringstidspunkt) {
        var manglendeInntektsmeldinger = inntektsmeldingRegisterTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(referanse, skjæringstidspunkt,false);
        var allePåkrevdeInntektsmeldinger = inntektsmeldingRegisterTjeneste.hentAllePåkrevdeInntektsmeldinger(referanse, skjæringstidspunkt);
        var saksbehandlersValg = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(referanse.behandlingId());
        return InntektsmeldingStatusMapper.mapInntektsmeldingStatus(allePåkrevdeInntektsmeldinger, manglendeInntektsmeldinger, saksbehandlersValg);
    }

}
