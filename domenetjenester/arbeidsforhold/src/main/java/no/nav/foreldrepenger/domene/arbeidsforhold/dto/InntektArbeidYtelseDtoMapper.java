package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import static no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER;
import static no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_SØKER;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.foreldrepenger.domene.arbeidsforhold.ArbeidsforholdWrapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.YtelserKonsolidertTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste.UtledArbeidsforholdParametere;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
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

    public InntektArbeidYtelseDto mapFra(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, SakInntektsmeldinger sakInntektsmeldinger,
            Optional<AktørId> aktørIdAnnenPart, UtledArbeidsforholdParametere param) {
        var dto = new InntektArbeidYtelseDto();
        mapRelaterteYtelser(dto, ref, iayGrunnlag, aktørIdAnnenPart);

        // TODO (FC) skill denne ut
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {
            mapArbeidsforhold(dto, ref, param, iayGrunnlag, sakInntektsmeldinger);
            dto.setInntektsmeldinger(lagInntektsmeldingDto(ref, iayGrunnlag));
        }
        return dto;
    }

    public InntektsmeldingerDto mapInntektsmeldinger(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {

            return new InntektsmeldingerDto(lagInntektsmeldingDto(ref, iayGrunnlag));
        } else {
            return new InntektsmeldingerDto(List.of());
        }
    }

    private void mapArbeidsforhold(InntektArbeidYtelseDto dto, BehandlingReferanse ref, UtledArbeidsforholdParametere param,
            InntektArbeidYtelseGrunnlag iayGrunnlag, SakInntektsmeldinger sakInntektsmeldinger) {
        var arbeidsforholdSet = arbeidsforholdAdministrasjonTjeneste.hentArbeidsforholdFerdigUtledet(ref, iayGrunnlag,
                sakInntektsmeldinger, param);
        dto.setSkalKunneLeggeTilNyeArbeidsforhold(skalKunneLeggeTilNyeArbeidsforhold(arbeidsforholdSet));
        dto.setSkalKunneLageArbeidsforholdBasrtPåInntektsmelding(skalKunneLageArbeidsforholdFraInntektsmelding(arbeidsforholdSet));
        dto.setArbeidsforhold(arbeidsforholdSet.stream().map(this::mapArbeidsforhold).collect(Collectors.toList()));
    }

    private boolean skalKunneLageArbeidsforholdFraInntektsmelding(Set<ArbeidsforholdWrapper> arbeidsforholdSet) {
        return !arbeidsforholdSet.isEmpty() && arbeidsforholdSet.stream().anyMatch(a -> a.getKilde().equals(ArbeidsforholdKilde.INNTEKTSMELDING));
    }

    private ArbeidsforholdDto mapArbeidsforhold(ArbeidsforholdWrapper wrapper) {
        var arbeidsforholdDto = new ArbeidsforholdDto();
        arbeidsforholdDto.setId(lagId(wrapper));
        arbeidsforholdDto.setFomDato(wrapper.getFomDato());
        arbeidsforholdDto.setTomDato((wrapper.getTomDato() != null) && wrapper.getTomDato().equals(Tid.TIDENES_ENDE) ? null : wrapper.getTomDato());
        mapArbeidsgiverIdentifikator(wrapper, arbeidsforholdDto);
        arbeidsforholdDto.setBrukArbeidsforholdet(wrapper.getBrukArbeidsforholdet());
        if ((wrapper.getBrukArbeidsforholdet() != null) && wrapper.getBrukArbeidsforholdet()) {
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
        return arbeidsforholdSet.stream()
                .anyMatch(wrapper -> Objects.equals(wrapper.getHandlingType(), ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER));
    }

    private void mapArbeidsgiverIdentifikator(ArbeidsforholdWrapper wrapper, ArbeidsforholdDto arbeidsforholdDto) {
        arbeidsforholdDto.setArbeidsgiverReferanse(wrapper.getArbeidsgiverReferanse());
    }

    private String lagId(ArbeidsforholdWrapper wrapper) {
        return wrapper.getArbeidsgiverReferanse() + "-" + wrapper.getArbeidsforholdId();
    }

    private List<InntektsmeldingDto> lagInntektsmeldingDto(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var dato = ref.getUtledetSkjæringstidspunkt();
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, dato, iayGrunnlag,
            ref.getSkjæringstidspunkt().getFørsteUttaksdatoSøknad().isPresent());
        return inntektsmeldinger.stream()
                .map(inntektsmelding -> {
                    var virksomhet = virksomhetTjeneste.finnOrganisasjon(inntektsmelding.getArbeidsgiver().getOrgnr());
                    return new InntektsmeldingDto(inntektsmelding, virksomhet);
                })
                .collect(Collectors.toList());
    }

    private void mapRelaterteYtelser(InntektArbeidYtelseDto dto, BehandlingReferanse ref, InntektArbeidYtelseGrunnlag grunnlag,
            Optional<AktørId> aktørIdAnnenPart) {
        dto.setRelatertTilgrensendeYtelserForSoker(mapTilDtoSøker(hentRelaterteYtelser(grunnlag, ref.aktørId())));
        aktørIdAnnenPart.ifPresent(annenPartAktørId -> {
            var hentRelaterteYtelser = hentRelaterteYtelserAnnenPart(grunnlag, annenPartAktørId);
            var relaterteYtelser = mapTilDtoAnnenPart(hentRelaterteYtelser);
            dto.setRelatertTilgrensendeYtelserForAnnenForelder(relaterteYtelser);
            // TODO Termitt. Trengs denne måten å skille på? For å evt. få til dette må det
            // filtreres her etter at det hentes.(bare innvilget)
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
        // Relaterte yteleser fra InntektArbeidYtelseAggregatet
        return ytelseTjeneste.utledYtelserRelatertTilBehandling(aktørId, grunnlag, Set.of());
    }

    private List<TilgrensendeYtelserDto> hentRelaterteYtelserAnnenPart(InntektArbeidYtelseGrunnlag grunnlag, AktørId aktørId) {
        return ytelseTjeneste.utledAnnenPartsYtelserRelatertTilBehandling(aktørId, grunnlag, Set.of());
    }
}
