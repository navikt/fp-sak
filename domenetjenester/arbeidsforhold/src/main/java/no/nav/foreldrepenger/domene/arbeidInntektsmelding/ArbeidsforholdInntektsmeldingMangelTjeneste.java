package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValgRepository;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk.ArbeidInntektHistorikkinnslagTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;

@ApplicationScoped
public class ArbeidsforholdInntektsmeldingMangelTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsforholdInntektsmeldingMangelTjeneste.class);

    private ArbeidsforholdValgRepository arbeidsforholdValgRepository;
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ArbeidsforholdInntektsmeldingsMangelUtleder arbeidsforholdInntektsmeldingsMangelUtleder;
    private ArbeidInntektHistorikkinnslagTjeneste arbeidInntektHistorikkinnslagTjeneste;


    public ArbeidsforholdInntektsmeldingMangelTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidsforholdInntektsmeldingMangelTjeneste(ArbeidsforholdValgRepository arbeidsforholdValgRepository,
                                                       ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste,
                                                       InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                       ArbeidsforholdInntektsmeldingsMangelUtleder arbeidsforholdInntektsmeldingsMangelUtleder,
                                                       ArbeidInntektHistorikkinnslagTjeneste arbeidInntektHistorikkinnslagTjeneste) {
        this.arbeidsforholdValgRepository = arbeidsforholdValgRepository;
        this.arbeidsforholdTjeneste = arbeidsforholdTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.arbeidsforholdInntektsmeldingsMangelUtleder = arbeidsforholdInntektsmeldingsMangelUtleder;
        this.arbeidInntektHistorikkinnslagTjeneste = arbeidInntektHistorikkinnslagTjeneste;
    }

    public void lagreManglendeOpplysningerVurdering(BehandlingReferanse behandlingReferanse, ManglendeOpplysningerVurderingDto dto) {

        var arbeidsforholdMedMangler = arbeidsforholdInntektsmeldingsMangelUtleder.finnManglerIArbeidsforholdInntektsmeldinger(behandlingReferanse);
        var entitet = ArbeidsforholdInntektsmeldingMangelMapper.mapManglendeOpplysningerVurdering(dto, arbeidsforholdMedMangler);
        entitet.forEach(ent -> arbeidsforholdValgRepository.lagre(ent, behandlingReferanse.getBehandlingId()));
        var iaygrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId());
        arbeidInntektHistorikkinnslagTjeneste.opprettHistorikkinnslag(behandlingReferanse, dto, iaygrunnlag);

        // Kall til abakus, gjøres til slutt
        ryddBortManuelleArbeidsforholdVedBehov(behandlingReferanse, dto);
    }

    private void ryddBortManuelleArbeidsforholdVedBehov(BehandlingReferanse behandlingReferanse, ManglendeOpplysningerVurderingDto dto) {
        var eksisterendeInfo = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingUuid()).getArbeidsforholdInformasjon();
        if (eksisterendeInfo.map(info -> ArbeidsforholdInntektsmeldingRyddeTjeneste.arbeidsforholdSomMåRyddesBortVedNyttValg(dto, info)).orElse(false)) {
            var informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandlingReferanse.getBehandlingId());
            informasjonBuilder.fjernOverstyringerSomGjelder(ArbeidsforholdInntektsmeldingMangelMapper.lagArbeidsgiver(dto.getArbeidsgiverIdent()));
            arbeidsforholdTjeneste.lagreOverstyring(behandlingReferanse.getBehandlingId(), behandlingReferanse.getAktørId(), informasjonBuilder);
        }
    }

    public void lagreManuelleArbeidsforhold(BehandlingReferanse behandlingReferanse, ManueltArbeidsforholdDto dto) {
        var arbeidsforholdMedMangler = arbeidsforholdInntektsmeldingsMangelUtleder.finnManglerIArbeidsforholdInntektsmeldinger(behandlingReferanse);
        var eksisterendeValg = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandlingReferanse.getBehandlingId());
        var valgSomMåRyddesBort = ArbeidsforholdInntektsmeldingRyddeTjeneste.valgSomMåRyddesBortVedOpprettelseAvArbeidsforhold(dto, eksisterendeValg);

        valgSomMåRyddesBort.ifPresent(arbeidsforholdValg -> arbeidsforholdValgRepository.fjernValg(arbeidsforholdValg));

        var informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandlingReferanse.getBehandlingId());
        ArbeidsforholdInformasjonBuilder oppdatertBuilder = ArbeidsforholdInntektsmeldingMangelMapper.mapManueltArbeidsforhold(dto, arbeidsforholdMedMangler, informasjonBuilder);

        var iaygrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId());
        arbeidInntektHistorikkinnslagTjeneste.opprettHistorikkinnslag(behandlingReferanse, dto, iaygrunnlag);

        // Kall til kalkulus, gjøres til slutt
        arbeidsforholdTjeneste.lagreOverstyring(behandlingReferanse.getBehandlingId(), behandlingReferanse.getAktørId(), oppdatertBuilder);
    }

    public List<ArbeidsforholdMangel> utledManglerPåArbeidsforholdInntektsmelding(BehandlingReferanse behandlingReferanse) {
        return arbeidsforholdInntektsmeldingsMangelUtleder.finnManglerIArbeidsforholdInntektsmeldinger(behandlingReferanse);
    }

    public List<ArbeidsforholdValg> hentArbeidsforholdValgForSak(BehandlingReferanse behandlingReferanse) {
        return arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandlingReferanse.getBehandlingId());
    }

    /**
     * Tjeneste for å rydde vekk valg som ikke er relevant i behandlingen etter kopiering fra forrige behandling.
     * @param ref
     */
    public void ryddVekkUgyldigeValg(BehandlingReferanse ref) {
        var valgPåBehandlingen = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(ref.getBehandlingId());
        var manglerPåBehandlingen = utledManglerPåArbeidsforholdInntektsmelding(ref);
        var valgSomMåDeaktiveres = ArbeidsforholdInntektsmeldingRyddeTjeneste.finnUgyldigeValgSomErGjort(valgPåBehandlingen, manglerPåBehandlingen);
        valgSomMåDeaktiveres.forEach(valg -> {
            LOG.info("Deaktiverer valg som ikke lenger er gyldig: {}", valg);
            arbeidsforholdValgRepository.fjernValg(valg);
        });
    }

    /**
     * Tjeneste for å rydde vekk overstyringer som ikke lenger er mulig å gjøre (overgang fra gammelt aksjonspunkt 5080)
     * @param ref
     */
    public void ryddVekkUgyldigeArbeidsforholdoverstyringer(BehandlingReferanse ref) {
        var overstyringer = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId()).getArbeidsforholdOverstyringer();
        var overstyringerSomMåFjernes = ArbeidsforholdInntektsmeldingRyddeTjeneste.finnUgyldigeOverstyringer(overstyringer);
        if (!overstyringerSomMåFjernes.isEmpty()) {
            var informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(ref.getBehandlingId());
            overstyringerSomMåFjernes.forEach(os -> {
                LOG.info("Fjerner overstyring som ikke lenger er gyldig: {}", os);
                informasjonBuilder.fjernOverstyringerSomGjelder(os.getArbeidsgiver());
            });
            arbeidsforholdTjeneste.lagreOverstyring(ref.getBehandlingId(), ref.getAktørId(), informasjonBuilder);
        }
    }
}
