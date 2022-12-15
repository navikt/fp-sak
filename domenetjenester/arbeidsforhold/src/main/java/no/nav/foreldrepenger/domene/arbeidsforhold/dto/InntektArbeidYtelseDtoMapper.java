package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import static no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER;
import static no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_SØKER;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.YtelserKonsolidertTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class InntektArbeidYtelseDtoMapper {

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private VirksomhetTjeneste virksomhetTjeneste;
    private YtelserKonsolidertTjeneste ytelseTjeneste;

    public InntektArbeidYtelseDtoMapper() {
        // for CDI proxy
    }

    @Inject
    public InntektArbeidYtelseDtoMapper(YtelserKonsolidertTjeneste ytelseTjeneste,
                                        InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                        VirksomhetTjeneste virksomhetTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.ytelseTjeneste = ytelseTjeneste;
    }

    public InntektArbeidYtelseDto mapFra(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, Optional<AktørId> aktørIdAnnenPart) {
        var dto = new InntektArbeidYtelseDto();
        mapRelaterteYtelser(dto, ref, iayGrunnlag, aktørIdAnnenPart);
        return dto;
    }

    public InntektsmeldingerDto mapInntektsmeldinger(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(ref.fagsakYtelseType())) {

            return new InntektsmeldingerDto(lagInntektsmeldingDto(ref, iayGrunnlag));
        } else {
            return new InntektsmeldingerDto(List.of());
        }
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
