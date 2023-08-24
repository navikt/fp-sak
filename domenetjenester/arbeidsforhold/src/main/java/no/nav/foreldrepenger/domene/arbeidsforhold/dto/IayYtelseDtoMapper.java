package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.arbeidsforhold.YtelserKonsolidertTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.typer.AktørId;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER;
import static no.nav.foreldrepenger.domene.arbeidsforhold.dto.BehandlingRelaterteYtelserMapper.RELATERT_YTELSE_TYPER_FOR_SØKER;

@ApplicationScoped
public class IayYtelseDtoMapper {

    private YtelserKonsolidertTjeneste ytelseTjeneste;

    public IayYtelseDtoMapper() {
        // for CDI proxy
    }

    @Inject
    public IayYtelseDtoMapper(YtelserKonsolidertTjeneste ytelseTjeneste) {
        this.ytelseTjeneste = ytelseTjeneste;
    }

    public IAYYtelseDto mapFra(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, Optional<AktørId> aktørIdAnnenPart) {
        var dto = new IAYYtelseDto();
        mapRelaterteYtelser(dto, ref, iayGrunnlag, aktørIdAnnenPart);
        return dto;
    }

    private void mapRelaterteYtelser(IAYYtelseDto dto, BehandlingReferanse ref, InntektArbeidYtelseGrunnlag grunnlag,
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
