package no.nav.foreldrepenger.domene.arbeidInntektsmelding;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValgRepository;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.historikk.ArbeidInntektHistorikkinnslagTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;

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
                LOG.warn(msg);
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

    public void lagreManuelleArbeidsforhold(BehandlingReferanse behandlingReferanse, ManueltArbeidsforholdDto dto) {
        var arbeidsforholdMedMangler = arbeidsforholdInntektsmeldingsMangelUtleder.finnManglerIArbeidsforholdInntektsmeldinger(behandlingReferanse);
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

    public List<ArbeidsforholdMangel> utledManglerPåArbeidsforholdInntektsmelding(BehandlingReferanse behandlingReferanse) {
        return arbeidsforholdInntektsmeldingsMangelUtleder.finnManglerIArbeidsforholdInntektsmeldinger(behandlingReferanse);
    }

    public List<ArbeidsforholdValg> hentArbeidsforholdValgForSak(BehandlingReferanse behandlingReferanse) {
        return arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(behandlingReferanse.behandlingId());
    }

    /**
     * Tjeneste for å rydde vekk valg som ikke er relevant i behandlingen etter kopiering fra forrige behandling.
     * @param ref
     */
    public void ryddVekkUgyldigeValg(BehandlingReferanse ref) {
        var valgPåBehandlingen = arbeidsforholdValgRepository.hentArbeidsforholdValgForBehandling(ref.behandlingId());
        var manglerPåBehandlingen = utledManglerPåArbeidsforholdInntektsmelding(ref);
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
}
