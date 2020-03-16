package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import static no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER;
import static no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_SØKER;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.foreldrepenger.domene.arbeidsforhold.ArbeidsforholdWrapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.YtelserKonsolidertTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste.UtledArbeidsforholdParametere;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class InntektArbeidYtelseDtoMapper {

    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;
    private YtelserKonsolidertTjeneste ytelseTjeneste;

    public InntektArbeidYtelseDtoMapper() {
        // for CDI proxy
    }

    @Inject
    public InntektArbeidYtelseDtoMapper(ArbeidsforholdAdministrasjonTjeneste arbeidsforholdAdministrasjonTjeneste,
                                        YtelserKonsolidertTjeneste ytelseTjeneste,
                                        InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                        VirksomhetTjeneste virksomhetTjeneste) {

        this.arbeidsforholdAdministrasjonTjeneste = arbeidsforholdAdministrasjonTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.ytelseTjeneste = ytelseTjeneste;
    }

    public InntektArbeidYtelseDto mapFra(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, SakInntektsmeldinger sakInntektsmeldinger, Optional<AktørId> aktørIdAnnenPart, UtledArbeidsforholdParametere param) {
        InntektArbeidYtelseDto dto = new InntektArbeidYtelseDto();
        mapRelaterteYtelser(dto, ref, iayGrunnlag, aktørIdAnnenPart);

        // TODO (FC) skill denne ut
        if (!ref.getFagsakYtelseType().gjelderEngangsstønad()) {
            mapArbeidsforhold(dto, ref, param, iayGrunnlag, sakInntektsmeldinger);
            dto.setInntektsmeldinger(lagInntektsmeldingDto(ref, iayGrunnlag));
        }
        return dto;
    }

    private void mapArbeidsforhold(InntektArbeidYtelseDto dto, BehandlingReferanse ref, UtledArbeidsforholdParametere param, InntektArbeidYtelseGrunnlag iayGrunnlag, SakInntektsmeldinger sakInntektsmeldinger) {
        Set<ArbeidsforholdWrapper> arbeidsforholdSet = arbeidsforholdAdministrasjonTjeneste.hentArbeidsforholdFerdigUtledet(ref, iayGrunnlag, sakInntektsmeldinger, param);
        dto.setSkalKunneLeggeTilNyeArbeidsforhold(skalKunneLeggeTilNyeArbeidsforhold(arbeidsforholdSet));
        dto.setSkalKunneLageArbeidsforholdBasrtPåInntektsmelding(skalKunneLageArbeidsforholdFraInntektsmelding(arbeidsforholdSet));
        dto.setArbeidsforhold(arbeidsforholdSet.stream().map(this::mapArbeidsforhold).collect(Collectors.toList()));
    }

    private boolean skalKunneLageArbeidsforholdFraInntektsmelding(Set<ArbeidsforholdWrapper> arbeidsforholdSet) {
        return !arbeidsforholdSet.isEmpty() && arbeidsforholdSet.stream().anyMatch(a -> a.getKilde().equals(ArbeidsforholdKilde.INNTEKTSMELDING));
    }

    private ArbeidsforholdDto mapArbeidsforhold(ArbeidsforholdWrapper wrapper) {
        ArbeidsforholdDto arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setId(lagId(wrapper));
        arbeidsforholdDto.setFomDato(wrapper.getFomDato());
        arbeidsforholdDto.setTomDato(wrapper.getTomDato() != null && wrapper.getTomDato().equals(Tid.TIDENES_ENDE) ? null : wrapper.getTomDato());
        arbeidsforholdDto.setNavn(wrapper.getNavn());
        mapArbeidsgiverIdentifikator(wrapper, arbeidsforholdDto);
        arbeidsforholdDto.setBrukArbeidsforholdet(wrapper.getBrukArbeidsforholdet());
        if (wrapper.getBrukArbeidsforholdet() != null && wrapper.getBrukArbeidsforholdet()) {
            arbeidsforholdDto.setErNyttArbeidsforhold(wrapper.getErNyttArbeidsforhold());
            arbeidsforholdDto.setFortsettBehandlingUtenInntektsmelding(wrapper.getFortsettBehandlingUtenInntektsmelding());
        }
        arbeidsforholdDto.setArbeidsforholdId(wrapper.getArbeidsforholdId());
        arbeidsforholdDto.setEksternArbeidsforholdId(wrapper.getEksternArbeidsforholdId());
        arbeidsforholdDto.setIkkeRegistrertIAaRegister(wrapper.getIkkeRegistrertIAaRegister());
        arbeidsforholdDto.setHarErstattetEttEllerFlere(wrapper.getHarErsattetEttEllerFlere());
        arbeidsforholdDto.setErstatterArbeidsforholdId(wrapper.getErstatterArbeidsforhold());
        arbeidsforholdDto.setKilde(wrapper.getKilde());
        arbeidsforholdDto.setMottattDatoInntektsmelding(wrapper.getMottattDatoInntektsmelding());
        arbeidsforholdDto.setTilVurdering(wrapper.isHarAksjonspunkt());
        arbeidsforholdDto.setBegrunnelse(wrapper.getBegrunnelse());
        arbeidsforholdDto.setVurderOmSkalErstattes(wrapper.getVurderOmSkalErstattes());
        arbeidsforholdDto.setStillingsprosent(wrapper.getStillingsprosent());
        arbeidsforholdDto.setErSlettet(wrapper.getErSlettet());
        arbeidsforholdDto.setErEndret(wrapper.getErEndret());
        arbeidsforholdDto.setHandlingType(wrapper.getHandlingType());
        arbeidsforholdDto.setBrukMedJustertPeriode(wrapper.getBrukMedJustertPeriode());
        arbeidsforholdDto.setLagtTilAvSaksbehandler(wrapper.getLagtTilAvSaksbehandler());
        arbeidsforholdDto.setBasertPaInntektsmelding(wrapper.getBasertPåInntektsmelding());
        arbeidsforholdDto.setInntektMedTilBeregningsgrunnlag(wrapper.getInntektMedTilBeregningsgrunnlag());
        arbeidsforholdDto.setSkjaeringstidspunkt(wrapper.getSkjaeringstidspunkt());
        arbeidsforholdDto.setBrukPermisjon(wrapper.getBrukPermisjon());
        arbeidsforholdDto.setPermisjoner(wrapper.getPermisjoner());
        arbeidsforholdDto.setOverstyrtTom(Tid.TIDENES_ENDE.equals(wrapper.getOverstyrtTom()) ? null : wrapper.getOverstyrtTom());
        arbeidsforholdDto.setKanOppretteNyttArbforFraIM(wrapper.getKanOppretteNyttArbforFraIM());
        return arbeidsforholdDto;
    }

    private boolean skalKunneLeggeTilNyeArbeidsforhold(Set<ArbeidsforholdWrapper> arbeidsforholdSet) {
        if (arbeidsforholdSet.isEmpty()) {
            return true;
        }
        return arbeidsforholdSet.stream().anyMatch(wrapper -> Objects.equals(wrapper.getHandlingType(), ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER));
    }

    private void mapArbeidsgiverIdentifikator(ArbeidsforholdWrapper wrapper, ArbeidsforholdDto arbeidsforholdDto) {
        arbeidsforholdDto.setArbeidsgiverIdentifikator(wrapper.getArbeidsgiverIdentifikator());
        if (gjelderVirksomhet(wrapper)) {
            arbeidsforholdDto.setArbeidsgiverIdentifiktorGUI(wrapper.getArbeidsgiverIdentifikator());
        } else {
            arbeidsforholdDto.setArbeidsgiverIdentifiktorGUI(wrapper.getPersonArbeidsgiverIdentifikator());
        }
    }

    private boolean gjelderVirksomhet(ArbeidsforholdWrapper wrapper) {
        return wrapper.getPersonArbeidsgiverIdentifikator() == null;
    }

    private String lagId(ArbeidsforholdWrapper wrapper) {
        return wrapper.getArbeidsgiverIdentifikator() + "-" + wrapper.getArbeidsforholdId();
    }

    private List<InntektsmeldingDto> lagInntektsmeldingDto(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        LocalDate dato = ref.getUtledetSkjæringstidspunkt();
        List<Inntektsmelding> inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(ref.getAktørId(), dato, iayGrunnlag);
        return inntektsmeldinger.stream()
            .map(inntektsmelding -> {
                Optional<Virksomhet> virksomhet = virksomhetTjeneste.hentVirksomhet(inntektsmelding.getArbeidsgiver().getOrgnr());
                return new InntektsmeldingDto(inntektsmelding, virksomhet);
            })
            .collect(Collectors.toList());
    }

    private void mapRelaterteYtelser(InntektArbeidYtelseDto dto, BehandlingReferanse ref, InntektArbeidYtelseGrunnlag grunnlag, Optional<AktørId> aktørIdAnnenPart) {
        dto.setRelatertTilgrensendeYtelserForSoker(mapTilDtoSøker(hentRelaterteYtelser(grunnlag, ref.getAktørId())));
        aktørIdAnnenPart.ifPresent(annenPartAktørId -> {
            List<TilgrensendeYtelserDto> hentRelaterteYtelser = hentRelaterteYtelser(grunnlag, annenPartAktørId);
            List<RelaterteYtelserDto> relaterteYtelser = mapTilDtoAnnenPart(hentRelaterteYtelser);
            dto.setRelatertTilgrensendeYtelserForAnnenForelder(relaterteYtelser);
            // TODO Termitt. Trengs denne måten å skille på? For å evt. få til dette må det filtreres her etter at det hentes.(bare innvilget)
            dto.setInnvilgetRelatertTilgrensendeYtelserForAnnenForelder(relaterteYtelser);
        });

    }

    private List<RelaterteYtelserDto> mapTilDtoAnnenPart(List<TilgrensendeYtelserDto> tilgrensendeYtelserDtos) {
        return BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(tilgrensendeYtelserDtos, RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER);
    }

    private List<RelaterteYtelserDto> mapTilDtoSøker(List<TilgrensendeYtelserDto> tilgrensendeYtelserDtos) {
        return BehandlingRelaterteYtelserMapper.samleYtelserBasertPåYtelseType(tilgrensendeYtelserDtos, RELATERT_YTELSE_TYPER_FOR_SØKER);
    }

    private List<TilgrensendeYtelserDto> hentRelaterteYtelser(InntektArbeidYtelseGrunnlag grunnlag, AktørId aktørId) {
        final List<TilgrensendeYtelserDto> relatertYtelser = new ArrayList<>();
        // Relaterte yteleser fra InntektArbeidYtelseAggregatet
        relatertYtelser.addAll(mapYtelseForAktørTilTilgrensedeYtelser(grunnlag, aktørId));

        return relatertYtelser;
    }

    private List<TilgrensendeYtelserDto> mapYtelseForAktørTilTilgrensedeYtelser(InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag, AktørId aktørId) {
        return ytelseTjeneste.utledYtelserRelatertTilBehandling(aktørId, inntektArbeidYtelseGrunnlag, Optional.empty());
    }

}
