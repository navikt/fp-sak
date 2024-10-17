package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidInntektsmelding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdKomplettVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold.ArbeidsforholdValg;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdInntektsmeldingMangelTjeneste;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.ArbeidsforholdMangel;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.HåndterePermisjoner;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.ArbeidOgInntektsmeldingDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.ArbeidsforholdDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.InntektDto;
import no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto.InntektsmeldingDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.InntektsmeldingKontaktinformasjon;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.KontaktinformasjonIM;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.xml.MottattDokumentXmlParser;

@ApplicationScoped
public class ArbeidOgInntektsmeldingDtoTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    private ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste;
    private BehandlingRepository behandlingRepository;

    ArbeidOgInntektsmeldingDtoTjeneste() {
        // CDI
    }

    @Inject
    public ArbeidOgInntektsmeldingDtoTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                              MottatteDokumentRepository mottatteDokumentRepository,
                                              InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                              DokumentArkivTjeneste dokumentArkivTjeneste,
                                              ArbeidsforholdInntektsmeldingMangelTjeneste arbeidsforholdInntektsmeldingMangelTjeneste,
                                              BehandlingRepository behandlingRepository) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.dokumentArkivTjeneste = dokumentArkivTjeneste;
        this.arbeidsforholdInntektsmeldingMangelTjeneste = arbeidsforholdInntektsmeldingMangelTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public Optional<ArbeidOgInntektsmeldingDto> lagDto(BehandlingReferanse referanse, Skjæringstidspunkt skjæringstidspunkt) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(referanse.behandlingId()).orElse(null);
        if (iayGrunnlag == null) {
            return Optional.empty();
        }
        var mangler = arbeidsforholdInntektsmeldingMangelTjeneste.utledAlleManglerPåArbeidsforholdInntektsmelding(referanse, skjæringstidspunkt);
        var mangelPermisjon = HåndterePermisjoner.finnArbForholdMedPermisjonUtenSluttdatoMangel(referanse, skjæringstidspunkt, iayGrunnlag);
        if (!mangelPermisjon.isEmpty()) {
            mangler.addAll(mangelPermisjon);
        }
        var saksbehandlersVurderinger = arbeidsforholdInntektsmeldingMangelTjeneste.hentArbeidsforholdValgForSak(referanse);
        var inntektsmeldinger = hentInntektsmeldingerForIayGrunnlag(iayGrunnlag, referanse, skjæringstidspunkt, mangler, saksbehandlersVurderinger);
        var arbeidsforhold = mapArbeidsforhold(iayGrunnlag, referanse, skjæringstidspunkt, mangler, saksbehandlersVurderinger);
        var inntekter = mapInntekter(iayGrunnlag, referanse, skjæringstidspunkt);
        return Optional.of(new ArbeidOgInntektsmeldingDto(inntektsmeldinger, arbeidsforhold, inntekter, skjæringstidspunkt.getUtledetSkjæringstidspunkt()));
    }

    public List<InntektsmeldingDto> hentInntektsmeldingerForIayGrunnlag(InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                        BehandlingReferanse referanse,
                                                                        Skjæringstidspunkt stp,
                                                                        List<ArbeidsforholdMangel> mangler,
                                                                        List<ArbeidsforholdValg> saksbehandlersVurderinger) {
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(referanse, stp.getUtledetSkjæringstidspunkt(), iayGrunnlag, true);
        var referanser = iayGrunnlag.getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser)
            .orElse(Collections.emptyList());

        return mapInntektsmeldinger(inntektsmeldinger, referanse, stp, referanser, mangler, saksbehandlersVurderinger);
    }

    public List<InntektsmeldingDto> hentAlleInntektsmeldingerForFagsak(BehandlingReferanse referanse, Skjæringstidspunkt stp) {
        var inntektsmeldinger = inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsak(referanse.saksnummer());

        return mapInntektsmeldinger(inntektsmeldinger, referanse, stp, List.of(), List.of(), List.of());
    }

    private List<InntektDto> mapInntekter(InntektArbeidYtelseGrunnlag iayGrunnlag, BehandlingReferanse referanse, Skjæringstidspunkt stp) {
        var filter = new InntektFilter(iayGrunnlag.getAktørInntektFraRegister(referanse.aktørId()));
        return ArbeidOgInntektsmeldingMapper.mapInntekter(filter, stp.getUtledetSkjæringstidspunkt());
    }

    private List<ArbeidsforholdDto> mapArbeidsforhold(InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                      BehandlingReferanse behandlingReferanse,
                                                      Skjæringstidspunkt stp,
                                                      List<ArbeidsforholdMangel> mangler,
                                                      List<ArbeidsforholdValg> saksbehandlersVurderinger) {
        var filter = new YrkesaktivitetFilter(iayGrunnlag.getAktørArbeidFraRegister(behandlingReferanse.aktørId())
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList()));
        var referanser = iayGrunnlag.getArbeidsforholdInformasjon()
            .map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser)
            .orElse(Collections.emptyList());
        var arbeidsforholdFraRegister = ArbeidOgInntektsmeldingMapper.mapArbeidsforhold(filter, referanser,
            stp.getUtledetSkjæringstidspunkt(), mangler, saksbehandlersVurderinger, iayGrunnlag.getArbeidsforholdOverstyringer());
        var arbeidsforholdFraOverstyringer = ArbeidOgInntektsmeldingMapper.mapManueltOpprettedeArbeidsforhold(
            iayGrunnlag.getArbeidsforholdOverstyringer(), referanser, mangler);

        var alleArbeidsforhold = new ArrayList<>(arbeidsforholdFraRegister);
        alleArbeidsforhold.addAll(arbeidsforholdFraOverstyringer);
        return alleArbeidsforhold;
    }

    private List<InntektsmeldingDto> mapInntektsmeldinger(List<Inntektsmelding> inntektsmeldinger,
                                                          BehandlingReferanse referanse,
                                                          Skjæringstidspunkt stp,
                                                          Collection<ArbeidsforholdReferanse> arbeidsforholdReferanser,
                                                          List<ArbeidsforholdMangel> mangler,
                                                          List<ArbeidsforholdValg> saksbehandlersVurderinger) {
        var motatteDokumenter = mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(referanse.fagsakId());
        var alleInntektsmeldingerFraArkiv = dokumentArkivTjeneste.hentAlleDokumenterCached(referanse.saksnummer())
            .stream()
            .filter(this::gjelderInntektsmelding)
            .toList();

        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(referanse.saksnummer())
            .stream()
            .filter(Behandling::erYtelseBehandling)
            .toList();

        var arbeidsforholdValgListe = arbeidsforholdInntektsmeldingMangelTjeneste.hentArbeidsforholdValgForSak(referanse);

        var mapAvIMTilBehandlingIder = behandlinger.stream()
            .flatMap(behandling -> inntektsmeldingTjeneste.hentInntektsmeldinger( BehandlingReferanse.fra(behandling), stp.getUtledetSkjæringstidspunkt())
                .stream()
                .filter(im -> arbeidsforholdValgListe.stream().noneMatch(arbeidsforholdValg -> {
                    var matchendeArbeidsforhold = arbeidsforholdValg.getArbeidsgiver().getIdentifikator().equals(im.getArbeidsgiver().getIdentifikator());
                    var harValgtFortsetteUtenIM = arbeidsforholdValg.getVurdering() == ArbeidsforholdKomplettVurderingType.IKKE_OPPRETT_BASERT_PÅ_INNTEKTSMELDING;
                    return matchendeArbeidsforhold && harValgtFortsetteUtenIM;
                }))
                .map(im -> Map.entry(im.getJournalpostId(), behandling.getUuid())))
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));


        return inntektsmeldinger.stream().map(im -> {
            var dokumentId = finnDokumentId(im.getJournalpostId(), alleInntektsmeldingerFraArkiv);
            var kontaktinfo = finnMotattXML(motatteDokumenter, im).flatMap(this::trekkUtKontaktInfo);
            var tilknyttedeBehandlingIder = mapAvIMTilBehandlingIder.get(im.getJournalpostId());
            return ArbeidOgInntektsmeldingMapper.mapInntektsmelding(im, arbeidsforholdReferanser, kontaktinfo, dokumentId, mangler,
                saksbehandlersVurderinger, tilknyttedeBehandlingIder);
        }).toList();
    }

    private boolean gjelderInntektsmelding(ArkivJournalPost dok) {
        return dok.getHovedDokument() != null && dok.getHovedDokument().getDokumentType() != null && dok.getHovedDokument()
            .getDokumentType()
            .erInntektsmelding();
    }

    private Optional<String> finnDokumentId(JournalpostId journalpostId, List<ArkivJournalPost> alleInntektsmeldingerFraArkiv) {
        return alleInntektsmeldingerFraArkiv.stream()
            .filter(im -> Objects.equals(im.getJournalpostId(), journalpostId))
            .findFirst()
            .map(d -> d.getHovedDokument().getDokumentId());
    }

    private Optional<KontaktinformasjonIM> trekkUtKontaktInfo(MottattDokument mottattIM) {
        var imWrapper = MottattDokumentXmlParser.unmarshallXml(mottattIM.getPayloadXml());
        if (imWrapper instanceof InntektsmeldingKontaktinformasjon i) {
            return Optional.of(i.finnKontaktinformasjon());
        }
        return Optional.empty();
    }

    private Optional<MottattDokument> finnMotattXML(List<MottattDokument> dokumenter, Inntektsmelding im) {
        return dokumenter.stream().filter(d -> Objects.equals(d.getJournalpostId(), im.getJournalpostId())).findFirst();
    }
}
