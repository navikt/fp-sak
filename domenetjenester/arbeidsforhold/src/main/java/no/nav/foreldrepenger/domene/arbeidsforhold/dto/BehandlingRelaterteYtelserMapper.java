package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.vedtak.konfig.Tid;

public class BehandlingRelaterteYtelserMapper {

    private static final Map<FagsakYtelseType, RelatertYtelseType> YTELSE_TYPE_MAP = Map.of(
        FagsakYtelseType.ENGANGSTØNAD, RelatertYtelseType.ENGANGSSTØNAD,
        FagsakYtelseType.FORELDREPENGER, RelatertYtelseType.FORELDREPENGER,
        FagsakYtelseType.SVANGERSKAPSPENGER, RelatertYtelseType.SVANGERSKAPSPENGER
    );

    public static final List<RelatertYtelseType> RELATERT_YTELSE_TYPER_FOR_SØKER = List.of(
        RelatertYtelseType.FORELDREPENGER,
        RelatertYtelseType.ENGANGSSTØNAD,
        RelatertYtelseType.SYKEPENGER,
        RelatertYtelseType.ENSLIG_FORSØRGER,
        RelatertYtelseType.DAGPENGER,
        RelatertYtelseType.ARBEIDSAVKLARINGSPENGER,
        RelatertYtelseType.SVANGERSKAPSPENGER);

    public static final List<RelatertYtelseType> RELATERT_YTELSE_TYPER_FOR_ANNEN_FORELDER = List.of(
        RelatertYtelseType.FORELDREPENGER,
        RelatertYtelseType.ENGANGSSTØNAD);

    private BehandlingRelaterteYtelserMapper() {
    }

    public static List<TilgrensendeYtelserDto> mapFraBehandlingRelaterteYtelser(Collection<Ytelse> ytelser) {
        return ytelser.stream()
            .map(ytelse -> lagTilgrensendeYtelse(ytelse))
            .collect(Collectors.toList());
    }

    public static RelatertYtelseType mapFraFagsakYtelseTypeTilRelatertYtelseType(FagsakYtelseType type) {
        return YTELSE_TYPE_MAP.getOrDefault(type, RelatertYtelseType.UDEFINERT);
    }

    private static TilgrensendeYtelserDto lagTilgrensendeYtelse(Ytelse ytelse) {
        TilgrensendeYtelserDto tilgrensendeYtelserDto = new TilgrensendeYtelserDto();
        tilgrensendeYtelserDto.setRelatertYtelseType(ytelse.getRelatertYtelseType().getKode());
        tilgrensendeYtelserDto.setPeriodeFraDato(ytelse.getPeriode().getFomDato());
        tilgrensendeYtelserDto.setPeriodeTilDato(endreTomDatoHvisLøpende(ytelse.getPeriode().getTomDato()));
        tilgrensendeYtelserDto.setStatus(ytelse.getStatus().getKode());
        tilgrensendeYtelserDto.setSaksNummer(ytelse.getSaksnummer());
        return tilgrensendeYtelserDto;
    }

    public static TilgrensendeYtelserDto mapFraFagsak(Fagsak fagsak, LocalDate periodeDato) {
        TilgrensendeYtelserDto tilgrensendeYtelserDto = new TilgrensendeYtelserDto();
        RelatertYtelseType relatertYtelseType = YTELSE_TYPE_MAP.getOrDefault(fagsak.getYtelseType(), RelatertYtelseType.UDEFINERT);
        tilgrensendeYtelserDto.setRelatertYtelseType(relatertYtelseType.getKode());
        tilgrensendeYtelserDto.setStatus(fagsak.getStatus().getKode());
        tilgrensendeYtelserDto.setPeriodeFraDato(periodeDato);
        tilgrensendeYtelserDto.setPeriodeTilDato(endreTomDatoHvisLøpende(periodeDato));
        tilgrensendeYtelserDto.setSaksNummer(fagsak.getSaksnummer());
        return tilgrensendeYtelserDto;
    }

    private static LocalDate endreTomDatoHvisLøpende(LocalDate tomDato) {
        if (Tid.TIDENES_ENDE.equals(tomDato)) {
            return null;
        }
        return tomDato;
    }

    public static List<RelaterteYtelserDto> samleYtelserBasertPåYtelseType(List<TilgrensendeYtelserDto> tilgrensendeYtelser, List<RelatertYtelseType> ytelsesTyper) {
        List<RelaterteYtelserDto> relaterteYtelserDtos = new LinkedList<>();
        for (RelatertYtelseType relatertYtelseType : ytelsesTyper) {
            relaterteYtelserDtos.add(new RelaterteYtelserDto(relatertYtelseType.getKode(), sortTilgrensendeYtelser(tilgrensendeYtelser, relatertYtelseType.getKode())));
        }
        return relaterteYtelserDtos;
    }

    private static List<TilgrensendeYtelserDto> sortTilgrensendeYtelser(List<TilgrensendeYtelserDto> relatertYtelser, String relatertYtelseType) {
        return relatertYtelser.stream().filter(tilgrensendeYtelserDto -> (relatertYtelseType.equals(tilgrensendeYtelserDto.getRelatertYtelseType())))
            .sorted()
            .collect(Collectors.toList());
    }
}
